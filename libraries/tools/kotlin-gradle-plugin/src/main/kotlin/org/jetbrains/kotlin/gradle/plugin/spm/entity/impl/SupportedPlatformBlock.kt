/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.spm.entity.impl

import org.gradle.api.tasks.Input
import org.jetbrains.kotlin.gradle.plugin.spm.entity.impl.SupportedPlatformBlock.SupportedPlatform
import org.jetbrains.kotlin.konan.target.Family

/**
 * TODO: comment
 *
 * @see [SupportedPlatform](https://docs.swift.org/package-manager/PackageDescription/PackageDescription.html#supportedplatform)
 * @see [SupportedPlatform](https://github.com/apple/swift-package-manager/blob/main/Documentation/PackageDescription.md#supportedplatform)
 */
class SupportedPlatformBlock {
    val platforms = mutableListOf<SupportedPlatform>()

    fun macOS(version: String) {
        val platform = SupportedPlatform(Family.OSX, version)
        platforms.add(platform)
    }

    fun iOS(version: String) {
        val platform = SupportedPlatform(Family.IOS, version)
        platforms.add(platform)
    }

    fun tvOS(version: String) {
        val platform = SupportedPlatform(Family.TVOS, version)
        platforms.add(platform)
    }

    fun watchOS(version: String) {
        val platform = SupportedPlatform(Family.WATCHOS, version)
        platforms.add(platform)
    }

    data class SupportedPlatform(
        @Input val type: Family,
        @Input val version: String
    )
}
