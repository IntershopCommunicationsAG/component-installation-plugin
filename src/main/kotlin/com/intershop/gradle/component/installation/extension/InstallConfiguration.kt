/*
 * Copyright 2018 Intershop Communications AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intershop.gradle.component.installation.extension

import com.intershop.gradle.component.installation.utils.DescriptorManager
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import java.io.File
import javax.inject.Inject

open class InstallConfiguration @Inject constructor(project: Project) {

    private val ivyPatternSet: MutableSet<String> = mutableSetOf()

    init {
        ivyPatternSet.add(DescriptorManager.INTERSHOP_PATTERN)
        ivyPatternSet.add(IvyArtifactRepository.GRADLE_ARTIFACT_PATTERN)
        ivyPatternSet.add(IvyArtifactRepository.IVY_ARTIFACT_PATTERN)
    }

    var installAdminDir: File = project.buildDir

    fun addIvyPattern(pattern: String) {
        ivyPatternSet.add(pattern)
    }

    val ivyPatterns: Set<String>
        get() = ivyPatternSet

}