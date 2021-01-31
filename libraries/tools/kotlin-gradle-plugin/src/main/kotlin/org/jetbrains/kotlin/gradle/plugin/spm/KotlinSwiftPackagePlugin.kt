/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.gradle.plugin.spm

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.multiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.addExtension
import org.jetbrains.kotlin.gradle.plugin.spm.entity.Product
import org.jetbrains.kotlin.gradle.plugin.spm.entity.impl.ProductBlock
import org.jetbrains.kotlin.gradle.tasks.registerTask

class KotlinSwiftPackagePlugin : Plugin<Project> {

    override fun apply(project: Project): Unit = with(project) {
        pluginManager.withPlugin("kotlin-multiplatform") {
            val kotlinExtension = project.multiplatformExtension
            val swiftPackageExtension = SwiftPackageExtension(this)
            kotlinExtension.addExtension("spm", swiftPackageExtension)
            registerDummySwiftPackageTask(project, swiftPackageExtension)
        }
    }

    private fun registerDummySwiftPackageTask(
        project: Project,
        swiftPackageExtension: SwiftPackageExtension
    ) {
        project.registerTask<SwiftPackageTask>("SPM_hello") {
            it.doLast {
                println("start")
                println(swiftPackageExtension.name)
                println("end")
            }
        }

        project.registerTask<SwiftPackageTask>("SPM_product") {
            it.doLast {
                println("start")
                println("products")

                println(
                    swiftPackageExtension.products
                        .filterIsInstance(ProductBlock.Library::class.java)
                        .map { ProductBlock.Library::container }
                        .joinToString("\n")
                )

                println("end")
            }
        }

        project.registerTask<SwiftPackageTask>("SPM_targets") {
            it.doLast {
                println("start")
                println("targets")
                println("end")
            }
        }
    }
}
