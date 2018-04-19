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
import org.gradle.util.GFileUtils
import java.io.File

open class InstallTask : Sync() {

    companion object {
        const val ERRORMSG = "No copy destination directory has been specified, " +
                "use 'into' to specify a target directory."
    }

    private val contentTypeProperty: Property<ContentType> = project.objects.property(ContentType::class.java)

    override fun createCopyAction(): CopyAction {
        val destinationDir = destinationDir ?: throw InvalidUserDataException(ERRORMSG)

        return SyncCopyActionDecorator(destinationDir, FileCopyAction(fileLookup.getFileResolver(destinationDir)),
                super.getPreserve(), directoryFileTreeFactory, System.currentTimeMillis())
    }

    @get:Input
    var contentType: ContentType
        get() = contentTypeProperty.getOrElse(ContentType.UNSPECIFIED)
        set(value) = contentTypeProperty.set(value)

    fun setContentType(type: String) {
        contentTypeProperty.set(ContentType.valueOf(type))
    }

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
