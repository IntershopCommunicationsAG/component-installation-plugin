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
 * Data class of a repository.
 *
 * @property type describes the type of repository
 * @property url  URL string of the repository
 * @property credentials credentials for basic authentication
 */
data class Repository @JvmOverloads constructor(val type: RepositoryType,
                                                val url: String,
                                                val credentials: Credentials,
                                                val pattern: String = "") {
    /**
     * Maven basic artifact path if available
     */
    var artifactPath = ""

    /**
     * Necessary for Ivy path handling.
     */
    var version = ""
}
