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
package com.intershop.gradle.component.installation

import com.intershop.gradle.component.descriptor.FileContainer
import com.intershop.gradle.component.descriptor.Module
import com.intershop.gradle.component.descriptor.util.ComponentUtil
import com.intershop.gradle.component.installation.extension.InstallationExtension
import com.intershop.gradle.component.installation.extension.OSType
import com.intershop.gradle.component.installation.tasks.LibsTask
import com.intershop.gradle.component.installation.tasks.ModuleTask
import com.intershop.gradle.component.installation.tasks.PackageTask
import com.intershop.gradle.component.installation.utils.ContentType
import com.intershop.gradle.component.installation.utils.DescriptorManager
import com.intershop.gradle.component.installation.utils.data.Artifact
import com.intershop.gradle.component.installation.utils.data.FileItem
import com.intershop.gradle.component.installation.utils.data.LibData
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.model.Defaults
import org.gradle.model.ModelMap
import org.gradle.model.RuleSource
import org.gradle.model.internal.core.ModelPath
import org.gradle.model.internal.core.ModelReference
import org.gradle.model.internal.core.ModelRegistrations
import org.gradle.model.internal.registry.ModelRegistry
import org.slf4j.LoggerFactory
import java.io.File
import javax.inject.Inject

@Suppress("unused")
class ComponentInstallPlugin @Inject constructor(private val modelRegistry: ModelRegistry) : Plugin<Project> {

    companion object {
        private val MAININSTALL = Triple("install", "Component Installation", "Run installation of ")
        private val MAINUPDATE = Triple("update", "Component Update", "Run update of ")
    }

    override fun apply(project: Project) {
        with(project) {
            logger.info("Install plugin adds extension {} to {}",
                    InstallationExtension.INSTALLATION_EXTENSION_NAME, name)

            val extension = extensions.findByType(InstallationExtension::class.java)
                    ?: extensions.create(InstallationExtension.INSTALLATION_EXTENSION_NAME,
                            InstallationExtension::class.java, project)

            tasks.maybeCreate(MAININSTALL.first).group = MAININSTALL.second
            tasks.maybeCreate(MAINUPDATE.first).group = MAINUPDATE.second

            if(modelRegistry.state(ModelPath.nonNullValidatedPath("installExtension")) == null) {
                modelRegistry.register(ModelRegistrations.bridgedInstance(
                        ModelReference.of("installExtension", InstallationExtension::class.java), extension)
                        .descriptor("component install configuration").build())
            }
        }
    }

    @Suppress("unused")
    class InstallRule : RuleSource() {

        companion object {
            val LOGGER = LoggerFactory.getLogger(InstallRule::class.java)

            fun checkForOSandType(detectedOS: OSType,
                                  classifier: String,
                                  environment: Set<String>,
                                  types: Set<String>): Boolean {
                var rv = (classifier.isNotBlank() && OSType.from(classifier) == detectedOS) || classifier.isBlank()
                rv = rv && checkForType(environment, types)
                return rv
            }

            fun checkForType(environment: Set<String>,
                             types: Set<String>): Boolean {
                return if(! types.isEmpty() && ! environment.isEmpty()) {
                    types.intersect(environment).size > 0
                } else {
                    true
                }
            }

            fun calculatFilePath(fileTarget: String, fileName: String, fileExtension: String): String{
                val path = StringBuilder(fileTarget)
                if(! path.endsWith("/")) {
                    path.append("/")
                }
                path.append(fileName)
                if(fileExtension.isNotBlank()) {
                    if(! path.endsWith(".")) {
                        path.append(".")
                    }
                    path.append(fileExtension)
                }
                return path.toString()
            }

            fun calculateInstallDir(installDir: File, path: String) : File {
                return if(path.isNotBlank()) {
                    File(installDir, path)
                } else {
                    installDir
                }
            }

            fun createComponentTasks(tasks: ModelMap<Task>,
                                     name: String,
                                     commonName: String,
                                     mainconf: Triple<String, String, String>): Task? {
                if(tasks.get(name) == null) {
                    tasks.create(name) {
                        it.group = mainconf.second
                        it.description = "${mainconf.third}'${commonName}'"
                    }
                    val installTask = tasks.get(mainconf.first)
                    installTask?.dependsOn(name)
                }
                return tasks.get(name)
            }

            fun createPackageTask(tasks: ModelMap<Task>,
                                  file: File,
                                  pkg: FileContainer,
                                  installDir: File,
                                  target: String,
                                  taskName: String,
                                  singleFiles: Set<FileItem>,
                                  runUpdate: Boolean = false) {
                if(tasks.get(taskName) == null) {
                    tasks.create(taskName, PackageTask::class.java)
                }
                val pkgTask = tasks.get(taskName)
                if(pkgTask is PackageTask) {
                    with(pkgTask) {
                        this.runUpdate = runUpdate
                        this.fileContainer = file

                        this.installDir = calculateInstallDir(installDir, target)
                        this.installPath = pkg.targetPath
                        this.targetIncluded = pkg.targetIncluded
                        this.excludesFromUpdate = pkg.excludesFromUpdate
                        this.fileItems = singleFiles
                    }
                } else {
                    throw GradleException("Task '${taskName} exists, but it has the wrong type!")
                }
            }

            fun createModuleTask(tasks: ModelMap<Task>,
                                 module: Module,
                                 modulePath: String,
                                 installDir: File,
                                 target: String,
                                 taskName: String,
                                 singleFiles: Set<FileItem>,
                                 runUpdate: Boolean = false) {
                if(tasks.get(taskName) == null) {
                    tasks.create(taskName, ModuleTask::class.java)
                }
                val moduleTask = tasks.get(taskName)
                if(moduleTask is ModuleTask) {
                    with(moduleTask) {
                        this.runUpdate = runUpdate
                        this.dependency = module.dependency.toString()
                        this.jarPath = module.jarPath
                        this.descriptorPath = module.descriptorPath
                        this.classifiers = module.classifiers
                        this.jars = module.jars
                        this.pkgs = module.pkgs
                        this.moduleName = module.name

                        this.installDir = calculateInstallDir(installDir, target)
                        this.installPath = modulePath
                        this.targetIncluded = module.targetIncluded
                        this.excludesFromUpdate = module.excludesFromUpdate
                        this.fileItems = singleFiles
                    }
                }
            }
        }

        @Defaults
        fun configureDeploymentTasks(tasks: ModelMap<Task>,
                                     installExtension: InstallationExtension) {

            with(installExtension) {
                if(detectedOS == OSType.OTHER) {
                    throw GradleException("The operating system is not suppported by the component install plugin!")
                }

                components.forEach { componentDep ->
                    val descriptorMgr =
                            DescriptorManager(project.repositories, componentDep.dependency, installConfig.ivyPatterns)

                    val descriptorRepo = descriptorMgr.getDescriptorRepository()
                    if(descriptorRepo != null) {
                        val targetFile = File(installConfig.installAdminDir,
                                "${componentDep.commonName}/components/component.component")

                        descriptorMgr.loadDescriptorFile(descriptorRepo, targetFile)
                        descriptorMgr.validateDescriptor(targetFile)

                        val component = ComponentUtil.componentFromFile(targetFile)

                        val fileItemSet = mutableSetOf<FileItem>()

                        component.fileItems.forEach { file ->
                            val localFile = File(installConfig.installAdminDir,
                                    "${componentDep.commonName}/files/${file.name}.${file.extension}")

                            val classifier = file.classifier

                            if(checkForOSandType(detectedOS, classifier, this.environment, file.types)) {
                                val artifact = if (classifier.isNotBlank() && OSType.from(classifier) == detectedOS) {
                                    Artifact(file.name, file.extension, file.extension, classifier)
                                } else {
                                    Artifact(file.name, file.extension, file.extension)
                                }

                                descriptorMgr.loadArtifactFile(descriptorRepo, artifact, localFile)

                                fileItemSet.add(FileItem(targetFile,
                                        calculatFilePath(file.targetPath, file.name, file.extension),
                                        ContentType.valueOf(file.contentType.toString()), file.excludeFromUpdate))
                            }
                        }

                        val installPrefix = "${MAININSTALL.first}${componentDep.commonName.capitalize()}"
                        val updatePrefix = "${MAINUPDATE.first}${componentDep.commonName.capitalize()}"

                        val installTask = createComponentTasks(tasks, installPrefix,
                                componentDep.commonName, MAININSTALL)

                        val updateTask = createComponentTasks(tasks, updatePrefix,
                                componentDep.commonName, MAINUPDATE)

                        component.fileContainers.forEach { pkg ->

                            val pkgFile = File(installConfig.installAdminDir,
                                    "${componentDep.commonName}/pkgs/${pkg.name}.zip")

                            val classifier = pkg.classifier

                            if(checkForOSandType(detectedOS, classifier, this.environment, pkg.types)) {
                                val artifact = if (classifier.isNotBlank() && OSType.from(classifier) == detectedOS) {
                                    Artifact(pkg.name, pkg.itemType, "zip", classifier)
                                } else {
                                    Artifact(pkg.name, pkg.itemType, "zip")
                                }

                                descriptorMgr.loadArtifactFile(descriptorRepo, artifact, pkgFile)

                                val taskNameSuffix = "Pkg${pkg.name.capitalize()}${pkg.itemType.capitalize()}"

                                createPackageTask(tasks, pkgFile, pkg, this.installDir, component.containerTarget,
                                        "$installPrefix$taskNameSuffix", fileItemSet)

                                installTask?.dependsOn("$installPrefix$taskNameSuffix")

                                if(! pkg.excludeFromUpdate) {
                                    createPackageTask(tasks, pkgFile, pkg, this.installDir, component.containerTarget,
                                            "$updatePrefix$taskNameSuffix", fileItemSet, true)

                                    updateTask?.dependsOn("$updatePrefix$taskNameSuffix")
                                }
                            }
                        }

                        component.modules.forEach { module ->
                            if(checkForType(this.environment, module.value.types)) {
                                val taskNameSuffix = "Module${module.value.name.capitalize()}"
                                createModuleTask(tasks, module.value, module.key, this.installDir,
                                        component.modulesTarget, "$installPrefix$taskNameSuffix",
                                        fileItemSet)

                                installTask?.dependsOn("$installPrefix$taskNameSuffix")

                                if (!module.value.excludeFromUpdate) {
                                    createModuleTask(tasks, module.value, module.key, this.installDir,
                                            component.modulesTarget, "$updatePrefix$taskNameSuffix",
                                            fileItemSet, true)

                                    updateTask?.dependsOn("$updatePrefix$taskNameSuffix")
                                }
                            }
                        }

                        if(component.libs.size > 0) {

                            tasks.create("installLibs", LibsTask::class.java) {
                                component.libs.values.forEach {lib ->
                                    if(checkForType(this.environment, lib.types)) {
                                        it.addLibData(LibData(lib.dependency.group, lib.dependency.module, lib.dependency.version, lib.targetName))
                                    }
                                }
                                it.installDir = this.installDir
                                it.installPath = component.libsTarget
                            }

                            installTask?.dependsOn("installLibs")
                            updateTask?.dependsOn("installLibs")
                        }

                        println(component.properties.size)
                    } else {
                        throw GradleException("Component '${componentDep.dependency.getDependencyString()}' is " +
                                "not found in configured repositories. See log files for more information.")
                    }
                }
            }
        }
    }
}
