/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.spm.entity.impl

import groovy.lang.Closure
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.util.ConfigureUtil
import org.jetbrains.kotlin.gradle.plugin.spm.utils.StringUnaryPlusContainer
import org.jetbrains.kotlin.gradle.plugin.spm.entity.Target
import org.jetbrains.kotlin.gradle.plugin.spm.entity.TargetDependency

/**
 * TODO: comment
 *
 * @see [Target](https://github.com/apple/swift-package-manager/blob/main/Documentation/PackageDescription.md#target)
 * @see [Target](https://docs.swift.org/package-manager/PackageDescription/PackageDescription.html#target)
 */
class TargetBlock {
    val targets = mutableListOf<Target>()

    fun target(name: String, configure: RegularTarget.() -> Unit) {
        val target = RegularTarget(name).apply(configure)
        targets.add(target)
    }

    fun target(name: String, configure: Closure<*>) = target(name) {
        ConfigureUtil.configure(configure, this)
    }

    fun executableTarget(name: String, configure: ExecutableTarget.() -> Unit) {
        val target = ExecutableTarget(name).apply(configure)
        targets.add(target)
    }

    fun executableTarget(name: String, configure: Closure<*>) = executableTarget(name) {
        ConfigureUtil.configure(configure, this)
    }

    fun testTarget(name: String, configure: TestTarget.() -> Unit) {
        val target = TestTarget(name).apply(configure)
        targets.add(target)
    }

    fun testTarget(name: String, configure: Closure<*>) = testTarget(name) {
        ConfigureUtil.configure(configure, this)
    }

    fun systemLibrary(name: String, configure: SystemLibrary.() -> Unit) {
        val library = SystemLibrary(name).apply(configure)
        targets.add(library)
    }

    fun systemLibrary(name: String, configure: Closure<*>) = systemLibrary(name) {
        ConfigureUtil.configure(configure, this)
    }

    fun binaryTarget(name: String, url: String, checksum: String) {
        val binaryTarget = BinaryTarget(name, url, checksum)
        targets.add(binaryTarget)
    }

    fun binaryTarget(name: String, path: String) {
        val binaryTarget = BinaryTarget(name, path)
        targets.add(binaryTarget)
    }

    abstract class AbstractTarget(private val name: String) : Target {
        override fun getName(): String = name

        @Nested
        var dependencies = mutableListOf<TargetDependency>()

        @Input
        @Optional
        var path: String? = null

        @Nested
        var excludePaths = mutableListOf<String>()

        @Nested
        @Optional
        var sourceFiles = mutableListOf<String>()

        @Nested
        @Optional
        var resources = mutableListOf<ResourceBlock.Resource>()

        @Input
        @Optional
        var publicHeadersPath: String? = null

        // TODO: add some params
        //       cSettings: [CSetting]? = nil,
        //       cxxSettings: [CXXSetting]? = nil,
        //       swiftSettings: [SwiftSetting]? = nil,
        //       linkerSettings: [LinkerSetting]? = nil

        fun dependencies(configure: TargetDependencyBlock.() -> Unit) {
            val targetDependencyBlock = TargetDependencyBlock().apply(configure)
            dependencies.addAll(targetDependencyBlock.targetDependencies)
        }

        fun exclude(configure: ExcludePath.() -> Unit) {
            val exclude = ExcludePath().apply(configure)
            excludePaths.addAll(exclude.container)
        }

        fun sources(configure: SourceFile.() -> Unit) {
            val sources = SourceFile().apply(configure)
            sourceFiles.addAll(sources.container)
        }

        fun resources(configure: ResourceBlock.() -> Unit) {
            val resource = ResourceBlock().apply(configure)
            resources.addAll(resource.resources)
        }

        class ResourceBlock {
            var resources = mutableListOf<Resource>()

            fun process(path: String, localization: Resource.Localization? = null) {
                val resource = Resource(path, localization)
                resources.add(resource)
            }

            fun copy(path: String) {
                val resource = Resource(path)
                resources.add(resource)
            }

            class Resource(
                @Input val path: String,
                @Input @Optional val localization: Localization? = null
            ) {
                // TODO: localization: Localization? = nil
                class Localization
            }
        }
    }

    class RegularTarget(name: String) : AbstractTarget(name)

    class ExecutableTarget(name: String) : AbstractTarget(name)

    class TestTarget(name: String) : AbstractTarget(name)

    class SystemLibrary(
        private val name: String,
        @Input @Optional var path: String? = null,
        @Input @Optional var pkgConfig: String? = null
    ) : Target {
        override fun getName(): String = name
        // TODO: пока непонятно как именно с ним работать...
        // @Input @Optional var providers: List<SystemPackageProvider>? = null
        // class SystemPackageProvider
    }

    class BinaryTarget(
        private val name: String,
        @Input val url: String
    ) : Target {
        override fun getName(): String = name

        // TODO: need fix
        var checksum: String = ""

        constructor(name: String, path: String, checksum: String) : this(name, path) {
            this.checksum = checksum
        }
    }

    class ExcludePath : StringUnaryPlusContainer()

    class SourceFile : StringUnaryPlusContainer()
}
