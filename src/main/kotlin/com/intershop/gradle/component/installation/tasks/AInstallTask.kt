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
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.DestinationRootCopySpec
import org.gradle.api.internal.file.copy.FileCopyAction
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.util.PatternSet
import org.gradle.util.GFileUtils
import java.io.File

abstract class AInstallTask : Sync() {

    companion object {
        const val ERRORMSG = "No copy destination directory has been specified, use 'into' to specify a target directory."
    }

    private val installPathProperty: Property<String> = project.objects.property(String::class.java)
    private val targetIncludedProperty = project.objects.property<Boolean>()
    private val updateExcludesProperty: SetProperty<String> = project.objects.setProperty(String::class.java)
    private val contentTypeProperty: Property<ContentType> = project.objects.property(ContentType::class.java)
    private val fileItemsProperty: SetProperty<FileItem> = project.objects.setProperty(FileItem::class.java)

    private val preserveInDestination = PatternSet()

    override fun createCopyAction(): CopyAction {
        val destinationDir = destinationDir ?: throw InvalidUserDataException(ERRORMSG)

        return SyncCopyActionDecorator(destinationDir, FileCopyAction(fileLookup.getFileResolver(destinationDir)),
                preserveInDestination, directoryFileTreeFactory, System.currentTimeMillis())
    }

    override fun getRootSpec(): DestinationRootCopySpec {
        val rootSpec = super.getRootSpec()

        if(runUpdate && ! updateExcludes.isEmpty()) {
            updateExcludes.forEach {
                rootSpec.exclude(it)
            }
        }

        if(! fileItems.isEmpty()) {
            fileItems.forEach { item ->
                if (item.filePath.startsWith(installPath) &&
                        ((runUpdate && item.updatable && item.contentType != ContentType.DATA) || !runUpdate)) {

                    rootSpec.exclude(item.filePath)
                    rootSpec.from(item.file) {
                        it.into(item.filePath)
                    }
                }
            }
        }

        if(targetIncluded) {
            rootSpec.eachFile { details ->
                details.path = removeDirFromPath(installPath, details.path)
            }
        }

        rootSpec.duplicatesStrategy = DuplicatesStrategy.FAIL

        return rootSpec
    }

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

    init {
        // set default values ...
        installPathProperty.set("")
        targetIncludedProperty.set(false)
    }

    abstract fun specifyCopyConfiguration()

    @get:Input
    var installPath: String by installPathProperty

    fun provideInstallPath(installPath: Provider<String>) = installPathProperty.set(installPath)

    @get:Input
    var targetIncluded: Boolean by targetIncludedProperty

    fun targetIncluded(targetIncluded: Provider<Boolean>) = targetIncludedProperty.set(targetIncluded)

    @get:Input
    var contentType: ContentType
        get() = contentTypeProperty.getOrElse(ContentType.UNSPECIFIED)
        set(value) = contentTypeProperty.set(value)

    fun setContentType(type: String) {
        contentTypeProperty.set(ContentType.valueOf(type))
    }

    @get:Nested
    var fileItems: Set<FileItem> by fileItemsProperty

    fun addFileItemConf(conf: FileItem) {
        fileItemsProperty.add(conf)
    }

    fun provideFileItems(fileItemConfs: Provider<Set<FileItem>>) = fileItemsProperty.set(fileItemConfs)

    @get:Input
    var updateExcludes: Set<String> by updateExcludesProperty

    fun excludesFromUpdate(updateExcludes: Provider<Set<String>>) =
            updateExcludesProperty.set(updateExcludes)

    fun provideUpdateExcludesProperty(excludes: Provider<Set<String>>) = updateExcludesProperty.set(excludes)

    @get:Internal
    var runUpdate: Boolean = false

    @TaskAction
    override fun copy() {

        specifyCopyConfiguration()

        super.copy()

        val typeFile = File(destinationDir, ".install")
        if(typeFile.exists()) {
            GFileUtils.forceDelete(typeFile)
        }

        GFileUtils.writeFile(contentType.toString(), typeFile)
    }
}