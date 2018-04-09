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
import org.gradle.api.GradleException
import org.gradle.api.file.CopySpec
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.TaskAction
import java.io.File

open class PackageTask: AInstallTask() {

    private val fileContainerProperty: RegularFileProperty = project.layout.fileProperty()

    @get:InputFile
    var fileContainer: File
        get() = fileContainerProperty.get().asFile
        set(value) = fileContainerProperty.set(value)

    fun provideFileContainer(fileContainer: Provider<RegularFile>) = fileContainerProperty.set(fileContainer)

    var runUpdate: Boolean = false

    @TaskAction
    fun runInstall() {
        if(! outputDir.exists()) {
            throw GradleException("The target directory '${outputDir}' does not exists!")
        }
        if(! runUpdate) {
            project.sync { configureCopySpec(it) }
        } else {
            project.copy { configureCopySpec(it) }
        }
    }

    protected fun configureCopySpec(spec: CopySpec, update: Boolean = false) {
        spec.from(project.zipTree(fileContainer))
        spec.into(outputDir)

        if(update && ! excludesFromUpdate.isEmpty()) {
            excludesFromUpdate.forEach {
                spec.exclude(it)
            }
        }

        if(! fileItems.isEmpty()) {
            fileItems.forEach { item ->
                if (item.filePath.startsWith(installPath) &&
                        ((update && !item.excludeFromUpdate && item.contentType != ContentType.DATA) || !update)) {
                    spec.exclude(item.filePath)
                    spec.from(item.file) {
                        it.into(item.filePath)
                    }
                }
            }
        }

        if(targetIncluded) {
            spec.eachFile { details ->
                details.path = removeDirFromPath(installPath, details.path)
            }
        }

        spec.duplicatesStrategy = DuplicatesStrategy.FAIL
    }

    private fun removeDirFromPath(dirName: String,  path: String): String {
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
