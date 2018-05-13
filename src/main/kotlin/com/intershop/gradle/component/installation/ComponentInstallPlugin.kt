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
import com.intershop.gradle.component.installation.extension.LocalFileItem
import com.intershop.gradle.component.installation.filter.PropertiesFilterReader
import com.intershop.gradle.component.installation.tasks.InstallConfigManager
import com.intershop.gradle.component.installation.tasks.InstallConfigManager.Companion.checkForOS
import com.intershop.gradle.component.installation.tasks.InstallMutableTask
import com.intershop.gradle.component.installation.tasks.InstallTask
import com.intershop.gradle.component.installation.utils.ContentType
import com.intershop.gradle.component.installation.utils.DescriptorManager
import com.intershop.gradle.component.installation.utils.OSType
import com.intershop.gradle.component.installation.utils.OSType.Companion.checkClassifierForOS
import com.intershop.gradle.component.installation.utils.data.Artifact
import com.intershop.gradle.component.installation.utils.data.FileItem
import com.intershop.gradle.component.installation.utils.data.PropertyConfiguration
import com.intershop.gradle.component.installation.utils.filter
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.FileTreeElement
import org.gradle.api.specs.Spec
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
         * Task name of the first task before all installation subtasks will
         * be startet.
         */
        const val PREINSTALLTASKNAME = "preInstall"

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

            val mainInstallTask = tasks.maybeCreate(INSTALLTASKNAME)
            mainInstallTask.group = INSTALLGROUPNAME
            mainInstallTask.description = "Run installation of '${project.name}'"

            val mainPreInstallTask = tasks.maybeCreate(PREINSTALLTASKNAME)
            mainPreInstallTask.group = INSTALLGROUPNAME
            mainPreInstallTask.description = "Run all tasks before the installation of '${project.name}' starts"

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
            private val LOGGER = LoggerFactory.getLogger(InstallRule::class.java)

            private const val CHECKCONF = "Check your configuration!"
            private const val DOCUTEXT = "Consult documentation for next steps."

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
                                      fileItems: Set<LocalFileItem>,
                                      adminDir: File) {

                confMgr.fileItemSet.clear()

                // add files form descriptor
                confMgr.descriptor.fileItems.forEach { file ->
                    val localFile = File(adminDir, "files/${file.name}.${file.extension}")

                    val classifier = file.classifier

                    if (checkForOS(file) && confMgr.checkForType(file)) {
                        val artifact = Artifact.getArtifact(file.name, file.extension, file.extension, classifier)

                        descrMgr.loadArtifactFile(artifact, localFile)

                        confMgr.fileItemSet.add(FileItem(localFile,
                                calculateFilePath(file.targetPath, file.name, file.extension),
                                file.targetPath, ContentType.valueOf(file.contentType.toString()), file.updatable))
                    }
                }

                // add local file items for installation
                fileItems.forEach { item ->
                    val classifier = item.classifier

                    if(checkClassifierForOS(classifier) && confMgr.checkForType(item.types)) {
                        confMgr.fileItemSet.add(FileItem(item.file,
                                calculateFilePath(item.targetPath, item.file.nameWithoutExtension, item.file.extension),
                                item.targetPath, ContentType.valueOf(item.contentType), item.updatable))
                    }
                }
            }

            private fun calculateFilePath(fileTarget: String, fileName: String, fileExtension: String): String {
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
                                   propertyMap: Map<String, PropertyConfiguration>,
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

                if(spec is InstallMutableTask) {
                    propertyMap.forEach {
                        spec.eachFile { fc ->
                            if (getSpecFrom(it.key).isSatisfiedBy(fc)) {
                                fc.filter<PropertiesFilterReader>("action" to it.value.getAction())
                            }
                        }
                    }
                }

                spec.duplicatesStrategy = DuplicatesStrategy.FAIL
            }

            private fun configExcludesPreserve(spec: InstallTask,
                                               descr: ComponentDescr,
                                               comp: Component,
                                               item: ContainerItem,
                                               update: Boolean) {
                val preservePatternSet = PatternSet()

                spec.exclude(comp.excludes)

                if(update) {
                    spec.exclude(descr.excludes)
                    spec.exclude(item.excludes)
                }

                preservePatternSet.exclude(comp.preserve.excludes)
                preservePatternSet.include(comp.preserve.includes)

                if(update) {
                    preservePatternSet.exclude(descr.preserveExcludes)
                    preservePatternSet.include(descr.preserveIncludes)
                    preservePatternSet.exclude(item.preserveExcludes)
                    preservePatternSet.include(item.preserveIncludes)
                }

                spec.preserve.setIncludes(preservePatternSet.includes)
                spec.preserve.setExcludes(preservePatternSet.excludes)


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

            private fun getSpecFrom (include: String): Spec<FileTreeElement> {
                val patternSet = PatternSet()
                patternSet.include(include)
                return patternSet.asSpec
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
                throw GradleException("The operating system is not supported by the component install plugin!")
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
                val metaData = descriptorMgr.getDescriptorMetadata(targetFile)

                // read configuration
                val mainDescr = ComponentUtil.componentFromFile(targetFile)

                val compDir = getComponentDir(installExtension.installDir, compToInstall, mainDescr.target)

                // update mode
                val instDescrFile = File(File(compDir, mainDescr.descriptorPath), "component.component")
                var update = false

                if(instDescrFile.exists()) {
                    val prevID = ComponentUtil.metadataFromFile(targetFile).componentID

                    if(prevID.group != metaData.componentID.group || prevID.module != metaData.componentID.module) {
                        throw GradleException("The previous installed component was '$prevID'. $CHECKCONF $DOCUTEXT ")
                    }

                    update = true
                }

                val confMgr = InstallConfigManager(installExtension, tasks,
                        compToInstall.commonName, mainDescr, compDir, update)

                // copy descriptor file for documentation
                val descrTaskname = INSTALLTASKNAME.plus(confMgr.getSuffixStr("componentDescriptor"))

                val descrInstall = confMgr.getInstallTask(descrTaskname, ContentType.IMMUTABLE)

                with(descrInstall) {
                    from(project.file(targetFile))
                    destinationDir = confMgr.getTargetDir(mainDescr.descriptorPath)

                    dependsOn(confMgr.preCompInstallTaskName)
                }
                confMgr.compInstallTask?.dependsOn(descrTaskname)


                val propertyMap = mutableMapOf<String, PropertyConfiguration>()

                mainDescr.properties.forEach { prop ->
                    if(checkForOS(prop) && confMgr.checkForType(prop) && (prop.updatable || ! update) ) {
                        val mapEntry = propertyMap[prop.pattern]
                        if(mapEntry != null) {
                            mapEntry.properties[prop.key] = prop.value
                        } else {
                            val pconf = PropertyConfiguration()
                            pconf.properties[prop.key] = prop.value
                            propertyMap[prop.pattern] = pconf
                        }
                    }
                }

                // load files from desc
                initFileItems(confMgr, descriptorMgr, compToInstall.fileItems.localFileItems , compAdminDir)

                // install file containers
                mainDescr.fileContainers.forEach { pkg ->
                    val pkgFile = File(compAdminDir, "pkgs/${pkg.name}-${pkg.itemType}.zip")

                    if (checkForOS(pkg) && confMgr.checkForType(pkg) && (pkg.updatable || ! update) ) {
                        val artifact = Artifact.getArtifact(pkg.name, pkg.itemType, "zip", pkg.classifier)
                        descriptorMgr.loadArtifactFile(artifact, pkgFile)

                        val taskName = INSTALLTASKNAME.plus(confMgr.getSuffixStr("pkg", pkg.name, pkg.itemType))
                        val pkgTask = confMgr.getInstallTask(taskName, ContentType.valueOf(pkg.contentType.toString()))

                        with(pkgTask) {
                            from(project.zipTree(pkgFile))

                            configSpec(this, confMgr, propertyMap, pkg.targetIncluded, update, pkg.targetPath)
                            configExcludesPreserve(this, mainDescr, compToInstall, pkg, update)

                            destinationDir = confMgr.getTargetDir(mainDescr.containerPath, pkg.targetPath)

                            dependsOn(confMgr.preCompInstallTaskName)
                        }

                        confMgr.compInstallTask?.dependsOn(taskName)

                    }
                }

                // install modules
                mainDescr.modules.forEach { entry ->

                    if(confMgr.checkForType(entry.value) && (entry.value.updatable || ! update)) {
                        val taskName = INSTALLTASKNAME.plus(confMgr.getSuffixStr("module", entry.value.name))

                        val install = confMgr.getInstallTask(taskName,
                                ContentType.valueOf(entry.value.contentType.toString()))

                        with(install) {
                            confMgr.configureModuleSpec(this, entry.value)

                            configSpec(this, confMgr, propertyMap, entry.value.targetIncluded, update, entry.key)
                            configExcludesPreserve(this, mainDescr, compToInstall, entry.value, update)

                            destinationDir = confMgr.getTargetDir(mainDescr.modulesPath, entry.key)

                            dependsOn(confMgr.preCompInstallTaskName)
                        }

                        confMgr.compInstallTask?.dependsOn(taskName)
                    }
                }

                // install libs
                if(mainDescr.libs.isNotEmpty()) {
                    val libTaskName = INSTALLTASKNAME.plus(confMgr.getSuffixStr("libs"))

                    val libInstall = confMgr.getInstallTask(libTaskName, ContentType.IMMUTABLE)

                    libInstall.destinationDir = confMgr.getTargetDir(mainDescr.libsPath)
                    libInstall.duplicatesStrategy = DuplicatesStrategy.FAIL

                    confMgr.configureLibsSpec(libInstall, mainDescr.libs)

                    libInstall.dependsOn(confMgr.preCompInstallTaskName)

                    confMgr.compInstallTask?.dependsOn(libTaskName)
                }

                if(mainDescr.directoryItems.isNotEmpty()) {
                    mainDescr.directoryItems.forEach {
                        if(confMgr.checkForType(it) && (it.updatable || ! update)) {
                            with(confMgr.getDirectoryTask(it.targetPath)) {
                                directoryPath = confMgr.getTargetDir(it.targetPath).absolutePath
                                contentType = ContentType.valueOf(it.contentType.toString())

                                dependsOn(confMgr.preCompInstallTaskName)
                                confMgr.compInstallTask?.dependsOn(name)
                            }
                        }
                    }
                }

                if(mainDescr.linkItems.isNotEmpty()) {
                    with(confMgr.getLinkTask()) {
                        mainDescr.linkItems.forEach { link ->
                            if(OSType.checkClassifierForOS(link.classifiers) &&
                                    confMgr.checkForType(link) && (link.updatable || ! update)) {

                                addLink(confMgr.getTargetDir(link.name).absolutePath,
                                        confMgr.getTargetDir(link.targetPath).absolutePath)
                            }
                        }

                        val compInstallTask = confMgr.compInstallTask
                        if(compInstallTask != null) {
                            mustRunAfter(compInstallTask.path)
                        }

                        val installTask = tasks.get(INSTALLTASKNAME)
                        installTask?.dependsOn(this.name)
                    }
                }

                confMgr.initCleanupTask(backupDir)
            }



        }
    }
}
