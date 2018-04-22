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

import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import java.net.URI

/**
 * Data class of a repository.
 *
 * @property type describes the type of repository.
 * @property url  URL string of the repository.
 * @property credentials credentials for basic authentication.
 * @property pattern Ivy pattern
 */
data class Repository @JvmOverloads constructor(val type: RepositoryType,
                                                val url: URI,
                                                val credentials: Credentials,
                                                val pattern: String = "") {

    companion object {
        /**
         * Initialize a internal repository object from an existing
         * Gradle IVY repository.
         *
         * @param repo Ivy artifact repository
         * @param pattern Ivy pattern
         *
         * @return the internal repository object
         */
        @JvmStatic
        fun initIvyRepoFrom(repo: IvyArtifactRepository, pattern: String) : Repository {
            return Repository(RepositoryType.IVY, repo.url, Credentials.initFrom(repo), pattern)
        }

        /**
         * Initialize a internal repository object from an existing
         * Gradle Maven repository.
         *
         * @param repo Maven artifact repository
         *
         * @return the internal repository object
         */
        @JvmStatic
        fun initMavenRepoFrom(repo: MavenArtifactRepository) : Repository {
            return Repository(RepositoryType.MAVEN, repo.url, Credentials.initFrom(repo))
        }
    }

    /**
     * URL string object of the repository object.
     */
    val urlStr: String
        get() = url.toURL().toString()

    /**
     * Maven basic artifact path if available.
     *
     * @property artifactPath artifact path of a maven repo, if available
     */
    var artifactPath = ""

    /**
     * The final version of an Ivy repository.
     *
     * @property version final version in an IVY repository.
     */
    var version = ""
}
