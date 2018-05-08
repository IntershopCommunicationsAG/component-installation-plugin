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
import org.gradle.api.InvalidUserDataException
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.FileCopyAction
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import org.gradle.internal.impldep.org.bouncycastle.asn1.cms.CMSAttributes.contentType
import org.gradle.util.GFileUtils
import java.io.File

/**
 * This class is the main installation task of this plugin.
 * It extends the standard Gradle Sync task, so that the last modified
 * time of input files will be preserved.
 */
open class InstallTask : Sync() {

    companion object {
        private const val ERRORMSG = "No copy destination directory has been specified, " +
                "use 'into' to specify a target directory."
    }

    private val contentTypeProperty: Property<ContentType> = project.objects.property(ContentType::class.java)

    /**
     * See org.gradle.api.tasks.AbstractCopyTask for more information.
     *
     * @return a copy action to preserve the last modified time and existing target files.
     */
    override fun createCopyAction(): CopyAction {
        val destinationDir = destinationDir ?: throw InvalidUserDataException(ERRORMSG)

        return SyncCopyActionDecorator(destinationDir, FileCopyAction(fileLookup.getFileResolver(destinationDir)),
                super.getPreserve(), directoryFileTreeFactory, System.currentTimeMillis())
    }

    /**
     * The contentype will be stored in the specified component or module.
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
     * @param type string representation of a contenty type.
     */
    fun setContentType(type: String) {
        contentTypeProperty.set(ContentType.valueOf(type))
    }

    /**
     * The task action of the task. It calls the copy
     * function of the Copy task and adds the content type
     * information to the target dir.
     */
    @TaskAction
    override fun copy() {
        super.copy()

        val typeFile = File(destinationDir, ".install")
        if(typeFile.exists()) {
            GFileUtils.forceDelete(typeFile)
        }

        GFileUtils.writeFile(contentType.toString(), typeFile)
    }
}
