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

            private fun getTargetDir(baseDir: File, path: String): File {
                return if (path.isNotBlank()) {
                    File(baseDir, path)
                } else {
                    baseDir
                }
            }

            private fun loadFileItems(ext: InstallationExtension,
                                      descrMgr: DescriptorManager,
                                      adminDir: File,
                                      items: Set<FileItemDescr>): Set<FileItem> {
                val fileItemSet = mutableSetOf<FileItem>()

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

                        fileItemSet.add(FileItem(localFile,
                                calculatFilePath(file.targetPath, file.name, file.extension),
                                ContentType.valueOf(file.contentType.toString()), file.excludeFromUpdate))
                    }
                }
                return fileItemSet
            }

            private fun createInstallTask(tasks: ModelMap<Task>,
                                          compInstallTaskName: String,
                                          commonName: String): Task? {
                if (tasks.get(compInstallTaskName) == null) {
                    tasks.create(compInstallTaskName) {
                        it.group = INSTALLGROUPNAME
                        it.description = "Run installation of '${commonName}'"
                    }
                    val installTask = tasks.get(INSTALLTASKNAME)
                    installTask?.dependsOn(compInstallTaskName)
                }
                return tasks.get(compInstallTaskName)
            }

            private fun createContainerTasks(tasks: ModelMap<Task>,
                                             ext: InstallationExtension,
                                             descrMgr: DescriptorManager,
                                             adminDir: File,
                                             compDir: File,
                                             comp: ComponentDescr,
                                             taskPrefix: String,
                                             fileItemSet: Set<FileItem>,
                                             installTask: Task?) {

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

                        val taskName = "${taskPrefix}Pkg${pkg.name.capitalize()}${pkg.itemType.capitalize()}"

                        if (tasks.get(taskName) == null) {
                            tasks.create(taskName, PackageTask::class.java)
                        }
                        val pkgTask = tasks.get(taskName)
                        if (pkgTask is PackageTask) {
                            with(pkgTask) {
                                this.onlyIf { ! pkg.excludeFromUpdate }

                                this.fileContainer = pkgFile

                                this.installDir = getTargetDir(compDir, comp.containerTarget)
                                this.installPath = pkg.targetPath
                                this.targetIncluded = pkg.targetIncluded
                                this.excludesFromUpdate = pkg.excludesFromUpdate
                                this.fileItems = fileItemSet
                            }
                        } else {
                            throw GradleException("Task '${taskName} exists, but it has the wrong type!")
                        }

                        installTask?.dependsOn(taskName)
                    }
                }
            }

            private fun createModuleTasks(tasks: ModelMap<Task>,
                                          ext: InstallationExtension,
                                          comp: ComponentDescr,
                                          compDir: File,
                                          taskPrefix: String,
                                          fileItemSet: Set<FileItem>,
                                          installTask: Task?) {

                comp.modules.forEach { module ->

                    if (checkForType(ext.environment, module.value.types)) {

                        val taskName = "${taskPrefix}Module${module.value.name.capitalize()}"

                        if (tasks.get(taskName) == null) {
                            tasks.create(taskName, ModuleTask::class.java)
                        }
                        val moduleTask = tasks.get(taskName)
                        if (moduleTask is ModuleTask) {
                            with(moduleTask) {
                                this.onlyIf { ! module.value.excludeFromUpdate }

                                this.dependency = module.value.dependency.toString()
                                this.jarPath = module.value.jarPath
                                this.descriptorPath = module.value.descriptorPath
                                this.classifiers = module.value.classifiers
                                this.jars = module.value.jars
                                this.pkgs = module.value.pkgs
                                this.moduleName = module.value.name

                                this.installDir = getTargetDir(compDir, comp.modulesTarget)
                                this.installPath = module.key
                                this.targetIncluded = module.value.targetIncluded
                                this.excludesFromUpdate = module.value.excludesFromUpdate
                                this.fileItems = fileItemSet
                            }
                        } else {
                            throw GradleException("Task '${taskName} exists, but it has the wrong type!")
                        }

                        installTask?.dependsOn(taskName)
                    }
                }
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

                // update mode
                var update = false
                val instDescrFile = File(getTargetDir(compDir, mainDescr.descriptorPath), "component.component")

                if(instDescrFile.exists()) {
                    val prevID = ComponentUtil.metadataFromFile(targetFile).componentID

                    if(prevID.group != metadData.componentID.group || prevID.module != metadData.componentID.module) {
                        throw GradleException("The previous installed component was '$prevID'. $CHECKCONF $DOCUTEXT ")
                    }

                    update = true
                }

                // parameter is required - update or install
                val paramRequired = (compDir.exists() && ! update)

                // load files from desc
                val fileItemSet = loadFileItems(installExtension, descriptorMgr, compAdminDir, mainDescr.fileItems)

                //calculate task prefix
                val installPrefix = "${INSTALLTASKNAME}${compToInstall.commonName.capitalize()}"

                // create main install task of component
                val installTask = createInstallTask(tasks, installPrefix, compToInstall.commonName)

                // create package tasks
                createContainerTasks(tasks, installExtension, descriptorMgr, compAdminDir, compDir,
                        mainDescr, installPrefix, fileItemSet, installTask)

                // create module tasks
                createModuleTasks(tasks, installExtension, mainDescr, compDir, installPrefix, fileItemSet, installTask)

                if(mainDescr.libs.size > 0) {

                    val libsTaskname = "${installPrefix}Libs"
                    tasks.create(libsTaskname, LibsTask::class.java) {

                        mainDescr.libs.values.forEach {lib ->
                            if(checkForType(installExtension.environment, lib.types)) {
                                with(lib.dependency) {
                                    it.addLibData(LibData(this.group, this.module, this.version, lib.targetName))
                                }
                            }
                        }
                        it.installDir = compDir
                        it.installPath = mainDescr.libsTarget
                    }

                    installTask?.dependsOn(libsTaskname)
                }

                println(mainDescr.properties.size)
            }
        }
    }
}
