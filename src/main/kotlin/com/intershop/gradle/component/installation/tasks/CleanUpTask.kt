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

import com.intershop.gradle.component.installation.utils.ContentType
import com.intershop.gradle.component.installation.utils.TreeNode
import com.intershop.gradle.component.installation.utils.getValue
import com.intershop.gradle.component.installation.utils.setValue
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.util.GFileUtils
import java.io.File
import java.nio.file.Files
import java.nio.file.Paths

/**
 * This task will remove all not configured files and directories from the
 * installation. The information is taken from the specified component
 * descriptor.
 * This task does not work incrementally.
 */
open class CleanUpTask: DefaultTask() {

    // backup directory
    private val backupDirProperty = project.layout.directoryProperty()

    // installation directory
    private val installDirProperty = project.layout.directoryProperty()

    // main target directories
    private val descriptorPathProperty = project.objects.property(String::class.java)
    private val modulesPathProperty = project.objects.property(String::class.java)
    private val containersPathProperty = project.objects.property(String::class.java)
    private val libsPathProperty = project.objects.property(String::class.java)

    // module directories
    private val modulePathsSetProperty = project.objects.setProperty(String::class.java)

    // package directories
    private val containerPathsSetProperty = project.objects.setProperty(String::class.java)

    /**
     * Backup directory of this task. It is used for special or unknown directories and files.
     *
     * @property backupDir the file object of the backup directory.
     */
    @get:Internal
    var backupDir: File
        get() = backupDirProperty.get().asFile
        set(value) = backupDirProperty.set(value)

    /**
     * Set the provider for the backup directory property.
     *
     * @param backupDir directory provider of the property.
     */
    fun provideBackupDir(backupDir: Provider<Directory>) = backupDirProperty.set(backupDir)

    /**
     * The installation directory of the component. All items of the component
     * will be installed to this directory.
     *
     * @property installDir the file object of the installation directory.
     */
    @get:Internal
    var installDir: File
        get() = installDirProperty.get().asFile
        set(value) = installDirProperty.set(value)

    /**
     * Set the provider for the installation directory property.
     *
     * @param installDir directory provider of the property.
     */
    fun provideInstallDir(installDir: Provider<Directory>) = installDirProperty.set(installDir)

    /**
     * Descriptor path configuration of the component.
     *
     * @property descriptorPath path for the component descriptor.
     */
    @get:Internal
    var descriptorPath: String by descriptorPathProperty

    /**
     * Set the provider for the descriptor path property.
     *
     * @param descriptorPath provider for the descriptor path property.
     */
    fun provideDescriptorPath(descriptorPath: Provider<String>) = descriptorPathProperty.set(descriptorPath)

    /**
     * Path of all modules of the component.
     *
     * @property modulesPath path for all modules from the descriptor.
     */
    @get:Internal
    var modulesPath: String by modulesPathProperty

    /**
     * Set the provider for the modules path property.
     *
     * @param modulesPath provider for the modules path property.
     */
    fun provideModulesPath(modulesPath: Provider<String>) = modulesPathProperty.set(modulesPath)

    /**
     * Path of all containers of the component.
     *
     * @property containersPath path for all containers from the descriptor.
     */
    @get:Internal
    var containersPath: String by containersPathProperty

    /**
     * Set the provider for the containers path property.
     *
     * @param containersPath provider for the containers path property.
     */
    fun provideContainersPath(containersPath: Provider<String>) = containersPathProperty.set(containersPath)

    /**
     * Path of all libraries of the component.
     *
     * @property libsPath path for all libraries from the descriptor.
     */
    @get:Internal
    var libsPath: String by libsPathProperty

    /**
     * Set the provider for the libraries path property.
     *
     * @param libsPath provider for the libraries path property.
     */
    fun provideLibsPath(libsPath: Provider<String>) = libsPathProperty.set(libsPath)

    /**
     * Set of of all module paths of the component.
     *
     * @property modulePaths path set of all modules from the descriptor.
     */
    @get:Internal
    var modulePaths: Set<String> by modulePathsSetProperty

    /**
     * Set the provider for the module path set property.
     *
     * @param modulePaths provider for the module path set property.
     */
    fun provideModulePaths(modulePaths: Provider<Set<String>>) = modulePathsSetProperty.set(modulePaths)

    /**
     * Set of of all module containers of the component.
     *
     * @property containerPaths path set of all containers from the descriptor.
     */
    @get:Internal
    var containerPaths: Set<String> by containerPathsSetProperty

    /**
     * Set the provider for the container path set property.
     *
     * @param containerPaths provider for the container path set property.
     */
    fun provideContainerPaths(containerPaths: Provider<Set<String>>) = containerPathsSetProperty.set(containerPaths)

    /**
     * The task action of the task. Delete not configured directories or move these files or
     * directories to the backup directory.
     */
    @TaskAction
    fun cleanup() {
        // create configuration target
        val root = TreeNode(installDir.name)
        root.addPath(descriptorPath).target = true
        root.addPath(libsPath).target = true

        val moduleNode = root.addPath(modulesPath)

        modulePaths.forEach {
            moduleNode.addPath(it).target = true
        }

        val containerNode = root.addPath(containersPath)

        containerPaths.forEach {
            containerNode.addPath(it).target = true
        }

        doCheck(root, installDir)
    }

    private fun doCheck(node: TreeNode, dir: File) {
        node.distinctFiles(dir).forEach {
            var backup = false
            if(it.isDirectory) {
                Files.walk(Paths.get(it.absolutePath)).
                        filter({ it.toFile().isFile && it.toFile().name == ".install" }).
                        forEach {
                    val content = it.toFile().readText()
                    if (content.contains(ContentType.DATA.toString()) ||
                            content.contains(ContentType.UNSPECIFIED.toString())) {
                        backup = true
                    }
                }
                if (backup) {
                    logger.debug("Directory '{}' will be moved to backup '{}'", it.absolutePath, backupDir.absolutePath)
                    GFileUtils.moveDirectory(it, File(backupDir, it.name))
                    logger.info("Directory '{}' moved to backup '{}'", it.absolutePath, backupDir.absolutePath)
                } else {
                    logger.debug("Directory '{}' will be deleted.", it.absolutePath)
                    GFileUtils.deleteDirectory(it)
                    logger.info("Directory '{}' deleted.", it.absolutePath)
                }
            } else {
                logger.debug("File '{}' will be moved to backup '{}'", it.absolutePath, backupDir.absolutePath)
                GFileUtils.moveFile(it, backupDir)
                logger.info("File '{}' moved to backup '{}'", it.absolutePath, backupDir.absolutePath)
            }
        }

        node.intersectFiles(dir).forEach {
            if(! node.isTarget(it)) {
                val n = node.getChild(it.name)
                if(n != null) {
                    doCheck(n, it)
                }
            }
        }
    }
}
