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
package com.intershop.gradle.component.installation.utils.data

/**
 * Data class of dependency configuration.
 *
 * @property group group or organization of the dependency
 * @property module module or name of the dependency
 * @property version version or revision of the dependency
 *
 * @constructor initialize a dependency object
 */
data class Dependency(val group: String, val module: String, val version: String) {

    /**
     * Get the standard Gradle dependency string from
     * dependency object.
     *
     * @return dependency string.
     */
    fun getDependencyString(): String {
        return "$group:$module:$version"
    }

    /**
     * Get a version pattern from the configuration.
     *
     * @property versionPattern an regex string for a version configuration.
     */
    val versionPattern: String
        get() = version.replace(".", "\\.").replace("+", ".*")

    /**
     * Returns true if the version contains the placeholder for latest version overall.
     *
     * @property hasLatestVersion is true if the configuration contains a placeholder the for latest version.
     */
    val hasLatestVersion: Boolean
        get() = version == "+" || version == "latest"

    /**
     * Returns true if the version contains a placeholder for versions.
     *
     * @property hasVersionPattern is true if the configuration a placeholder.
     */
    val hasVersionPattern: Boolean
        get() = version.endsWith("+") && version != "+"

    /**
     * Returns true if the version contains a placeholder for latest versions.
     *
     * @property hasLatestPattern is true if the configuration contains a placeholder for latest versions.
     */
    val hasLatestPattern: Boolean
        get() = version.endsWith("+") || version == "latest"
}
