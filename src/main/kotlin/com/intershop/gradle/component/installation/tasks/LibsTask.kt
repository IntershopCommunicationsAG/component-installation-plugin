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

import com.intershop.gradle.component.installation.utils.data.LibData
import com.intershop.gradle.component.installation.utils.getValue
import com.intershop.gradle.component.installation.utils.setValue
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Nested
import org.gradle.ivy.IvyDescriptorArtifact
import org.gradle.ivy.IvyModule
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact

open class LibsTask: AInstallTask() {

    private val installPathProperty: Property<String> = project.objects.property(String::class.java)
    private val libDataProperty: SetProperty<LibData> = project.objects.setProperty(LibData::class.java)

    @get:Nested
    var libData: Set<LibData> by libDataProperty

    fun provideLibData(libdata: Provider<Set<LibData>>) = libDataProperty.set(libdata)

    fun addLibData(data: LibData) {
        libDataProperty.add(data)
    }

    override fun specifyCopyConfiguration() {
        val fileMap = getArtifacts()
        val fileCollection = project.files(fileMap.keys)

        super.from(fileCollection)

        super.eachFile {
            val newName = fileMap[it.file.absolutePath]
            it.name = newName ?: it.name
        }
    }

    private fun getArtifacts(): Map<String, String> {
        try {
            val libDataFileMap = mutableMapOf<String, String>()
            val list = libData.map { project.dependencies.create(it.getString()) as ModuleDependency }

            val conf = project.configurations.maybeCreate("config${this.name.capitalize()}")
            conf.description = "Configuration for Libraries Installation of ComponentName"
            conf.isTransitive = false

            conf.defaultDependencies {
                it.addAll(list)
            }

            val artifacts = conf.resolvedConfiguration.firstLevelModuleDependencies

            artifacts.forEach { rd ->
                rd.allModuleArtifacts.forEach { ra ->
                    val data = libData.find { it.getString() == ra.id.componentIdentifier.displayName }
                    if(data != null) {
                        libDataFileMap[ra.file.path] = "${data.path}.${ra.file.extension}"
                    } else {
                        libDataFileMap[ra.file.path] = ra.file.name
                    }
                }
            }

            val componentIds = conf.incoming.resolutionResult.allDependencies.mapNotNull {
                (it as? ResolvedDependencyResult)?.selected?.id
            }

            val ivyArtifactResolutionResult = project.dependencies.createArtifactResolutionQuery().forComponents(componentIds).
                    withArtifacts(IvyModule::class.java, IvyDescriptorArtifact::class.java).execute()

            ivyArtifactResolutionResult.resolvedComponents.forEach {
                it.getArtifacts(org.gradle.api.component.Artifact::class.java).forEach { ra ->
                    if(ra is ResolvedArtifactResult) {
                        val data = libData.find { it.getString() == ra.id.componentIdentifier.displayName }
                        if(data != null) {
                            libDataFileMap[ra.file.path] = "${data.path}.${ra.file.extension}"
                        } else {
                            libDataFileMap[ra.file.path] = ra.file.name
                        }
                    }
                }
            }

            val pomArtifactResolutionResult = project.dependencies.createArtifactResolutionQuery().forComponents(componentIds).
                    withArtifacts(MavenModule::class.java,  MavenPomArtifact::class.java).execute()

            pomArtifactResolutionResult.resolvedComponents.forEach {
                it.getArtifacts(org.gradle.api.component.Artifact::class.java).forEach { ra ->
                    if(ra is ResolvedArtifactResult) {
                        val data = libData.find { it.getString() == ra.id.componentIdentifier.displayName }
                        if(data != null) {
                            libDataFileMap[ra.file.path] = "${data.path}.${ra.file.extension}"
                        } else {
                            libDataFileMap[ra.file.path] = ra.file.name
                        }
                    }
                }
            }

            return libDataFileMap
        } catch(ex: ResolveException) {
            throw ResolveException("modules in library configuration of '${name}'", ex)
        }
    }
}