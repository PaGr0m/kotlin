/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.spm

import groovy.lang.Closure
import org.gradle.api.NamedDomainObjectSet
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.util.ConfigureUtil

class SwiftPackageExtension(private val project: Project) {
    @Input
    var name: String? = null

    @Nested
    val platform: SupportedPlatform? = null // TODO: listOf

    @Nested
    val product: Product = Product(project)

    @Nested
    val dependencies: Dependency = Dependency(project)
//    val dependencies = Dependency(project).dependencies

    // TODO: macOS ...
    class SupportedPlatform {
        @get:Input
        val name: String = ""
    }

     class Product(
        private val project: Project
    ) {
        private val _products = project.container(ProductType::class.java)

        @get:Internal
        val product: NamedDomainObjectSet<ProductType>
            get() = _products

        fun library(name: String, configure: ProductType.Library.() -> Unit) {
            val product = ProductType.Library(name, "TODO", listOf())
            product.configure()
//            _products.add()
        }

        fun library(name: String, configure: Closure<*>) = library(name) {
            ConfigureUtil.configure(configure, this)
        }

        fun executable(name: String, configure: ProductType.Executable.() -> Unit) {
            // TODO: body
        }

        fun executable(name: String, configure: Closure<*>) = executable(name) {
            ConfigureUtil.configure(configure, this)
        }

        sealed class ProductType {
            data class Executable(
                @Input val name: String,
                @Input val targets: List<String>
            )

            data class Library(
                @Input val name: String,
                @Input val type: String, // TODO: enum
                @Input val targets: List<String>
            )
        }
    }

    class Dependency(
        private val project: Project
    ) {
        private val _dependencies = project.container(Dependency::class.java)

        @get:Internal
        val dependencies: NamedDomainObjectSet<Dependency>
            get() = _dependencies

        fun dependencyPackage(name: String, configure: Dependency.() -> Unit) {
            // TODO: add to dependencies
        }

        fun dependencyPackage(path: String) {
            // TODO: add to dependencies
        }

        fun dependencyPackage(url: String, version: String) {
        }

        /*fun dependencyPackage(TODO: url: String, requirement: Requirement) {}*/

        fun dependencyPackage(url: String, range: IntProgression) {
        }

        fun dependencyPackage(url: String, range: IntRange) {
        }
    }

//    class SwiftTarget() {
//        sealed class TargetType {
//            class Target
//        }
//    }
}
