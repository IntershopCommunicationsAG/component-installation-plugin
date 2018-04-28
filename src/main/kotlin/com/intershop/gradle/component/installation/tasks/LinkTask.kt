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

import com.intershop.gradle.component.installation.utils.getValue
import com.intershop.gradle.component.installation.utils.setValue
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import java.io.File
import java.nio.file.Files
import java.nio.file.LinkOption

/**
 * This task creates a list of symbolic links for a component.
 *
 * @constructor default constructor initialize a default task.
 */
open class LinkTask: DefaultTask() {

    private val linkDataSetProperty: SetProperty<LinkData> = project.objects.setProperty(LinkData::class.java)

    /**
     * Set of of all link data of the component.
     *
     * @property linkdata link data from the descriptor.
     */
    @get:Internal
    var linkdata: Set<LinkData> by linkDataSetProperty

    /**
     * Set the provider for the link data set property.
     *
     * @param linkdata provider for the link data set property.
     */
    fun provideModulePaths(linkdata: Provider<Set<LinkData>>) = linkDataSetProperty.set(linkdata)

    /**
     * Add link data to this task with name and target.
     *
     * @param namePath name of the symbolic link
     * @param targetPath target of the symbolic link
     */
    fun addLink(namePath: String, targetPath: String) {
        linkDataSetProperty.add(LinkData(namePath, targetPath))
    }

    /**
     * Task action creates links.
     */
    @TaskAction
    fun createLink() {
        linkdata.forEach { linkData ->
            createLink(linkData.namePath, linkData.targetPath)
        }
    }

    private fun createLink(namePath: String, targetPath: String) {
        val fileFilePath = File(namePath).toPath()
        val targetFilePath = File(targetPath).toPath()

        if(Files.exists(fileFilePath, LinkOption.NOFOLLOW_LINKS) && Files.isSymbolicLink(fileFilePath)) {
            if(Files.readSymbolicLink(fileFilePath) != targetFilePath) {
                Files.deleteIfExists(fileFilePath)
            }
        } else {
            GradleException("The file '$fileFilePath' exists and is not a symbolic link!" )
        }

        if(! Files.exists(targetFilePath)) {
            project.logger.warn("The target '{}' of link '{}' does not exists!", targetPath, namePath)
        } else if(! Files.exists(fileFilePath, LinkOption.NOFOLLOW_LINKS)) {
            Files.createSymbolicLink(fileFilePath, targetFilePath)
        }
    }

    /**
     * This class describes a link with name and target.
     *
     * @property namePath name of the symbolic link
     * @property targetPath target of the symbolic link
     *
     * @constructor initialize a link configuration with name and target
     */
    data class LinkData( val namePath: String, val targetPath: String)
}
