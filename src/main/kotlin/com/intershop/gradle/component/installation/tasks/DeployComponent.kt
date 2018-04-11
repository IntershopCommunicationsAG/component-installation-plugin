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
package com.intershop.gradle.component.installation.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction

open class DeployComponent : AInstallTask() {

    private val jarSources: ConfigurableFileCollection = project.files()

    @get:Input
    var dependencyProperty: String = ""



    @get:InputFiles
    val jarFiles: FileCollection
        get() = jarSources
/**
    @Suppress("private")
    @get:InputFile
    val ivyFile: File?
        get() =  getIvyFile(dependencyProperty)
**/
    @Suppress("unused")
    @TaskAction
    fun runComponentDepyment(){
        /**
        jars.forEach {
            jarSources.from(getJarFile(dependencyProperty, it))
        }

        project.copy {
            it.from(ivyFile)
            it.from(jarSources) {
                it.into("release/libs")
            }
            it.into(outputDir)
        }
        **/
    }
/**
    fun getJarFile(dependency: String, name: String): File? {
        //val artifacts = getArtifacts(dependency, Artifact(name, "jar", "jar"))
        return if (! artifacts.isEmpty()) artifacts.first().file else null
    }

    fun getIvyFile(dependency: String): File? {
        //val artifacts = getArtifacts(dependency, Artifact("ivy", "ivy", "xml"))
        return if (! artifacts.isEmpty()) artifacts.first().file else null
    }
    **/
}
