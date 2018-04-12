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
import javax.inject.Inject

open class Component @Inject constructor(val group: String,
                                         val module: String,
                                         val version: String,
                                         val path: String = "") {

    val commonName: String
        get() {
            val sb = StringBuilder(module)
            if(path.isNotBlank()) {
                path.split("/").last().capitalize()
            }
            return sb.toString()
        }

    val dependency: Dependency
        get() {
            return Dependency(group, module, version)
        }
}
