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

    @get:Internal
    var backupDir: File
        get() = backupDirProperty.get().asFile
        set(value) = backupDirProperty.set(value)

    fun provideBackupDir(backupDir: Provider<Directory>) = backupDirProperty.set(backupDir)

    @get:Internal
    var installDir: File
        get() = installDirProperty.get().asFile
        set(value) = installDirProperty.set(value)

    fun provideInstallDir(installDir: Provider<Directory>) = installDirProperty.set(installDir)

    @get:Internal
    var descriptorPath: String by descriptorPathProperty

    fun provideDescriptorPath(descriptorPath: Provider<String>) = descriptorPathProperty.set(descriptorPath)

    @get:Internal
    var modulesPath: String by modulesPathProperty

    fun provideModulesPath(modulesPath: Provider<String>) = modulesPathProperty.set(modulesPath)

    @get:Internal
    var containersPath: String by containersPathProperty

    fun provideContainersPath(containersPath: Provider<String>) = containersPathProperty.set(containersPath)

    @get:Internal
    var libsPath: String by libsPathProperty

    fun provideLibsPath(libsPath: Provider<String>) = libsPathProperty.set(libsPath)

    @get:Internal
    var modulePaths: Set<String> by modulePathsSetProperty

    fun provideModulePaths(modulePaths: Provider<Set<String>>) = modulePathsSetProperty.set(modulePaths)

    @get:Internal
    var containerPaths: Set<String> by containerPathsSetProperty

    fun provideContainerPaths(containerPaths: Provider<Set<String>>) = containerPathsSetProperty.set(containerPaths)

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
                Files.walk(Paths.get(it.absolutePath)).filter({ it.toFile().isFile && it.toFile().name == ".install" }).forEach {
                    val content = it.toFile().readText()
                    if (content.contains(ContentType.DATA.toString()) || content.contains(ContentType.UNSPECIFIED.toString())) {
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