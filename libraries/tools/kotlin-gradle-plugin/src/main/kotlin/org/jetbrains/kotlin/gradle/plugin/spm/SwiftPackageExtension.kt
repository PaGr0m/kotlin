/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.spm

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.spm.entity.Product
import org.jetbrains.kotlin.gradle.plugin.spm.entity.Target
import org.jetbrains.kotlin.gradle.plugin.spm.entity.impl.DependencyBlock
import org.jetbrains.kotlin.gradle.plugin.spm.entity.impl.ProductBlock
import org.jetbrains.kotlin.gradle.plugin.spm.entity.impl.SupportedPlatformBlock
import org.jetbrains.kotlin.gradle.plugin.spm.entity.impl.TargetBlock

class SwiftPackageExtension(private val project: Project) {
    @Input
    lateinit var name: String

    private val platformsContainer = project.container(SupportedPlatformBlock.SupportedPlatform::class.java)
    private val productsContainer = project.container(Product::class.java)
    private val dependenciesContainer = project.container(DependencyBlock.Package::class.java)
    private val targetsContainer = project.container(Target::class.java)

    @get:Nested
    val platforms: List<SupportedPlatformBlock.SupportedPlatform>
        get() = platformsContainer.toList()

    @get:Nested
    val products: List<Product>
        get() = productsContainer.toList()

    @get:Nested
    val dependencies: List<DependencyBlock.Package>
        get() = dependenciesContainer.toList()

    @get:Nested
    val targets: List<Target>
        get() = targetsContainer.toList()

    // TODO: swiftLanguageVersions: [SwiftVersion]? = nil,
    // TODO: cLanguageStandard: CLanguageStandard? = nil,
    // TODO: cxxLanguageStandard: CXXLanguageStandard? = nil

    fun platforms(configure: SupportedPlatformBlock.() -> Unit) {
        val supportedPlatform = SupportedPlatformBlock().apply(configure)
        platformsContainer.addAll(supportedPlatform.platforms)
    }

    fun platforms(configure: Closure<*>) = platforms {
        ConfigureUtil.configure(configure, this)
    }

    fun products(configure: ProductBlock.() -> Unit) {
        val productBlock = ProductBlock().apply(configure)
        productsContainer.addAll(productBlock.products)
    }

    fun products(configure: Closure<*>) = products {
        ConfigureUtil.configure(configure, this)
    }

    fun dependencies(configure: DependencyBlock.() -> Unit) {
        val dependencyBlock = DependencyBlock().apply(configure)
        dependenciesContainer.addAll(dependencyBlock.dependencies)
    }

    fun dependencies(configure: Closure<*>) = dependencies {
        ConfigureUtil.configure(configure, this)
    }

    fun targets(configure: TargetBlock.() -> Unit) {
        val targetBlock = TargetBlock().apply(configure)
        targetsContainer.addAll(targetBlock.targets)
    }

    fun targets(configure: Closure<*>) = targets {
        ConfigureUtil.configure(configure, this)
    }
}
