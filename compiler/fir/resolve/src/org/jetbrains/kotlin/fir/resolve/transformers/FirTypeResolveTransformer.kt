/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.resolve.transformers

import org.jetbrains.kotlin.fir.FirSession
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.expressions.FirAnnotationCall
import org.jetbrains.kotlin.fir.expressions.FirBlock
import org.jetbrains.kotlin.fir.expressions.FirDelegatedConstructorCall
import org.jetbrains.kotlin.fir.expressions.FirStatement
import org.jetbrains.kotlin.fir.resolve.ScopeSession
import org.jetbrains.kotlin.fir.scopes.FirScope
import org.jetbrains.kotlin.fir.scopes.createImportingScopes
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.fir.types.impl.FirImplicitBuiltinTypeRef
import org.jetbrains.kotlin.fir.visitors.CompositeTransformResult
import org.jetbrains.kotlin.fir.visitors.compose

class FirTypeResolveProcessor(
    session: FirSession, scopeSession: ScopeSession
) : FirTransformerBasedResolveProcessor<FirDeclarationStatus>(session, scopeSession) {
    override val transformer = FirTypeResolveTransformer(session, scopeSession)
}

fun <F : FirClass<F>> F.runTypeResolvePhaseForLocalClass(
    session: FirSession,
    scopeSession: ScopeSession,
    currentScopeList: List<FirScope>,
): F {
    val transformer = FirTypeResolveTransformer(session, scopeSession, currentScopeList)

    return this.transform<F, Nothing?>(transformer, null).single
}

class FirTypeResolveTransformer(
    override val session: FirSession,
    scopeSession: ScopeSession,
    initialScopes: List<FirScope> = emptyList()
) : FirAbstractTreeTransformerWithSuperTypes<FirDeclarationStatus?>(
    phase = FirResolvePhase.TYPES,
    scopeSession
) {

    init {
        scopes.addAll(initialScopes.asReversed())
    }

    private val classes = mutableListOf<FirClass<*>>()

    private val containingClass: FirClass<*>? get() = classes.lastOrNull()

    private val typeResolverTransformer: FirSpecificTypeResolverTransformer = FirSpecificTypeResolverTransformer(session)

    private inline fun storeClass(
        klass: FirClass<*>,
        computeResult: () -> CompositeTransformResult<FirStatement>
    ): CompositeTransformResult<FirStatement> {
        classes += klass
        val result = computeResult()
        classes.removeAt(classes.lastIndex)
        return result
    }

    override fun transformDeclarationStatus(
        declarationStatus: FirDeclarationStatus,
        data: FirDeclarationStatus?
    ): CompositeTransformResult<FirDeclarationStatus> {
        return (data ?: declarationStatus).compose()
    }

    override fun transformFile(file: FirFile, data: FirDeclarationStatus?): CompositeTransformResult<FirFile> {
        checkSessionConsistency(file)
        return withScopeCleanup {
            scopes.addAll(createImportingScopes(file, session, scopeSession))
            super.transformFile(file, data)
        }
    }

    override fun transformRegularClass(regularClass: FirRegularClass, data: FirDeclarationStatus?): CompositeTransformResult<FirStatement> {
        withScopeCleanup {
            regularClass.addTypeParametersScope()
            regularClass.typeParameters.forEach {
                it.accept(this, data)
            }
            unboundCyclesInTypeParametersSupertypes(regularClass)
        }

        return storeClass(regularClass) {
            regularClass.transformStatus(this, regularClass.resolveModality(regularClass.status, containingClass))
            resolveNestedClassesSupertypes(regularClass, data)
        }
    }

    override fun transformAnonymousObject(
        anonymousObject: FirAnonymousObject,
        data: FirDeclarationStatus?
    ): CompositeTransformResult<FirStatement> {
        return storeClass(anonymousObject) {
            return resolveNestedClassesSupertypes(anonymousObject, data)
        }
    }

    override fun transformConstructor(constructor: FirConstructor, data: FirDeclarationStatus?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            constructor.addTypeParametersScope()
            constructor.transformStatus(this, constructor.resolveModality(constructor.status, containingClass))
            transformDeclaration(constructor, data)
        }
    }

    override fun transformTypeAlias(typeAlias: FirTypeAlias, data: FirDeclarationStatus?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            typeAlias.addTypeParametersScope()
            transformDeclaration(typeAlias, data)
        }
    }

    override fun transformEnumEntry(enumEntry: FirEnumEntry, data: FirDeclarationStatus?): CompositeTransformResult<FirDeclaration> {
        enumEntry.replaceResolvePhase(FirResolvePhase.TYPES)
        enumEntry.transformReturnTypeRef(this, data)
        enumEntry.transformTypeParameters(this, data)
        enumEntry.transformAnnotations(this, data)
        return enumEntry.compose()
    }

    override fun transformProperty(property: FirProperty, data: FirDeclarationStatus?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            property.addTypeParametersScope()
            property.replaceResolvePhase(FirResolvePhase.TYPES)
            property.transformStatus(this, property.resolveModality(property.status, containingClass))
            property.transformTypeParameters(this, data)
                .transformReturnTypeRef(this, data)
                .transformReceiverTypeRef(this, data)
                .transformGetter(this, data)
                .transformSetter(this, data)
                .transformAnnotations(this, data)
            if (property.isFromVararg == true) {
                property.transformTypeToArrayType()
                property.getter?.transformReturnTypeRef(StoreType, property.returnTypeRef)
                property.setter?.valueParameters?.map { it.transformReturnTypeRef(StoreType, property.returnTypeRef) }
            }

            unboundCyclesInTypeParametersSupertypes(property)

            property.compose()
        }
    }

    override fun transformSimpleFunction(simpleFunction: FirSimpleFunction, data: FirDeclarationStatus?): CompositeTransformResult<FirDeclaration> {
        return withScopeCleanup {
            simpleFunction.addTypeParametersScope()
            simpleFunction.transformStatus(this, simpleFunction.resolveModality(simpleFunction.status, containingClass))
            transformDeclaration(simpleFunction, data).also {
                unboundCyclesInTypeParametersSupertypes(it.single as FirTypeParametersOwner)
            }
        }
    }

    private fun unboundCyclesInTypeParametersSupertypes(typeParametersOwner: FirTypeParameterRefsOwner) {
        for (typeParameter in typeParametersOwner.typeParameters) {
            if (typeParameter !is FirTypeParameter) continue
            if (hasSupertypePathToParameter(typeParameter, typeParameter, mutableSetOf())) {
                // TODO: Report diagnostic somewhere
                typeParameter.replaceBounds(
                    listOf(session.builtinTypes.nullableAnyType)
                )
            }
        }
    }

    private fun hasSupertypePathToParameter(
        currentTypeParameter: FirTypeParameter,
        typeParameter: FirTypeParameter,
        visited: MutableSet<FirTypeParameter>
    ): Boolean {
        if (visited.isNotEmpty() && currentTypeParameter == typeParameter) return true
        if (!visited.add(currentTypeParameter)) return false

        return currentTypeParameter.bounds.any {
            val nextTypeParameter = it.coneTypeSafe<ConeTypeParameterType>()?.lookupTag?.typeParameterSymbol?.fir ?: return@any false

            hasSupertypePathToParameter(nextTypeParameter, typeParameter, visited)
        }
    }

    override fun transformImplicitTypeRef(implicitTypeRef: FirImplicitTypeRef, data: FirDeclarationStatus?): CompositeTransformResult<FirTypeRef> {
        if (implicitTypeRef is FirImplicitBuiltinTypeRef) return transformTypeRef(implicitTypeRef, data)
        return implicitTypeRef.compose()
    }

    override fun transformTypeRef(typeRef: FirTypeRef, data: FirDeclarationStatus?): CompositeTransformResult<FirTypeRef> {
        return typeResolverTransformer.transformTypeRef(typeRef, towerScope)
    }

    override fun transformValueParameter(valueParameter: FirValueParameter, data: FirDeclarationStatus?): CompositeTransformResult<FirStatement> {
        valueParameter.transformReturnTypeRef(this, data)
        valueParameter.transformAnnotations(this, data)
        valueParameter.transformVarargTypeToArrayType()
        return valueParameter.compose()
    }

    override fun transformBlock(block: FirBlock, data: FirDeclarationStatus?): CompositeTransformResult<FirStatement> {
        return block.compose()
    }

    override fun transformDelegatedConstructorCall(
        delegatedConstructorCall: FirDelegatedConstructorCall,
        data: FirDeclarationStatus?
    ): CompositeTransformResult<FirStatement> {
        delegatedConstructorCall.replaceConstructedTypeRef(
            delegatedConstructorCall.constructedTypeRef.transform<FirTypeRef, FirDeclarationStatus?>(this, data).single
        )
        delegatedConstructorCall.transformCalleeReference(this, data)
        return delegatedConstructorCall.compose()
    }

    override fun transformAnnotationCall(annotationCall: FirAnnotationCall, data: FirDeclarationStatus?): CompositeTransformResult<FirStatement> {
        annotationCall.transformAnnotationTypeRef(this, data)
        return annotationCall.compose()
    }
}
