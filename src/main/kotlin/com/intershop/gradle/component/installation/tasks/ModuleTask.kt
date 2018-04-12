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
import com.intershop.gradle.component.installation.utils.getValue
import com.intershop.gradle.component.installation.utils.setValue
import org.gradle.api.GradleException
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.artifacts.ResolvedArtifact
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.CopySpec
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction

open class ModuleTask : AInstallTask() {

    private val ivyFilesCollection: ConfigurableFileCollection = project.files()
    private val jarFilesCollection: ConfigurableFileCollection = project.files()
    private val pkgFilesCollection: ConfigurableFileCollection = project.files()

    @get:Synchronized
    private var filesInit = true

    private val moduleNameProperty: Property<String> = project.objects.property(String::class.java)

    private val dependencyProperty: Property<String> = project.objects.property(String::class.java)

    private val jarPathProperty: Property<String> = project.objects.property(String::class.java)
    private val descriptorPathProperty: Property<String> = project.objects.property(String::class.java)

    private val pkgsProperty: SetProperty<String> = project.objects.setProperty(String::class.java)
    private val jarsProperty: SetProperty<String> = project.objects.setProperty(String::class.java)
    private val classifiersProperty: SetProperty<String> = project.objects.setProperty(String::class.java)

    @get:Internal
    var moduleName: String by moduleNameProperty

    fun provideModuleName(moduleName: Provider<String>) = moduleNameProperty.set(moduleName)

    @get:Input
    var dependency: String by dependencyProperty

    fun provideDependency(dependency: Provider<String>) = dependencyProperty.set(dependency)

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

    @get:InputFiles
    @get:Throws(GradleException::class)
    val jarFiles: FileCollection
        get() {
            if(filesInit) {
                initFiles()
            }
            return jarFilesCollection
        }

    @get:InputFiles
    val pkgFiles: FileCollection
        get() {
            if(filesInit) {
                initFiles()
            }
            return pkgFilesCollection
        }

    @get:InputFiles
    val ivyFiles: FileCollection
        get() {
            if(filesInit) {
                initFiles()
            }
            return ivyFilesCollection
        }

    @TaskAction
    fun runInstall() {
        if(! outputDir.exists()) {
            throw GradleException("The target directory '${outputDir}' does not exists!")
        }
        if(! runUpdate) {
            project.sync { configureCopySpec(it) }
        } else {
            project.copy { configureCopySpec(it) }
        }
    }

    protected fun configureCopySpec(spec: CopySpec, update: Boolean = false) {
        spec.from(ivyFiles) {
            it.into(descriptorPath)
        }
        spec.from(jarFiles) {
            it.into(jarPath)
        }
        spec.into(outputDir)

        pkgFiles.forEach {
            spec.from(project.zipTree(it))
        }

        finalizeSpec(spec, update)
    }

    private fun initFiles() {
        getArtifacts().forEach {
            if(it.type == "jar" && it.extension == "jar" && jars.contains(it.name)) {
                jarFilesCollection.from(it.file)
            }

            if(it.extension == "zip" &&
                    (OSType.from(it.classifier ?: "") == detectedOS || it.classifier.isNullOrBlank())) {
                pkgFilesCollection.from(it.file)
            }
            if(it.type == "ivy" && it.extension == "xml" && it.name == "ivy") {
                ivyFilesCollection.from(it.file)
            }
        }
        filesInit = false
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
