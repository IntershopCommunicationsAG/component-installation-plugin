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

import com.intershop.gradle.component.installation.utils.getValue
import com.intershop.gradle.component.installation.utils.setValue
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import java.io.File
import javax.inject.Inject

open class DeploymentExtension @Inject constructor(project: Project){

    companion object {
        const val DEPLOYMENT_EXTENSION_NAME = "deploy"
        const val DEPLOYMENT_GROUP_NAME = "Component Deployment"
    }

    private val repositoryURLProperty = project.objects.property(String::class.java)
    private val repositoryPatternProperty = project.objects.property(String::class.java)

    private val componentContainer = project.container(Component::class.java)

    private val deploymentTargetProperty = project.layout.directoryProperty()

    val repositoryURLProvider: Provider<String>
        get() = repositoryURLProperty

    @Suppress("unused")
    var repositoryURL: String by repositoryURLProperty

    val repositoryPatternProvider: Provider<String>
        get() = repositoryPatternProperty

    @Suppress("unused")
    var repositoryPattern: String by repositoryPatternProperty

    val deploymentTargetProvider: Provider<Directory>
        get() = deploymentTargetProperty

    @Suppress("unused")
    var deploymentTarget: File
        get() = deploymentTargetProperty.get().asFile
        set(value) = deploymentTargetProperty.set(value)

    val deploymentComponents: NamedDomainObjectContainer<Component>
        get() = componentContainer

    @Suppress("unused")
    fun component(name: String, component: Action<in Component>) {
        component.execute(componentContainer.maybeCreate(name))
    }
}
