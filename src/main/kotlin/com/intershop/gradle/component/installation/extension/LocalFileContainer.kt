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

import com.intershop.gradle.component.installation.utils.OSType
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.util.ConfigureUtil
import java.io.File

/**
 * This is the configuration container for local files,
 * that can be installed.
 */
class LocalFileContainer {

    /**
     * The set of local file items. The default configuration
     * is an empty set.
     *
     * @property localFileItems the set of file items.
     */
    val localFileItems = mutableSetOf<LocalFileItem>()

    /**
     * Adds a file item with a local file and
     * a target path.
     *
     * @param file the file instance of the local file.
     * @param targetPath the target path in the installation without file name.
     */
    fun add(file: File, targetPath: String) {
        val item = LocalFileItem(file, targetPath, OSType.detectedOS().toString())
        localFileItems.add(item)
    }

    /**
     * Adds a file item with a local file and
     * a target path. The configuration can be
     * adapted with an action.
     *
     * @param file the file instance of the local file.
     * @param targetPath the target path in the installation without file name.
     * @param action for the configuration of the local file item.
     */
    fun add(file: File, targetPath: String, action: Action<in LocalFileItem>) {
        val item = LocalFileItem(file, targetPath, OSType.detectedOS().toString())
        action.execute(item)
        localFileItems.add(item)
    }

    /**
     * Adds a file item with a local file and
     * a target path. The configuration can be
     * adapted with a closure.
     *
     *
     * @param file the file instance of the local file.
     * @param targetPath the target path in the installation without file name.
     * @param closure for the configuration of the local file item.
     */
    fun add(file: File, targetPath: String, closure: Closure<LocalFileItem>) {
        val item = LocalFileItem(file, targetPath, OSType.detectedOS().toString())
        ConfigureUtil.configure(closure, item)
        localFileItems.add(item)
    }
}
