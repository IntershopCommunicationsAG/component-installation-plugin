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

import com.intershop.gradle.component.installation.filter.FilterContainer
import com.intershop.gradle.component.installation.utils.getValue
import com.intershop.gradle.component.installation.utils.setValue
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.InvalidUserDataException
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.provider.Provider
import org.gradle.internal.reflect.Instantiator
import org.gradle.internal.service.ServiceRegistry
import java.io.File
import javax.inject.Inject

/**
 * The main extension of this plugin.
 *
 * It provides all information for the installation process
 * with all components and directories.
 *
 * @property project instance of the current project.
 *
 * @constructor initialize the extension for the current project.
 */
open class InstallationExtension @Inject constructor(val project: Project) {

    companion object {
        /**
         * The name of the installation extension.
         */
        const val INSTALLATION_EXTENSION_NAME = "installation"

    }

    private val installDirProperty = project.layout.directoryProperty()
    private val installConfigContainer = project.objects.newInstance(InstallConfiguration::class.java, project)

    private val environmentProperty = project.objects.setProperty(String::class.java)
    private val componentSet: MutableSet<Component> = mutableSetOf()

    private val services: ServiceRegistry = if(project is ProjectInternal) project.services else
        throw GradleException("Project ${project.name} is not correct initialized!")

    private val filtersContainer = FilterContainer(project, services.get(Instantiator::class.java))

    /**
     * Provider for the installation directory property.
     *
     * @property installDirProvider provider for the installation directory property. (read only)
     */
    val installDirProvider: Provider<Directory>
        get() = installDirProperty

    /**
     * Installation directory for all components.
     * This is the root directory for all configured components.
     *
     * @property installDir the file object of the installation directory
     */
    var installDir: File
        get() = installDirProperty.get().asFile
        set(value) = installDirProperty.set(value)

    /**
     * The container for installation configuration.
     *
     * @property installConfig the instance of the installation configuration
     */
    val installConfig: InstallConfiguration
        get() = installConfigContainer

    /**
     * Configures the installation configuration container.
     *
     * @param action action or closure to configure the configuration container.
     */
    fun installConfig(action: Action<in InstallConfiguration>) {
        action.execute(installConfigContainer)
    }

    /**
     * Provider for the environment configuration of the installation process.
     *
     * @property environmentProvider provider for the environment configuration. (read only)
     */
    val environmentProvider: Provider<Set<String>>
        get() = environmentProperty

    /**
     * Environment configuration of the installation.
     * This set of strings will be compared with the
     * configuration of component items.
     *
     * @property environment set of strings with the environment configuration
     */
    var environment: Set<String> by environmentProperty

    /**
     * Add an environment configuration to the set
     * of configuration strings.
     *
     * @param config environment configuration.
     */
    fun environment(config: String) {
        environmentProperty.add(config)
    }

    /**
     * Set of components that will be installed in the project.
     *
     * @property components
     */
    val components: Set<Component>
        get() = componentSet

    /**
     * Adds a dependency to the list of components.
     *
     * @param component a dependency of a component.
     */
    @Throws(GradleException::class)
    fun add(component: Any): Component {
        val dependency = project.dependencies.create(component)
        val componentExt = Component(dependency.group ?: "", dependency.name, dependency.version ?: "", "")

        with(componentExt) {
            if (componentSet.any { it.group == group && it.module == module && it.path == path }) {
                throw InvalidUserDataException("It is not possible to install the same component twice " +
                        "in the same path. Verify the configuration of '${this.dependency}'")
            }
        }

        componentSet.add(componentExt)
        return componentExt
    }

    /**
     * Adds a dependency to the list of components and
     * configures the path of the component.
     *
     * @param component a dependency of a component.
     * @param path the install path of the component
     */
    fun add(component: Any, path: String): Component {
        val dependency = project.dependencies.create(component)
        val componentExt = Component(dependency.group ?: "", dependency.name, dependency.version ?: "", path)

        with(componentExt) {
            if (componentSet.any { it.group == group && it.module == module && it.path == path }) {
                throw InvalidUserDataException("It is not possible to install the same component twice " +
                        "in the same path. Verify the configuration of '${this.dependency}'")
            }
        }

        componentSet.add(componentExt)
        return componentExt
    }

    /**
     * Adds a dependency to the list of components.
     * The component is configured with an action
     * or closure.
     *
     * @param component a dependency of a component.
     */
    fun add(component: Any, action: Action<in Component>): Component {
        val componentExt = add(component)
        action.execute(componentExt)
        return componentExt
    }

    /**
     * Adds a dependency to the list of components and
     * configures the path of the component.
     * The component is configured with an action
     * or closure.
     *
     * @param component a dependency of a component.
     * @param path the install path of the component
     */
    fun add(component: Any, path: String, action: Action<in Component>): Component {
        val componentExt = add(component, path)
        action.execute(componentExt)
        return componentExt
    }

    /**
     * This is the filter configuration container. It contains
     * all file filter to adapt an installation.
     *
     * @property filters the instance of the configuration container.
     */
    val filters: FilterContainer
        get() = filtersContainer

    /**
     * This method configures the filter container.
     *
     * @param action this can be an closure or action to configure the container.
     */
    fun filters(action: Action<in FilterContainer>) {
        action.execute(filtersContainer)
    }
}
