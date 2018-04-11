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
import com.intershop.gradle.component.installation.utils.data.FileItem
import com.intershop.gradle.component.installation.utils.getValue
import com.intershop.gradle.component.installation.utils.property
import com.intershop.gradle.component.installation.utils.setValue
import org.gradle.api.DefaultTask
import org.gradle.api.file.CopySpec
import org.gradle.api.file.Directory
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.OutputDirectory
import java.io.File

open class AInstallTask: DefaultTask() {

    private val installDirProperty: DirectoryProperty = project.layout.directoryProperty()
    private val installPathProperty: Property<String> = project.objects.property(String::class.java)

    private val targetIncludedProperty = project.objects.property<Boolean>()
    private val excludesFromUpdateProperty: SetProperty<String> = project.objects.setProperty(String::class.java)
    private val contentTypeProperty: Property<ContentType> = project.objects.property(ContentType::class.java)

    private val fileItemsProperty: SetProperty<FileItem> = project.objects.setProperty(FileItem::class.java)

    @get:Internal
    var installDir: File
        get() = installDirProperty.get().asFile
        set(value) = installDirProperty.set(value)

    fun provideInstallDir(installDir: Provider<Directory>) = installDirProperty.set(installDir)

    @get:Internal
    var installPath: String by installPathProperty

    fun provideInstallPath(installPath: Provider<String>) = installPathProperty.set(installPath)

    @get:Input
    var targetIncluded: Boolean
        get() = targetIncludedProperty.getOrElse(false)
        set(value) = targetIncludedProperty.set(value)

    fun targetIncluded(targetIncluded: Provider<Boolean>) = targetIncludedProperty.set(targetIncluded)

    @get:Input
    var excludesFromUpdate: Set<String> by excludesFromUpdateProperty

    fun excludesFromUpdate(excludesFromUpdate: Provider<Set<String>>) =
            excludesFromUpdateProperty.set(excludesFromUpdate)

    @get:Input
    var contentType: ContentType
        get() = contentTypeProperty.getOrElse(ContentType.UNSPECIFIED)
        set(value) = contentTypeProperty.set(value)

    fun setContentType(type: String) {
        contentTypeProperty.set(ContentType.valueOf(type))
    }

    @get:Nested
    var fileItems: Set<FileItem>
        get() = fileItemsProperty.get()
        set(value) = fileItemsProperty.set(value)

    fun addFileItemConf(conf: FileItem) {
        fileItemsProperty.add(conf)
    }

    fun provideFileItemsConfs(fileItemConfs: Provider<Set<FileItem>>) = fileItemsProperty.set(fileItemConfs)

    @get:OutputDirectory
    val outputDir: File
        get() = when {  installPath.isNotBlank() -> File(installDir, installPath)
                        else -> installDir }

    @get:Internal
    var runUpdate: Boolean = false

    protected fun removeDirFromPath(dirName: String,  path: String): String {
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

    protected fun finalizeSpec(spec: CopySpec, update: Boolean = false) {
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

        val type = File(outputDir, ".type")
        if(! type.exists() && contentType == ContentType.DATA) {
            type.createNewFile()
            type.appendText(contentType.toString())
        }
    }

}
