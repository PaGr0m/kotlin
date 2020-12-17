/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.targets.native.spm

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

class SwiftPackageTask : DefaultTask() {

    @TaskAction
    fun hello() {
        println("Hello future SPM plugin!")
    }
}