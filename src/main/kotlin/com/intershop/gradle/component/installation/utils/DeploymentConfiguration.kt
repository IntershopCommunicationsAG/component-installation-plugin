/*
 * Copyright 2018 Intershop Communications AG.
 *
 * Licensed under the Apache License, VersionComparator 2.0 (the "License");
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
package com.intershop.gradle.component.installation.utils

import com.intershop.gradle.component.installation.extension.Component
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider

data class DeploymentConfiguration(

    val repositoryURLProvider: Provider<String>,
    val repositoryPatternProvider: Provider<String>,
    val deploymentTargetProvider: Provider<Directory>,

    val componentContainer: NamedDomainObjectContainer<Component>,

    val dependencyHandler: DependencyHandler,

    val repositories: RepositoryHandler
)
