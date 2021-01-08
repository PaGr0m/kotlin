/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.spm

import groovy.lang.Closure
import org.gradle.api.Project
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.util.ConfigureUtil

class SwiftPackageExtension(private val project: Project) {
    @Input
    lateinit var name: String

    // TODO: List<String> or String
    @Input
    @Optional
    var defaultLocalization: List<String>? = null

    @Nested
    @Optional
    var platforms: List<SupportedPlatform>? = null

    @Nested
    var productsLibraries: List<Products.ProductType.Library> = listOf()

    @Nested
    var productsExecutables: List<Products.ProductType.Executable> = listOf()

    @Nested
    var dependencies: List<Dependencies.Package> = listOf()

    @Nested
    var targetsRegular: List<Targets.Target> = listOf()

    @Nested
    var targetsExecutable: List<Targets.Target> = listOf()

    @Nested
    var targetsTest: List<Targets.Target> = listOf()

    // TODO: swiftLanguageVersions: [SwiftVersion]? = nil,
    // TODO: cLanguageStandard: CLanguageStandard? = nil,
    // TODO: cxxLanguageStandard: CXXLanguageStandard? = nil

    fun platforms(configure: SupportedPlatform.() -> Unit) {
        val supportedPlatform = SupportedPlatform().apply(configure)
        platforms = supportedPlatform.platforms
    }

    fun products(configure: Products.() -> Unit) {
        val product = Products().apply(configure)

        productsLibraries = product.libraries
        productsExecutables = product.executables
    }

    fun dependencies(configure: Dependencies.() -> Unit) {
        val dependency = Dependencies().apply(configure)
        dependencies = dependency.dependencies
    }

    fun targets(configure: Targets.() -> Unit) {
        val target = Targets().apply(configure)

        targetsRegular = target.regulars
        targetsExecutable = target.executables
        targetsTest = target.tests
    }

    // TODO: add fun with another params (supportedPlatform)
    class SupportedPlatform() {
        var platforms = mutableListOf<SupportedPlatform>()

        lateinit var type: PlatformType
        lateinit var version: String

        constructor(type: PlatformType, version: String) : this() {
            this.type = type
            this.version = version
        }

        fun macOS(version: String) {
            val platform = SupportedPlatform(PlatformType.macOS, version)
            platforms.add(platform)
        }

        fun iOS(version: String) {
            val platform = SupportedPlatform(PlatformType.iOS, version)
            platforms.add(platform)
        }

        fun tvOS(version: String) {
            val platform = SupportedPlatform(PlatformType.tvOS, version)
            platforms.add(platform)
        }

        fun watchOS(version: String) {
            val platform = SupportedPlatform(PlatformType.watchOS, version)
            platforms.add(platform)
        }

        enum class PlatformType {
            macOS, iOS, tvOS, watchOS
        }
    }

    class Products {
        var libraries = mutableListOf<ProductType.Library>()
        var executables = mutableListOf<ProductType.Executable>()

        fun library(name: String, configure: ProductType.Library.() -> Unit) {
            val library = ProductType.Library(name).apply(configure)
            libraries.add(library)
        }

        fun library(name: String, configure: Closure<*>) = library(name) {
            ConfigureUtil.configure(configure, this)
        }

        fun executable(name: String, configure: ProductType.Executable.() -> Unit) {
            val executable = ProductType.Executable(name).apply(configure)
            executables.add(executable)
        }

        fun executable(name: String, configure: Closure<*>) = executable(name) {
            ConfigureUtil.configure(configure, this)
        }

        sealed class ProductType {
            class Executable(@Input val name: String) {
                @Input
                val targets = arrayListOf<String>()

                operator fun String.unaryPlus() {
                    targets.add(this)
                }
            }

            class Library(@Input val name: String) {
                // TODO: доделать
                @Input
                lateinit var type: LibraryType

                @Input
                val targets = arrayListOf<String>()

                operator fun String.unaryPlus() {
                    targets.add(this)
                }

                enum class LibraryType {
                    STATIC,
                    DYNAMIC
                }
            }
        }
    }

    // TODO: add fun with ranges
    // TODO: add Package Dependency Requirement
    class Dependencies {
        var dependencies = mutableListOf<Package>()

        fun `package`(url: String, version: String) {
            val dependency = Package(url, version)
            dependencies.add(dependency)
        }

        fun `package`(path: String) {
            val dependency = Package(path)
            dependencies.add(dependency)
        }

        data class Package(
            @Input val url: String,
            @Input @Optional var version: String? = null
        )
    }

    class Targets {
        // TODO: так и оставить как 3 списка?
        var regulars = mutableListOf<Target>()
        var executables = mutableListOf<Target>()
        var tests = mutableListOf<Target>()

        var libraries = mutableListOf<SystemLibrary>()
        var binaries = mutableListOf<BinaryTarget>()

        fun target(name: String, configure: Target.() -> Unit) {
            val target = Target(name).apply(configure)
            regulars.add(target)
        }

        fun executableTarget(name: String, configure: Target.() -> Unit) {
            val target = Target(name).apply(configure)
            executables.add(target)
        }

        fun testTarget(name: String, configure: Target.() -> Unit) {
            val target = Target(name).apply(configure)
            tests.add(target)
        }

        fun systemLibrary(name: String, configure: SystemLibrary.() -> Unit) {
            val library = SystemLibrary(name).apply(configure)
            libraries.add(library)
        }

        fun binaryTarget(name: String, url: String, checksum: String) {
            val binaryTarget = BinaryTarget(name, url, checksum)
            binaries.add(binaryTarget)
        }

        fun binaryTarget(name: String, path: String) {
            val binaryTarget = BinaryTarget(name, path)
            binaries.add(binaryTarget)
        }

        /** TODO
         * Все target оказались одинаковыми по сигнатурам,
         * кажется нет смысла разносить по разным классам
         */
        class Target(@Input val name: String) {
            @Input
            @Optional
            var path: String? = null

            @Input
            @Optional
            var publicHeadersPath: String? = null

            @Nested
            var dependenciesTarget = mutableListOf<TargetDependencies.Target>()

            @Nested
            var dependenciesProduct = mutableListOf<TargetDependencies.Product>()

            @Nested
            var excludePaths = mutableListOf<String>()

            @Nested
            @Optional
            var sourceFiles: List<String>? = null

            @Nested
            @Optional
            var resources: List<Resource>? = null

            fun dependencies(configure: TargetDependencies.() -> Unit) {
                val dependency = TargetDependencies().apply(configure)
                dependenciesTarget = dependency.targets
                dependenciesProduct = dependency.products
            }

            fun exclude(configure: ExcludePath.() -> Unit) {
                val exclude = ExcludePath().apply(configure)
                excludePaths = exclude.paths
            }

            fun sources(configure: SourceFile.() -> Unit) {
                val sources = SourceFile().apply(configure)
                sourceFiles = sources.files
            }

            fun resources(configure: Resource.() -> Unit) {
                val resource = Resource().apply(configure)
                resources = resource.resources
            }

            // TODO: доделать
//            fun cSettings() {}
//            fun cxxSettings() {}
//            fun swiftSettings() {}
//            fun linkerSettings() {}

            class Resource() {
                var resources = mutableListOf<Resource>()

                @Input
                lateinit var path: String

                // TODO: непонятно как задается
                @Input
                @Optional
                var localization: Localization? = null

                constructor(path: String) : this() {
                    this.path = path
                }

                constructor(path: String, localization: Localization? = null) : this(path) {
                    this.localization = localization
                }

                fun process(path: String, localization: Localization? = null) {
                    val resource = Resource(path, localization)
                    resources.add(resource)
                }

                fun copy(path: String) {
                    val resource = Resource(path)
                    resources.add(resource)
                }

                // TODO: ...
                class Localization()
            }

            // TODO: добавить -- condition: BuildSettingCondition? = null
//            class CSettings {
//                fun headerSearchPath(path: String) {}
//                fun define(name: String, to: String? = null) {}
//                fun unsafeFlags(flags: List<String>) {}
//            }
        }

        class SystemLibrary(
            @Input val name: String,
            @Input @Optional var path: String? = null,
            @Input @Optional var pkgConfig: String? = null
        ) {
            // TODO: пока непонятно как именно с ним работать...
            // @Input @Optional var providers: List<SystemPackageProvider>? = null
            // class SystemPackageProvider
        }

        class BinaryTarget(
            @Input val name: String,
            @Input val url: String
        ) {
            // TODO: need fix
            var checksum: String = ""

            constructor(name: String, path: String, checksum: String) : this(name, path) {
                this.checksum = checksum
            }
        }

        class TargetDependencies {
            var targets = mutableListOf<Target>()
            var products = mutableListOf<Product>()

            fun target(name: String, condition: String? = null) {
                val target = Target(name, condition)
                targets.add(target)
            }

            fun product(name: String, `package`: String, condition: String? = null) {
                val product = Product(name, `package`, condition)
                products.add(product)
            }

            data class Target(val name: String, val condition: String? = null)
            data class Product(val name: String, val `package`: String, val condition: String? = null)
        }

        // TODO: Вынести логику в отдельный класс и просто наследоваться?
        class ExcludePath {
            var paths = mutableListOf<String>()

            operator fun String.unaryPlus() {
                paths.add(this)
            }
        }

        // TODO: Вынести логику в отдельный класс и просто наследоваться?
        class SourceFile {
            var files = mutableListOf<String>()

            operator fun String.unaryPlus() {
                files.add(this)
            }
        }
    }
}
