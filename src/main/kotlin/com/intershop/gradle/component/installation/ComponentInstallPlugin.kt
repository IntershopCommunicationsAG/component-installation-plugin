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

import com.intershop.gradle.component.descriptor.util.ComponentUtil
import com.intershop.gradle.component.installation.extension.InstallationExtension
import com.intershop.gradle.component.installation.extension.OSType
import com.intershop.gradle.component.installation.tasks.ModuleTask
import com.intershop.gradle.component.installation.tasks.PackageTask
import com.intershop.gradle.component.installation.utils.ContentType
import com.intershop.gradle.component.installation.utils.DescriptorManager
import com.intershop.gradle.component.installation.utils.data.Artifact
import com.intershop.gradle.component.installation.utils.data.FileItem
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

    override fun apply(project: Project) {
        with(project) {
            logger.info("Install plugin adds extension {} to {}",
                    InstallationExtension.INSTALLATION_EXTENSION_NAME, name)

            val extension = extensions.findByType(InstallationExtension::class.java)
                    ?: extensions.create(InstallationExtension.INSTALLATION_EXTENSION_NAME,
                            InstallationExtension::class.java, project)

            tasks.maybeCreate("install").group = InstallationExtension.INSTALLATION_GROUP_NAME
            tasks.maybeCreate("update").group = InstallationExtension.INSTALLATION_GROUP_NAME

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
        }

        @Defaults
        fun configureDeploymentTasks(tasks: ModelMap<Task>,
                                     installExtension: InstallationExtension) {

            with(installExtension) {
                if(detectedOS == OSType.OTHER) {
                    throw GradleException("The operating system is not suppported by the component install plugin!")
                }

                components.forEach {
                    val descriptorMgr =
                            DescriptorManager(project.repositories, it.dependency, installConfig.ivyPatterns)

                    val descriptorRepo = descriptorMgr.getDescriptorRepository()
                    if(descriptorRepo != null) {
                        val targetFile = File(installConfig.installAdminDir,
                                "${it.commonName}/components/component.component")

                        descriptorMgr.loadDescriptorFile(descriptorRepo, targetFile)
                        descriptorMgr.validateDescriptor(targetFile)

                        val component = ComponentUtil.componentFromFile(targetFile)

                        val fileItemSet = mutableSetOf<FileItem>()

                        component.fileItems.forEach { file ->
                            val localFile = File(installConfig.installAdminDir,
                                    "${it.commonName}/files/${file.name}.${file.extension}")

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

                        val installTask = tasks.get("install")
                        val updateTask = tasks.get("update")

                        component.fileContainers.forEach { pkg ->

                            val pkgFile = File(installConfig.installAdminDir,
                                    "${it.commonName}/pkgs/${pkg.name}.zip")

                            val classifier = pkg.classifier

                            if(checkForOSandType(detectedOS, classifier, this.environment, pkg.types)) {
                                val artifact = if (classifier.isNotBlank() && OSType.from(classifier) == detectedOS) {
                                    Artifact(pkg.name, pkg.itemType, "zip", classifier)
                                } else {
                                    Artifact(pkg.name, pkg.itemType, "zip")
                                }

                                descriptorMgr.loadArtifactFile(descriptorRepo, artifact, pkgFile)

                                val taskNameSuffix = "Pkg${pkg.name.capitalize()}${pkg.itemType.capitalize()}"

                                tasks.create("install${taskNameSuffix}", PackageTask::class.java) {
                                    it.runUpdate = false
                                    it.fileContainer = pkgFile

                                    it.installDir = calculateInstallDir(this.installDir, component.containerTarget)
                                    it.installPath = pkg.targetPath
                                    it.targetIncluded = pkg.targetIncluded
                                    it.excludesFromUpdate = pkg.excludesFromUpdate
                                    it.fileItems = fileItemSet
                                }

                                installTask?.dependsOn("install${taskNameSuffix}")

                                if(! pkg.excludeFromUpdate) {
                                    tasks.create("update${taskNameSuffix}", PackageTask::class.java) {
                                        it.runUpdate = true
                                        it.fileContainer = pkgFile

                                        it.installDir = calculateInstallDir(this.installDir, component.containerTarget)
                                        it.installPath = pkg.targetPath
                                        it.targetIncluded = pkg.targetIncluded
                                        it.excludesFromUpdate = pkg.excludesFromUpdate
                                        it.fileItems = fileItemSet
                                    }

                                    updateTask?.dependsOn("update${taskNameSuffix}")
                                }
                            }
                        }

                        component.modules.forEach { module ->
                            if(checkForType(this.environment, module.value.types)) {
                                with(module.value)  module@{
                                    val taskNameSuffix = module.value.name.capitalize()
                                    tasks.create("install${taskNameSuffix}", ModuleTask::class.java) {
                                        it.runUpdate = false
                                        it.dependency = dependency.toString()
                                        it.jarPath = jarPath
                                        it.descriptorPath = descriptorPath
                                        it.classifiers = classifiers
                                        it.jars = jars
                                        it.pkgs = pkgs
                                        it.moduleName = name

                                        it.installDir = calculateInstallDir(this@with.installDir, component.modulesTarget)
                                        it.installPath = module.key
                                        it.targetIncluded = targetIncluded
                                        it.excludesFromUpdate = excludesFromUpdate
                                        it.fileItems = fileItemSet
                                    }

                                    installTask?.dependsOn("install${taskNameSuffix}")

                                    if (!module.value.excludeFromUpdate) {
                                        tasks.create("update${taskNameSuffix}", ModuleTask::class.java) {
                                            it.runUpdate = true
                                            it.dependency = dependency.toString()
                                            it.jarPath = jarPath
                                            it.descriptorPath = descriptorPath
                                            it.classifiers = classifiers
                                            it.jars = jars
                                            it.pkgs = pkgs
                                            it.moduleName = name

                                            it.installDir = calculateInstallDir(this@with.installDir, component.modulesTarget)
                                            it.installPath = module.key
                                            it.targetIncluded = targetIncluded
                                            it.excludesFromUpdate = excludesFromUpdate
                                            it.fileItems = fileItemSet
                                        }

                                        updateTask?.dependsOn("update${taskNameSuffix}")
                                    }

                                }
                            }
                        }


                        println(component.libs.size)
                        println(component.properties.size)
                    } else {
                        throw GradleException("Component '${it.dependency.getDependencyString()}' is " +
                                "not found in configured repositories. See log files for more information.")
                    }
                }
            }
        }
    }
}
