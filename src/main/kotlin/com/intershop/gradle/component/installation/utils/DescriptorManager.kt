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

package com.intershop.gradle.component.installation.utils

import com.intershop.gradle.component.installation.utils.data.Artifact
import com.intershop.gradle.component.installation.utils.data.Credentials
import com.intershop.gradle.component.installation.utils.data.Dependency
import com.intershop.gradle.component.installation.utils.data.Repository
import com.intershop.gradle.component.installation.utils.data.RepositoryType
import org.apache.ivy.core.IvyPatternHelper
import org.gradle.api.GradleException
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.slf4j.LoggerFactory
import java.io.File
import java.io.IOException

class DescriptorManager(val repositories: RepositoryHandler, val descriptor: Dependency, val ivyPattern: Set<String>) {

    companion object {
        private val logger = LoggerFactory.getLogger(DescriptorManager::class.java)

        const val DESCRIPTOR_NAME = "component"

        @JvmStatic
        private fun getPathFromIvy(dependency: Dependency, repo: Repository, artifact: Artifact) : String {
            val pathBuilder = StringBuilder(repo.url)
            if(!pathBuilder.endsWith("/")) {
                pathBuilder.append("/")
            }
            pathBuilder.append(IvyPatternHelper.substitute(repo.pattern, dependency.group, dependency.module, repo.version,
                        artifact.artifcat, artifact.type, artifact.ext))

            return pathBuilder.toString()
        }
    }



    @Throws(IOException::class, GradleException::class)
    fun loadDescriptorFile(repoData: Repository, target: File) {
        val artifact = Artifact(descriptor.module, DESCRIPTOR_NAME, DESCRIPTOR_NAME)
        var path = ""

        when(repoData.type) {
            RepositoryType.IVY -> {
                path = getPathFromIvy(descriptor, repoData, artifact)
            }
            RepositoryType.MAVEN -> {
                val pathBuilder = StringBuilder(repoData.artifactPath)
                pathBuilder.append("-")
                pathBuilder.append(DESCRIPTOR_NAME).append(".")
                pathBuilder.append(DESCRIPTOR_NAME)
                path = pathBuilder.toString()
            }
        }
        if(path.isNotBlank()) {
            if(! target.parentFile.mkdirs()) {
                throw GradleException("It is not possible to create directory '${target.parent}'." )
            }
            if(target.exists() && ! (target.isFile && target.canWrite())) {
                throw GradleException("The file '${target.absolutePath} can not be created.")
            }
            if((target.exists() && ! target.delete()) && target.createNewFile()) {
                throw GradleException("The file '${target.absolutePath} can not be recreated.")
            }

            val conn = RepositoryUtil.getUrlconnection(path, repoData.credentials)
            target.copyInputStreamToFile(conn.getInputStream())
        }
    }

    fun getDescriptorRepository(): Repository? {
        val versionRepoMap = mutableMapOf<String, Repository>()

        repositories.forEach repo@{ repo ->
            if((descriptor.version.endsWith("+") || descriptor.version == "latest") || versionRepoMap.size != 1) {
                when (repo) {
                    is IvyArtifactRepository -> {
                        val credentials = Credentials(repo.credentials.username ?: "", repo.credentials.password ?: "")
                        ivyPattern.forEach { pattern ->
                            checkIvyAvailability(Repository(RepositoryType.IVY, repo.url.toURL().toString(), credentials, pattern), versionRepoMap)
                            if (!(descriptor.version.endsWith("+") || descriptor.version == "latest") && versionRepoMap.size == 1) {
                                return@repo
                            }
                        }
                    }
                    is MavenArtifactRepository -> {
                        val credentials = Credentials(repo.credentials.username ?: "", repo.credentials.password ?: "")
                        checkMavenAvailability(Repository(RepositoryType.MAVEN, repo.url.toURL().toString(), credentials), versionRepoMap)
                    }
                    else -> {
                        throw GradleException("This kind of repository configuration for '${repo.name}'" +
                                " is not supported by the 'component-install-plugin' - wrong Gradle configuration.")
                    }
                }
            }
        }

        return if(versionRepoMap.size > 0) {
            val version = versionRepoMap.keys.sortedWith(VersionComparator()).last()
            versionRepoMap[version]
        } else {
            null
        }
    }

    private fun checkIvyAvailability(repoData: Repository, versionRepoMap: MutableMap<String, Repository>) {
        try {
            val version = RepositoryUtil.addIvyVersion(descriptor, repoData)
            if(version.isNotBlank()) {
                if(! versionRepoMap.containsKey(version)) {
                    versionRepoMap[version] = repoData
                }
            }
        } catch (ex: IOException) {
            logger.info("Dependency {} is not available in {}.", descriptor.getDependencyString(), repoData.url)
        }
    }

    private fun checkMavenAvailability(repoData: Repository, versionRepoMap: MutableMap<String, Repository>) {
        try {
            val version = RepositoryUtil.addMavenArtifactPath(descriptor, repoData)
            if(version.isNotBlank()) {
                if(! versionRepoMap.containsKey(version)) {
                    versionRepoMap[version] = repoData
                }
            }
        } catch (ex: IOException) {
            logger.info("Dependency {} is not available in {}.", descriptor.getDependencyString(), repoData.url)
        }
    }
}