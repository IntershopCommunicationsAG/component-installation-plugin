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
import com.intershop.gradle.component.installation.utils.getValue
import com.intershop.gradle.component.installation.utils.setValue
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.impldep.org.bouncycastle.asn1.cms.CMSAttributes.contentType
import org.gradle.util.GFileUtils
import java.io.File

/**
 * This task creates an empty directory with an content type
 * file. As long the content file exists the task is upToDate.
 *
 * @constructor default constructor initialize a default task.
 */
open class DirectoryTask: DefaultTask() {

    private val contentTypeProperty: Property<ContentType> = project.objects.property(ContentType::class.java)
    private val directoryPathProperty: Property<String> = project.objects.property(String::class.java)

    /**
     * The content type will be stored in the specified component or module.
     * Possible values are
     *  - IMMUTABLE/STATIC
     *  - DATA
     *  - CONFIGURATION
     *  - UNSPECIFIED
     *
     * @property contentType information will be added to the files on the disk
     */
    @get:Input
    var contentType: ContentType
        get() = contentTypeProperty.getOrElse(ContentType.UNSPECIFIED)
        set(value) = contentTypeProperty.set(value)

    /**
     * This will set the content type property from a string value.
     *
     * @param type string representation of a content type.
     */
    fun setContentType(type: String) {
        contentTypeProperty.set(ContentType.valueOf(type))
    }

    /**
     * Directory path configuration of the component.
     *
     * @property directoryPath path of the directory.
     */
    @get:Input
    var directoryPath: String by directoryPathProperty

    /**
     * Set the provider for the directory path property.
     *
     * @param directoryPath provider for the directory path property.
     */
    fun provideDirectoryPath(directoryPath: Provider<String>) = directoryPathProperty.set(directoryPath)

    @get:OutputFile
    val directoryFile: File
        get() {
            val targetDir = File(directoryPath)
            return File(targetDir, ".install")
        }

    /**
     * The directory will be created automatically as base
     * directory of the output file. The content file is
     * the output file for this task.
     */
    @TaskAction
    fun createDirectory() {
        if(directoryFile.exists()) {
            GFileUtils.forceDelete(directoryFile)
        }

        GFileUtils.writeFile(contentType.toString(), directoryFile)
    }
}
