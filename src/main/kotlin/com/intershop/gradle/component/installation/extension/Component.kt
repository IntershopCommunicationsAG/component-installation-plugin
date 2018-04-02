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


import org.gradle.api.Named
import javax.inject.Inject

open class Component @Inject constructor(val path: String) : Named {

    override fun getName(): String {
        return path
    }

    var repositoryURL = ""

    var repositoryPattern = ""

    var dependencyObject: Any? = null

    @Suppress("unused")
    fun from(dependency: Any) {
        dependencyObject = dependency
    }

    val commonName: String
        get() {
            val suffix = StringBuilder()
            val pathParts = path.split("/")
            pathParts.forEach { part ->
                part.split("\\").forEach {
                    suffix.append(it.toLowerCase().capitalize())
                }
            }
            return suffix.toString().replace(' ', '_')
        }
}