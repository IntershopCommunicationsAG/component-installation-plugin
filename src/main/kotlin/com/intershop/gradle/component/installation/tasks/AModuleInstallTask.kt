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

import com.intershop.gradle.component.installation.utils.data.Artifact
import com.intershop.gradle.component.installation.utils.getValue
import com.intershop.gradle.component.installation.utils.setValue
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Internal
import org.gradle.internal.impldep.org.eclipse.jgit.lib.ObjectChecker.type

open class AModuleInstallTask : AInstallTask() {

    private val packagesProperty: SetProperty<String> = project.objects.setProperty(String::class.java)
    private val jarsProperty: SetProperty<String> = project.objects.setProperty(String::class.java)


    @get:Internal
    var packages: Set<String> by packagesProperty

    fun providePackages(pkgs: Provider<Set<String>>) = packagesProperty.set(pkgs)

    @get:Internal
    var jars: Set<String> by jarsProperty

    fun provideJars(jars: Provider<Set<String>>) = jarsProperty.set(jars)




    protected fun getArtifacts(dependency: Any, artifact: Artifact) : Set<ResolvedArtifact> {
        val dep = project.dependencies.create(dependency) as ModuleDependency
        dep.isTransitive = false
        if(artifact.isNotEmpty()) {
            dep.artifact({
                if(artifact.ext.isNotEmpty()) {
                    it.extension = artifact.ext
                }
                if(type.isNotEmpty()) {
                    it.type = artifact.type
                }
                if(name.isNotEmpty()) {
                    it.name = artifact.artifact
                }
            })
        }

        val conf = project.configurations.detachedConfiguration(dep)
        conf.description = "Configuration for ${this.name}"
        conf.isTransitive = false
        conf.isVisible = false
        return conf.resolvedConfiguration.firstLevelModuleDependencies.first().allModuleArtifacts
    }
}
