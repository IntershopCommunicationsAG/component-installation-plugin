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
package com.intershop.gradle.component.installation.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.*
import java.io.File

open class DeployComponent : DefaultTask() {

    private val jarSources: ConfigurableFileCollection = project.files()

    @get:Input
    var dependencyProperty: String? = null

    @get:Internal
    var targetPath: String = ""

    @get:Internal
    var targetPathInternal: String = ""

    @get:Internal
    var installTarget: File? = null

    @get:Internal
    var jars: Set<String> = mutableSetOf()

    @Suppress("private")
    @get:OutputDirectory
    val outputDirectory: File
        get() {
            var tp = targetPath
            if(targetPathInternal.isNotBlank()) {
                tp = targetPathInternal
            }
            return File(installTarget, tp)
        }

    @get:InputFiles
    val jarFiles: FileCollection
        get() = jarSources

    @Suppress("private")
    @get:InputFile
    val ivyFile: File?
        get() =  getIvyFile(dependencyProperty!!)

    @Suppress("unused")
    @TaskAction
    fun runComponentDepyment(){
        jars.forEach {
            jarSources.from(getJarFile(dependencyProperty!!, it))
        }

        project.copy {
            it.from(ivyFile)
            it.from(jarSources) {
                it.into("release/libs")
            }
            it.into(outputDirectory)
        }
    }

    private fun getArtifacts(dependency: String, extension: String = "", type: String = "", name: String = "") : Set<ResolvedArtifact> {
        val dep = project.dependencies.create(dependency) as ModuleDependency
        dep.isTransitive = false
        if(extension.isNotEmpty() && type.isNotEmpty() && name.isNotEmpty()) {
            dep.artifact({
                if(extension.isNotEmpty()) {
                    it.extension = extension
                }
                if(type.isNotEmpty()) {
                    it.type = type
                }
                if(name.isNotEmpty()) {
                    it.name = name
                }
            })
        }

        val conf = project.configurations.detachedConfiguration(dep)
        conf.description = "Configuration for ${this.name}"
        conf.isTransitive = false
        conf.isVisible = false
        return conf.resolvedConfiguration.firstLevelModuleDependencies.first().allModuleArtifacts
    }

    fun getJarFile(dependency: String, name: String): File? {
        val artifacts = getArtifacts(dependency, "jar", "jar", name)
        return if (! artifacts.isEmpty()) artifacts.first().file else null
    }

    fun getIvyFile(dependency: String): File? {
        val artifacts = getArtifacts(dependency, "xml", "ivy", "ivy")
        return if (! artifacts.isEmpty()) artifacts.first().file else null
    }
}