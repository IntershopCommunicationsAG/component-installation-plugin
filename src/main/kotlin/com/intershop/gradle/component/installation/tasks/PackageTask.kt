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

import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.InputFile
import java.io.File

open class PackageTask: AInstallTask() {

    private val fileContainerProperty: RegularFileProperty = project.layout.fileProperty()

    @get:InputFile
    var fileContainer: File
        get() = fileContainerProperty.get().asFile
        set(value) = fileContainerProperty.set(value)

    fun provideFileContainer(fileContainer: Provider<RegularFile>) = fileContainerProperty.set(fileContainer)

    override fun specifyCopyConfiguration() {
        super.from(project.zipTree(fileContainer))
    }


}
