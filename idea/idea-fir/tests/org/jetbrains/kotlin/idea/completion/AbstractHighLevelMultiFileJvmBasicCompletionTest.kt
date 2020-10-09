/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.completion

import org.jetbrains.kotlin.idea.completion.test.AbstractMultiFileJvmBasicCompletionTest
import java.io.File

abstract class AbstractHighLevelMultiFileJvmBasicCompletionTest : AbstractMultiFileJvmBasicCompletionTest() {
    override val additionalValidDirectives: List<String> = listOf(FIR_COMPARISON)

    protected fun doTestWrapped(testPath: String) {
        val mainTestFile = File(testPath, "${getTestName(false)}.kt").also { assertExists(it) }

        runTestWithCustomEnableDirective(FIR_COMPARISON, mainTestFile) { doTest(testPath) }
    }
}