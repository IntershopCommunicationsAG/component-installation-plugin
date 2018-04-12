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

import org.gradle.api.tasks.Input

data class LibData constructor(
        @get:Input
        val group: String,
        @get:Input
        val name: String,
        @get:Input
        val version: String,
        @get:Input
        val path: String )  {

    /**
     * This returns the string representation
     * of the dependency in format group:module:version.
     */
    fun getString() : String {
        return "$group:$name:$version"
    }
}