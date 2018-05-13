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
package com.intershop.gradle.component.installation.extension

import com.intershop.gradle.component.installation.utils.ContentType
import java.io.File

/**
 * Class for the configuration of a file item for
 * installation from the local file system.
 * The target path is a relative path for the installed
 * component.
 *
 * @property file  File instance in the local file system.
 * @property targetPath relative target for the file
 * @property classifier for the configuration of the target OS. The default value is an empty string.
 * @property types for the environment configuration. The default value is an empty set.
 *
 * @constructor initialize a preconfigured local file item.
 */
data class LocalFileItem @JvmOverloads constructor(val file: File,
                                                   val targetPath: String,
                                                   val classifier: String = "",
                                                   val types: MutableSet<String> = mutableSetOf()) {

    /**
     * The content type of this file item. The default value
     * is 'IMMUTABLE'.
     *
     * @property contentType the string representation of this configuration.
     */
    var contentType: String = ContentType.IMMUTABLE.toString()

    /**
     * The update configuration of this file. The default value is true.
     *
     * @property updatable this is true if the file can be updated.
     */
    var updatable: Boolean = true
}
