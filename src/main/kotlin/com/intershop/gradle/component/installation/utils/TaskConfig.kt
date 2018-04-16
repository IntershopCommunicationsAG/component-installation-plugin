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
package com.intershop.gradle.component.installation.utils

import com.intershop.gradle.component.installation.ComponentInstallPlugin
import com.intershop.gradle.component.installation.utils.data.FileItem
import org.gradle.api.Task
import org.gradle.model.ModelMap
import java.io.File

class TaskConfig(val tasks: ModelMap<Task>, val commonName: String, val compInstallDir: File) {

    companion object {
        const val INSTALLTASKNAME = "install"

        @JvmStatic
        fun getTargetDir(dir: File, vararg path: String): File {
            val extPath = path.filter { ! it.isNullOrBlank()}.map { it.replace(" ", "_")}.
                    filter { it.matches("[a-z_\\-0-9\\.]+".toRegex()) }.
                    joinToString( "/" )

            return when {
                extPath.isNotBlank() -> File(dir, extPath)
                else -> dir
            }
        }
    }

    val taskPrefix = "${INSTALLTASKNAME}${commonName.capitalize()}"
    val fileItemSet: MutableSet<FileItem> = mutableSetOf()

    var compInstallTask: Task? = null

    init {
        if (tasks.get(taskPrefix) == null) {
            tasks.create(taskPrefix) {
                it.group = ComponentInstallPlugin.INSTALLGROUPNAME
                it.description = "Run installation of '${commonName}'"
            }
            val installTask = tasks.get(ComponentInstallPlugin.INSTALLTASKNAME)
            installTask?.dependsOn(taskPrefix)
        }
        compInstallTask = tasks.get(taskPrefix)
    }

    var update = false

    fun getTasknameFor(vararg suffix: String): String {
        return "$taskPrefix${suffix.asList().map { it.capitalize() }.joinToString("")}"
    }

    fun getTargetDir(vararg path: String): File {
        return getTargetDir(compInstallDir, *path)
    }


}