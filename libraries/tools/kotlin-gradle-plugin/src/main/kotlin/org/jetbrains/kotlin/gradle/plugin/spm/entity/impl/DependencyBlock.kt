/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.spm.entity.impl

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Optional
import org.jetbrains.kotlin.gradle.plugin.spm.entity.impl.DependencyBlock.Package

/**
 * TODO: comment
 *
 * @see [Package.Dependency](https://docs.swift.org/package-manager/PackageDescription/PackageDescription.html#package-dependency)
 * @see [Package.Dependency](https://github.com/apple/swift-package-manager/blob/main/Documentation/PackageDescription.md#package-dependency)
 */
class DependencyBlock {
    val dependencies = mutableListOf<Package>()

    fun `package`(url: String, version: String) {
        val dependency = Package(url, version)
        dependencies.add(dependency)
    }

    fun `package`(path: String) {
        val dependency = Package(path)
        dependencies.add(dependency)
    }

    // TODO: add overload function with ranges
    //       add Package Dependency Requirement

    data class Package(
        @Input val url: String,
        @Input @Optional val version: String? = null
    )
}
