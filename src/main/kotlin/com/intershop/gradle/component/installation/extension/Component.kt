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
package com.intershop.gradle.component.installation.extension

import com.intershop.gradle.component.installation.utils.data.Dependency
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import org.gradle.util.ConfigureUtil
import javax.inject.Inject

/**
 * This is the configuration container of a components.
 * It is initialized with the dependency and the installation
 * path. If the path is not specified the information is taken
 * from the descriptor.
 *
 * @property group the group or organization of the component dependency.
 * @property module the module or name of the of the component dependency.
 * @property version the version or revision of the component dependency.
 * @property path the path of the component in the configured installation dir.
 *
 * @constructor initialize a simple component without any additional configuration.
 */
data class Component @Inject constructor(val group: String,
                                         val module: String,
                                         val version: String,
                                         val path: String = "") {

    private val excludesProperty: MutableSet<String> = mutableSetOf()
    private val preserveProperty: PatternSet = PatternSet()

    /**
     * This is the common name of the component within the installation project.
     *
     * @property commonName the name without spaces and slashes (read only)
     */
    val commonName: String
        get() {
            val commonSB = StringBuilder(module)
            if(path.isNotBlank()) {
                commonSB.append(path.split("/").joinToString("") { it.capitalize() })
            }
            return commonSB.toString()
        }

    /**
     * The dependency object of the component. This is used for
     * further installation steps.
     *
     * @property dependency the object instance (read only)
     */
    val dependency: Dependency
        get() {
            return Dependency(group, module, version)
        }

    val fileItems: LocalFileContainer = LocalFileContainer()

    /**
     * Exclude patterns will be used to exclude special files
     * from the installation or update. Please note, that excluded
     * files will be also removed from an existing installation.
     *
     * @property excludes set of Ant style exclude patterns (read only)
     */
    val excludes: Set<String>
        get() = excludesProperty

    /**
     * Add a pattern to the exclude pattern set.
     *
     * @param pattern Ant style pattern
     */
    fun exclude(pattern: String) {
        excludesProperty.add(pattern)
    }

    /**
     * Add a set of patterns to the exclude pattern set.
     *
     * @param patterns Ant style pattern
     */
    fun exclude(patterns: Set<String>) {
        excludesProperty.addAll(patterns)
    }

    /**
     * Preserve patterns will used be excluded or included files.
     * Installed files will preserved.
     *
     * @property preserve pattern set configuration (read only)
     */
    val preserve: PatternFilterable
        get() = preserveProperty

    /**
     * Configures the preserve pattern set.
     *
     * @param closure the configuration closure.
     */
    @Suppress("unused")
    fun preserve(closure: Closure<Any>) {
        ConfigureUtil.configure(closure, preserveProperty)
    }

    /**
     * Configures the preserve pattern set.
     *
     * @param action the configuration action.
     */
    fun preserve(action: Action<in PatternFilterable>) {
        action.execute(preserveProperty)
    }
}
