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

import com.intershop.gradle.component.descriptor.items.ContainerItem
import com.intershop.gradle.component.descriptor.util.ComponentUtil
import com.intershop.gradle.component.installation.extension.Component
import com.intershop.gradle.component.installation.extension.InstallationExtension
import com.intershop.gradle.component.installation.extension.OSType
import com.intershop.gradle.component.installation.tasks.InstallConfigManager
import com.intershop.gradle.component.installation.tasks.InstallConfigManager.Companion.checkForOS
import com.intershop.gradle.component.installation.tasks.InstallTask
import com.intershop.gradle.component.installation.utils.ContentType
import com.intershop.gradle.component.installation.utils.DescriptorManager
import com.intershop.gradle.component.installation.utils.data.Artifact
import com.intershop.gradle.component.installation.utils.data.FileItem
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.util.PatternSet
import org.gradle.model.Defaults
import org.gradle.model.ModelMap
import org.gradle.model.RuleSource
import org.gradle.model.internal.core.ModelPath
import org.gradle.model.internal.core.ModelReference
import org.gradle.model.internal.core.ModelRegistrations
import org.gradle.model.internal.registry.ModelRegistry
import org.slf4j.LoggerFactory
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import com.intershop.gradle.component.descriptor.Component as ComponentDescr
import com.intershop.gradle.component.descriptor.FileItem as FileItemDescr
import com.intershop.gradle.component.descriptor.Module as ModuleDescr

/**
 * This is the main class of the plugin.
 *
 * @property modelRegistry the Gradle model registry of this project.
 *
 * @constructor initialize the plugin with the current model registry.
 */
@Suppress("unused")
class ComponentInstallPlugin @Inject constructor(private val modelRegistry: ModelRegistry?) : Plugin<Project> {

    companion object {
        /**
         * The task name of the main task. This is also the prefix for
         * all install tasks.
         */
        const val INSTALLTASKNAME = "install"
        /**
         * The task group name of all install tasks.
         */
        const val INSTALLGROUPNAME = "Component Installation"
    }

    /**
     * Apply this plugin to the given target object.
     *
     * @param project The target project object.
     */
    override fun apply(project: Project) {
        with(project) {
            logger.info("Install plugin adds extension {} to {}",
                    InstallationExtension.INSTALLATION_EXTENSION_NAME, name)

            val extension = extensions.findByType(InstallationExtension::class.java)
                    ?: extensions.create(InstallationExtension.INSTALLATION_EXTENSION_NAME,
                            InstallationExtension::class.java, project)

            tasks.maybeCreate(INSTALLTASKNAME).group = INSTALLGROUPNAME

            if(modelRegistry?.state(ModelPath.nonNullValidatedPath("installExtension")) == null) {
                modelRegistry?.register(ModelRegistrations.bridgedInstance(
                        ModelReference.of("installExtension", InstallationExtension::class.java), extension)
                        .descriptor("component install configuration").build())
            }
        }
    }

    /**
     * Internal class to configure tasks from the extension
     * in the project model space.
     *
     * A marker type for a class that is a collection of rules. A rule source is not used like a regular
     * Java object. It is a stateless container of methods and possibly constants.
     *
     * Please consult the “Rule based model configuration” chapter of the Gradle User Guide for
     * general information about “rules”.
     */
    @Suppress("unused")
    class InstallRule : RuleSource() {

        companion object {
            val LOGGER = LoggerFactory.getLogger(InstallRule::class.java)

            const val CHECKCONF = "Check your configuration!"
            const val DOCUTEXT = "Consult documentation for next steps."

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

            private fun initFileItems(confMgr: InstallConfigManager,
                                     descrMgr: DescriptorManager,
                                     adminDir: File) {

                confMgr.fileItemSet.clear()

                confMgr.descriptor.fileItems.forEach { file ->
                    val localFile = File(adminDir, "files/${file.name}.${file.extension}")
                    val classifier = file.classifier

                    if (checkForOS(file) && confMgr.checkForType(file)) {
                        val artifact = Artifact.getArtifact(file.name, file.extension, file.extension, classifier)

                        descrMgr.loadArtifactFile(artifact, localFile)

                        confMgr.fileItemSet.add(FileItem(localFile,
                                calculatFilePath(file.targetPath, file.name, file.extension),
                                file.targetPath, ContentType.valueOf(file.contentType.toString()), file.updatable))
                    }
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

            private fun configSpec(spec: InstallTask,
                                   confMgr: InstallConfigManager,
                                   targetIncluded: Boolean,
                                   update: Boolean,
                                   target: String) {

                if(confMgr.fileItemSet.isNotEmpty()) {
                    confMgr.fileItemSet.forEach { item ->
                        if(item.filePath.startsWith(target) &&
                                ((update && item.updatable && item.contentType != ContentType.DATA) || ! update)) {
                            spec.exclude(item.filePath)
                            spec.from(item.file.parent) {
                                it.include(item.file.name)
                                it.into(item.targetPath)
                            }
                        }
                    }
                }

                if(targetIncluded) {
                    spec.eachFile { details ->
                        details.path = removeDirFromPath(target, details.path)
                    }
                }

                spec.duplicatesStrategy = DuplicatesStrategy.FAIL
            }

            private fun configExcludesPreserve(spec: InstallTask,
                                               comp: Component,
                                               item: ContainerItem) {
                val preservePatternSet = PatternSet()

                preservePatternSet.include(item.preserveIncludes)
                preservePatternSet.include(comp.preserve.excludes)
                preservePatternSet.exclude(item.preserveExcludes)
                preservePatternSet.exclude(comp.preserve.includes)

                spec.preserve.setIncludes(preservePatternSet.includes)
                spec.preserve.setExcludes(preservePatternSet.excludes)

                spec.exclude(item.excludes)
                spec.exclude(comp.excludes)
            }

            private fun removeDirFromPath(dirName: String,
                                          path: String): String {
                return if(path.startsWith(dirName) && dirName.isNotBlank()) {
                    val newPath = path.replaceFirst(dirName, "")
                    if(newPath.startsWith("/")) {
                        newPath.substring(1)
                    } else {
                        newPath
                    }
                } else {
                    path
                }
            }

        }

        /**
         * This rule downloads the components and configures all installation tasks.
         *
         * @param tasks             Model backed map of tasks.
         * @param installExtension  The installation extensions.
         */
        @Defaults
        fun installTasksRule(tasks: ModelMap<Task>, installExtension: InstallationExtension) {

            if(OSType.detectedOS() == OSType.OTHER) {
                throw GradleException("The operating system is not suppported by the component install plugin!")
            }

            val repoHandler = installExtension.project.repositories
            val ivyPatterns = installExtension.installConfig.ivyPatterns
            val adminDir = installExtension.installConfig.installAdminDir

            installExtension.components.forEach { compToInstall ->

                // get URL for dependency
                val descriptorMgr = DescriptorManager(repoHandler, compToInstall.dependency, ivyPatterns)

                // load component configuration
                val compAdminDir = File(adminDir, compToInstall.commonName)

                val installTimeFormat = SimpleDateFormat("yyyyMMddHHmmssSSS")
                val backupDir = File(compAdminDir, "backup/${installTimeFormat.format(Date())}")

                val targetFile = File(compAdminDir, "components/component.component")
                descriptorMgr.loadDescriptorFile(targetFile)

                // get and verify metadata
                val metadData = descriptorMgr.getDescriptorMetadata(targetFile)

                // read configuration
                val mainDescr = ComponentUtil.componentFromFile(targetFile)

                val compDir = getComponentDir(installExtension.installDir, compToInstall, mainDescr.target)

                // update mode
                val instDescrFile = File(File(compDir, mainDescr.descriptorPath), "component.component")
                var update = false

                if(instDescrFile.exists()) {
                    val prevID = ComponentUtil.metadataFromFile(targetFile).componentID

                    if(prevID.group != metadData.componentID.group || prevID.module != metadData.componentID.module) {
                        throw GradleException("The previous installed component was '$prevID'. $CHECKCONF $DOCUTEXT ")
                    }

                    update = true
                }

                val confMgr = InstallConfigManager(installExtension, tasks,
                        compToInstall.commonName, mainDescr, compDir, update)

                // copy descriptor file for documentation
                val descrTaskname = INSTALLTASKNAME.plus(confMgr.getSuffixStr("componentDescriptor"))
                val descrInstall = confMgr.getInstallTask(descrTaskname)

                with(descrInstall) {
                    from(project.file(targetFile))
                    contentType = ContentType.IMMUTABLE
                    destinationDir = confMgr.getTargetDir(mainDescr.descriptorPath)
                }
                confMgr.compInstallTask?.dependsOn(descrTaskname)

                // load files from desc
                initFileItems(confMgr, descriptorMgr, compAdminDir)

                // install file containers
                mainDescr.fileContainers.forEach { pkg ->
                    val pkgFile = File(compAdminDir, "pkgs/${pkg.name}.zip")

                    if (checkForOS(pkg) && confMgr.checkForType(pkg) && pkg.updatable ) {
                        val artifact = Artifact.getArtifact(pkg.name, pkg.itemType, "zip", pkg.classifier)
                        descriptorMgr.loadArtifactFile(artifact, pkgFile)

                        val taskName = INSTALLTASKNAME.plus(confMgr.getSuffixStr("pkg", pkg.name, pkg.itemType))
                        val pkgTask = confMgr.getInstallTask(taskName)

                        with(pkgTask) {
                            from(project.zipTree(pkgFile))

                            configSpec(this, confMgr, pkg.targetIncluded, update, pkg.targetPath)
                            configExcludesPreserve(this, compToInstall, pkg)

                            pkgTask.contentType = ContentType.valueOf(pkg.contentType.toString())
                            destinationDir = confMgr.getTargetDir(mainDescr.containerTarget, pkg.targetPath)
                        }

                        confMgr.compInstallTask?.dependsOn(taskName)

                    }
                }

                // install modules
                mainDescr.modules.forEach { entry ->
                    val taskName = INSTALLTASKNAME.plus(confMgr.getSuffixStr("module", entry.value.name))

                    val install = confMgr.getInstallTask(taskName)
                    with(install) {
                        destinationDir = confMgr.getTargetDir(mainDescr.modulesTarget, entry.key)

                        confMgr.configureModuleSpec(this, entry.value)

                        configSpec(this, confMgr, entry.value.targetIncluded, update, entry.key)
                        configExcludesPreserve(this, compToInstall, entry.value)

                        install.contentType = ContentType.valueOf(entry.value.contentType.toString())

                    }

                    confMgr.compInstallTask?.dependsOn(taskName)
                }

                // install libs
                if(mainDescr.libs.isNotEmpty()) {
                    val libTaskName = INSTALLTASKNAME.plus(confMgr.getSuffixStr("libs"))

                    val libInstall = confMgr.getInstallTask(libTaskName)

                    libInstall.destinationDir = confMgr.getTargetDir(mainDescr.libsTarget)
                    libInstall.duplicatesStrategy = DuplicatesStrategy.FAIL

                    confMgr.configureLibsSpec(libInstall, mainDescr.libs)
                    libInstall.contentType = ContentType.IMMUTABLE

                    confMgr.compInstallTask?.dependsOn(libTaskName)
                }

                confMgr.initCleanupTask(backupDir)

                // configuration is currently not used
                //println(mainDescr.properties.size)
            }



        }
    }
}
