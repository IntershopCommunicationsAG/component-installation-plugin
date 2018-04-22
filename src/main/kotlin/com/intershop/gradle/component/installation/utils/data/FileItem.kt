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
package com.intershop.gradle.component.installation.utils.data

import com.intershop.gradle.component.installation.utils.ContentType
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import java.io.File

/**
 * File item data object to configure install tasks combines
 * the real file with the configuration from the descriptor.
 *
 * @property file           real file on file system.
 * @property filePath       the file path on the target system.
 * @property targetPath     the target path without the file name.
 * @property contentType    the content type of this file item.
 * @property updatable      this is true if the file can be updated.
 *
 * @constructor initialize a complete file item.
 */
data class FileItem constructor(
        @get:InputFile
        val file: File,
        @get:Input
        val filePath: String,
        @get: Input
        val targetPath: String,
        @get:Input
        val contentType: ContentType = ContentType.IMMUTABLE,
        @get:Input
        val updatable: Boolean = true)
