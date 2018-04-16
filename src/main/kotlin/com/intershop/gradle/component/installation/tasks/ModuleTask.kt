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

import com.intershop.gradle.component.installation.extension.OSType
import com.intershop.gradle.component.installation.utils.DependencyConfig
import com.intershop.gradle.component.installation.utils.getValue
import com.intershop.gradle.component.installation.utils.setValue
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal

open class ModuleTask : AInstallTask() {

    @get:Synchronized
    private var filesInit = true

    private val moduleNameProperty: Property<String> = project.objects.property(String::class.java)

    private val dependencyProperty: Property<DependencyConfig> = project.objects.property(DependencyConfig::class.java)

    private val jarPathProperty: Property<String> = project.objects.property(String::class.java)
    private val descriptorPathProperty: Property<String> = project.objects.property(String::class.java)

    private val pkgsProperty: SetProperty<String> = project.objects.setProperty(String::class.java)
    private val jarsProperty: SetProperty<String> = project.objects.setProperty(String::class.java)
    private val classifiersProperty: SetProperty<String> = project.objects.setProperty(String::class.java)

    @get:Internal
    var moduleName: String by moduleNameProperty

    fun provideModuleName(moduleName: Provider<String>) = moduleNameProperty.set(moduleName)

    @get:Input
    var dependency: DependencyConfig
        get() {
            val dependency = dependencyProperty.get() ?: throw GradleException("It is necessary to specify a dependency!")
            with(dependency.version.toLowerCase()) {
                super.getOutputs().upToDateWhen {
                    ! endsWith("snapshot") && ! endsWith("local")
                }
            }
            return dependency
        }
        set(value) = dependencyProperty.set(value)

    fun provideDependency(dependency: Provider<DependencyConfig>) = dependencyProperty.set(dependency)

    @get:Input
    var jarPath: String by jarPathProperty

    fun provideJarPath(jarPath: Provider<String>) = jarPathProperty.set(jarPath)

    @get:Input
    var descriptorPath: String by descriptorPathProperty

    fun provideDescriptorPath(descriptorPath: Provider<String>) = descriptorPathProperty.set(descriptorPath)

    @get:Input
    var pkgs: Set<String> by pkgsProperty

    fun providePkgs(pkgs: Provider<Set<String>>) = pkgsProperty.set(pkgs)

    @get:Input
    var jars: Set<String> by jarsProperty

    fun provideJars(jars: Provider<Set<String>>) = jarsProperty.set(jars)

    @get:Input
    var classifiers: Set<String> by classifiersProperty

    fun provideClassifiers(classifiers: Provider<Set<String>>) = classifiersProperty.set(classifiers)

    @get:Input
    val detectedOS: OSType
        get() = OSType.detectedOS()

    override fun specifyCopyConfiguration() {
        val ivyFiles: ConfigurableFileCollection = project.files()
        val jarFiles: ConfigurableFileCollection = project.files()
        val pkgFiles: ConfigurableFileCollection = project.files()

        initFiles(jarFiles, pkgFiles, ivyFiles)

        super.from(ivyFiles) {
            it.into(descriptorPath)
        }
        super.from(jarFiles) {
            it.into(jarPath)
        }

        pkgFiles.forEach {
            super.from(project.zipTree(it))
        }
    }

    private fun initFiles(jarFiles: ConfigurableFileCollection,
                          pkgFiles: ConfigurableFileCollection,
                          ivyFiles: ConfigurableFileCollection) {

        getArtifacts().forEach {
            if (it.type == "jar" && it.extension == "jar" && jars.contains(it.name)) {
                jarFiles.from(it.file)
            }

            if (it.extension == "zip" &&
                    (OSType.from(it.classifier ?: "") == detectedOS || it.classifier.isNullOrBlank())) {
                pkgFiles.from(it.file)
            }
            if (it.type == "ivy" && it.extension == "xml" && it.name == "ivy") {
                ivyFiles.from(it.file)
            }
        }
    }

    private fun getArtifacts() : Set<ResolvedArtifact> {
        try {
            val ivyDep = project.dependencies.create(dependency) as ModuleDependency

            ivyDep.artifact {
                it.name = "ivy"
                it.type = "ivy"
                it.extension = "xml"
            }

            val conf = project.configurations.create("config${this.name.capitalize()}")
            conf.description = "Configuration for ${this.moduleName}"
            conf.isTransitive = false
            conf.defaultDependencies {
                it.add(project.dependencies.create(dependency))
                it.add(project.dependencies.create(ivyDep))
            }

            return conf.resolvedConfiguration.firstLevelModuleDependencies.first().allModuleArtifacts

        }catch(ex: ResolveException) {
            throw ResolveException("modules of '${dependency}' in '${this.moduleName}'", ex)
        }
    }
}
