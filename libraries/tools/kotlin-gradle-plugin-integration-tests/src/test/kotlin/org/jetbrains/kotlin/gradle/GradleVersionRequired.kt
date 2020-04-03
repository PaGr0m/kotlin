/*
 * Copyright 2010-2017 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.gradle

import org.gradle.util.GradleVersion
import org.jetbrains.kotlin.gradle.utils.minSupportedGradleVersion
import org.junit.Assume

sealed class GradleVersionRequired(val minVersion: String, val maxVersion: String?) {
    class Exact(version: String) : GradleVersionRequired(version, version)

    class AtLeast(version: String) : GradleVersionRequired(version, null)

    class InRange(minVersion: String, maxVersion: String) : GradleVersionRequired(minVersion, maxVersion)

    class Until(maxVersion: String) : GradleVersionRequired(minSupportedGradleVersion, maxVersion)

    object None : GradleVersionRequired(minSupportedGradleVersion, null)
}


fun BaseGradleIT.Project.chooseWrapperVersionOrFinishTest(): String {
    val gradleVersionForTests = System.getProperty("kotlin.gradle.version.for.tests")?.toGradleVersion()
    val minVersion = max(gradleVersionRequirement.minVersion.toGradleVersion(), minSupportedGradleVersion.toGradleVersion())
    val maxVersion = gradleVersionRequirement.maxVersion?.toGradleVersion()

    if (gradleVersionForTests == null) {
        return minVersion.version
    }

    Assume.assumeTrue(minVersion <= gradleVersionForTests && (maxVersion == null || gradleVersionForTests <= maxVersion))

    return gradleVersionForTests.version
}

private fun <T : Comparable<T>> max(a: T, b: T): T = if (a < b) b else a

private fun String.toGradleVersion() = GradleVersion.version(this)