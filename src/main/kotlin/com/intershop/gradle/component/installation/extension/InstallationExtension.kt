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
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import java.io.File
import javax.inject.Inject

open class InstallationExtension @Inject constructor(val project: Project) {

    companion object {
        const val INSTALLATION_EXTENSION_NAME = "installation"
        const val INSTALLATION_GROUP_NAME = "Component Installation"
    }

    private val installDirProperty = project.layout.directoryProperty()
    private val installConfigContainer = project.objects.newInstance(InstallConfiguration::class.java, project)

    private val environmentProperty = project.objects.setProperty(String::class.java)
    private val componentSet: MutableSet<Component> = mutableSetOf()

    val installDirProvider: Provider<Directory>
        get() = installDirProperty

    var installDir: File
        get() = installDirProperty.get().asFile
        set(value) = installDirProperty.set(value)

    val installConfig: InstallConfiguration
        get() = installConfigContainer

    fun installConfig(action: Action<in InstallConfiguration>) {
        action.execute(installConfigContainer)
    }

    val environmentProvider: Provider<Set<String>>
        get() = environmentProperty

    var environment: Set<String> by environmentProperty

    fun environment(config: String) {
        environmentProperty.add(config)
    }

    val detectedOS: OSType
        get() = OSType.detectedOS()

    val components: Set<Component>
        get() = componentSet

    fun add(component: Any) {
        val dependency = project.dependencies.create(component)
        componentSet.add(Component(dependency.group ?: "", dependency.name, dependency.version ?: "", ""))
    }

    fun add(component: Any, path: String) {
        val dependency = project.dependencies.create(component)
        componentSet.add(Component(dependency.group ?: "", dependency.name, dependency.version ?: "", path))
    }
}
