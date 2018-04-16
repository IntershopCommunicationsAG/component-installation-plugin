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
import com.intershop.gradle.component.installation.extension.Component
import com.intershop.gradle.component.installation.extension.InstallationExtension
import com.intershop.gradle.component.installation.extension.OSType
import com.intershop.gradle.component.installation.tasks.LibsTask
import com.intershop.gradle.component.installation.tasks.ModuleTask
import com.intershop.gradle.component.installation.tasks.PackageTask
import com.intershop.gradle.component.installation.utils.ContentType
import com.intershop.gradle.component.installation.utils.DependencyConfig
import com.intershop.gradle.component.installation.utils.DescriptorManager
import com.intershop.gradle.component.installation.utils.TaskConfig
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
import com.intershop.gradle.component.descriptor.Component as ComponentDescr
import com.intershop.gradle.component.descriptor.FileItem as FileItemDescr

@Suppress("unused")
class ComponentInstallPlugin @Inject constructor(private val modelRegistry: ModelRegistry) : Plugin<Project> {

    companion object {
        val INSTALLTASKNAME = "install"
        val INSTALLGROUPNAME = "Component Installation"
    }

    override fun apply(project: Project) {
        with(project) {
            logger.info("Install plugin adds extension {} to {}",
                    InstallationExtension.INSTALLATION_EXTENSION_NAME, name)

            val extension = extensions.findByType(InstallationExtension::class.java)
                    ?: extensions.create(InstallationExtension.INSTALLATION_EXTENSION_NAME,
                            InstallationExtension::class.java, project)

            tasks.maybeCreate(INSTALLTASKNAME).group = INSTALLGROUPNAME

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

            const val CHECKCONF = "Check your configuration!"
            const val DOCUTEXT = "Consult documentation for next steps."

            private fun checkForOSandType(detectedOS: OSType,
                                          classifier: String,
                                          environment: Set<String>,
                                          types: Set<String>): Boolean {
                var rv = (classifier.isNotBlank() && OSType.from(classifier) == detectedOS) || classifier.isBlank()
                rv = rv && checkForType(environment, types)
                return rv
            }

            private fun checkForType(environment: Set<String>,
                                     types: Set<String>): Boolean {
                return if (!types.isEmpty() && !environment.isEmpty()) {
                    types.intersect(environment).size > 0
                } else {
                    true
                }
            }

            private fun getComponentDir(installDir: File,
                                        comp: Component,
                                        defPath: String): File {
                if (comp.path.isBlank() && defPath.isBlank()) {
                    throw GradleException("The install path of '${comp.commonName}' is not " +
                            "configured in '${installDir.absolutePath}'. $CHECKCONF")
                }

                return if (comp.path.isNotBlank()) {
                    File(installDir, comp.path)
                } else {
                    File(installDir, defPath)
                }
            }

            private fun calculatFilePath(fileTarget: String, fileName: String, fileExtension: String): String {
                val path = StringBuilder(fileTarget)
                if (!path.endsWith("/")) {
                    path.append("/")
                }
                path.append(fileName)
                if (fileExtension.isNotBlank()) {
                    if (!path.endsWith(".")) {
                        path.append(".")
                    }
                    path.append(fileExtension)
                }
                return path.toString()
            }

            private fun loadFileItems(ext: InstallationExtension,
                                      taskConfig: TaskConfig,
                                      descrMgr: DescriptorManager,
                                      adminDir: File,
                                      items: Set<FileItemDescr>) {

                taskConfig.fileItemSet.clear()

                items.forEach { file ->
                    val localFile = File(adminDir, "files/${file.name}.${file.extension}")
                    val classifier = file.classifier

                    if (checkForOSandType(ext.detectedOS, classifier, ext.environment, file.types)) {
                        val artifact = if (classifier.isNotBlank() && OSType.from(classifier) == ext.detectedOS) {
                            Artifact(file.name, file.extension, file.extension, classifier)
                        } else {
                            Artifact(file.name, file.extension, file.extension)
                        }

                        descrMgr.loadArtifactFile(artifact, localFile)

                        taskConfig.fileItemSet.add(FileItem(localFile,
                                calculatFilePath(file.targetPath, file.name, file.extension),
                                ContentType.valueOf(file.contentType.toString()), file.updatable))
                    }
                }
            }

            private fun createContainerTasks(ext: InstallationExtension,
                                             taskConfig: TaskConfig,
                                             descrMgr: DescriptorManager,
                                             adminDir: File,
                                             comp: ComponentDescr) {

                comp.fileContainers.forEach { pkg ->
                    val pkgFile = File(adminDir, "pkgs/${pkg.name}.zip")
                    val classifier = pkg.classifier

                    if (checkForOSandType(ext.detectedOS, classifier, ext.environment, pkg.types)) {
                        val artifact = if (classifier.isNotBlank() && OSType.from(classifier) == ext.detectedOS) {
                            Artifact(pkg.name, pkg.itemType, "zip", classifier)
                        } else {
                            Artifact(pkg.name, pkg.itemType, "zip")
                        }

                        descrMgr.loadArtifactFile(artifact, pkgFile)

                        val taskName = taskConfig.getTasknameFor("pkg", pkg.name, pkg.itemType)

                        if (taskConfig.tasks.get(taskName) == null) {
                            taskConfig.tasks.create(taskName, PackageTask::class.java)
                        }
                        val pkgTask = taskConfig.tasks.get(taskName)
                        if (pkgTask is PackageTask) {
                            with(pkgTask) {
                                this.onlyIf { ! pkg.updatable }

                                this.runUpdate = taskConfig.update

                                this.fileContainer = pkgFile
                                this.destinationDir = taskConfig.getTargetDir(comp.containerTarget, pkg.targetPath)
                                this.installPath = pkg.targetPath
                                this.targetIncluded = pkg.targetIncluded
                                this.updateExcludes = pkg.excludes
                                this.fileItems = taskConfig.fileItemSet
                            }
                        } else {
                            throw GradleException("Task '${taskName} exists, but it has the wrong type!")
                        }

                        taskConfig.compInstallTask?.dependsOn(taskName)
                    }
                }
            }

            private fun createModuleTasks(ext: InstallationExtension,
                                          taskConfig: TaskConfig,
                                          comp: ComponentDescr) {

                comp.modules.forEach { module ->

                    if (checkForType(ext.environment, module.value.types)) {

                        val taskName = taskConfig.getTasknameFor("module", module.value.name)

                        if (taskConfig.tasks.get(taskName) == null) {
                            taskConfig.tasks.create(taskName, ModuleTask::class.java)
                        }
                        val moduleTask = taskConfig.tasks.get(taskName)
                        if (moduleTask is ModuleTask) {
                            with(moduleTask) {
                                this.onlyIf { ! module.value.updatable }
                                this.outputs.upToDateWhen {
                                    isDependencyUpToDate(module.value.dependency.version)
                                }

                                this.runUpdate = taskConfig.update

                                this.dependency = DependencyConfig.getFrom(module.value.dependency)
                                this.jarPath = module.value.jarPath
                                this.descriptorPath = module.value.descriptorPath
                                this.classifiers = module.value.classifiers
                                this.jars = module.value.jars
                                this.pkgs = module.value.pkgs
                                this.moduleName = module.value.name

                                this.destinationDir = taskConfig.getTargetDir(comp.modulesTarget, module.key)
                                this.installPath = module.key
                                this.targetIncluded = module.value.targetIncluded
                                this.updateExcludes = module.value.excludes
                                this.fileItems = taskConfig.fileItemSet
                            }
                        } else {
                            throw GradleException("Task '${taskName} exists, but it has the wrong type!")
                        }

                        taskConfig.compInstallTask?.dependsOn(taskName)
                    }
                }
            }

            private fun isDependencyUpToDate(version: String): Boolean {
                return !(version.isBlank() || version.endsWith("SNAPSHOT") || version.endsWith("LOCAL"))
            }
        }

        @Defaults
        fun configureDeploymentTasks(tasks: ModelMap<Task>,
                                     installExtension: InstallationExtension) {

            if(installExtension.detectedOS == OSType.OTHER) {
                throw GradleException("The operating system is not suppported by the component install plugin!")
            }

            val repoHandler = installExtension.project.repositories
            val ivyPatterns = installExtension.installConfig.ivyPatterns
            val adminDir = installExtension.installConfig.installAdminDir

            installExtension.components.forEach { compToInstall ->
                // get URL for dependency
                val descriptorMgr = DescriptorManager(repoHandler, compToInstall.dependency, ivyPatterns)

                // load component configuration
                val compAdminDir = File(adminDir,"${compToInstall.commonName}")
                val targetFile = File(compAdminDir, "components/component.component")
                descriptorMgr.loadDescriptorFile(targetFile)

                // get and verify metadata
                val metadData = descriptorMgr.getDescriptorMetadata(targetFile)

                // read configuration
                val mainDescr = ComponentUtil.componentFromFile(targetFile)

                val compDir = getComponentDir(installExtension.installDir, compToInstall, mainDescr.target)

                val taskConfig = TaskConfig(tasks, compToInstall.commonName, compDir)

                // update mode
                val instDescrFile = File(taskConfig.getTargetDir(mainDescr.descriptorPath), "component.component")
                if(instDescrFile.exists()) {
                    val prevID = ComponentUtil.metadataFromFile(targetFile).componentID

                    if(prevID.group != metadData.componentID.group || prevID.module != metadData.componentID.module) {
                        throw GradleException("The previous installed component was '$prevID'. $CHECKCONF $DOCUTEXT ")
                    }

                    taskConfig.update = true
                }

                // load files from desc
                loadFileItems(installExtension, taskConfig, descriptorMgr, compAdminDir, mainDescr.fileItems)

                // create package tasks
                createContainerTasks(installExtension, taskConfig, descriptorMgr, compAdminDir, mainDescr)

                // create module tasks
                createModuleTasks(installExtension, taskConfig, mainDescr)

                if(mainDescr.libs.size > 0) {

                    val libsTaskname = taskConfig.getTasknameFor("libs")
                    tasks.create(libsTaskname, LibsTask::class.java) {

                        var uptodate = true
                        mainDescr.libs.values.forEach {lib ->
                            if(checkForType(installExtension.environment, lib.types)) {
                                with(lib.dependency) {
                                    it.addLibData(LibData(this.group, this.module, this.version, lib.targetName))
                                    uptodate = uptodate && isDependencyUpToDate(this.version)
                                }
                            }
                        }
                        it.destinationDir = compDir
                        it.installPath = mainDescr.libsTarget
                    }

                    taskConfig.compInstallTask?.dependsOn(libsTaskname)
                }

                println(mainDescr.properties.size)
            }
        }
    }
}
