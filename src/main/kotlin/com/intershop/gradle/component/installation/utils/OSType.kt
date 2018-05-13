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

import java.util.*

/**
 * Enumeration for the OS type of an #
 * installation process.
 */
enum class OSType {
    /**
     * OS type is windows.
     */
    WINDOWS,
    /**
     * OS type is a linux.
     */
    LINUX,
    /**
     * OS type is MacOS.
     */
    MACOS,
    /**
     * OS type is unknown.
     */
    OTHER;

    companion object {
        /**
         * Get the OSType enumeration object from a string.
         *
         * @param osKey OS type string
         *
         * @return OS type enumeration object
         */
        fun from(osKey: String): OSType {
            val internalOSKey = osKey.toLowerCase(Locale.ENGLISH)
            return when {
                (internalOSKey.indexOf("mac") >= 0 || internalOSKey.indexOf("darwin") >= 0) -> MACOS
                (internalOSKey.indexOf("win") >= 0) -> WINDOWS
                (internalOSKey.indexOf("nux") >= 0) -> LINUX
                else -> OTHER
            }
        }

        /**
         * Get the OSType enumeration object of the current OS.
         *
         * @return OS type enumeration object
         */
        fun detectedOS(): OSType {
            return from(System.getProperty("os.name", "generic"))
        }

        /**
         * Verify the parameter for the OS information.
         * It will return true, if the string is not empty
         * and matches with the current OS.
         *
         * @param classifier string with OS information.
         *
         * @return true it the classifier matches to the current OS.
         */
        fun checkClassifierForOS(classifier: String): Boolean {
            return classifier.isNotBlank() && from(classifier) == detectedOS() || classifier.isBlank()
        }

        /**
         * Verifies a list of classifiers with available OS.
         *
         * @param classifiers set of classifiers
         *
         * @return the return value is true, if the local file system
         * is included in the list of classifiers.
         */
        fun checkClassifierForOS(classifiers: Set<String>): Boolean {
            var returnValue = classifiers.isEmpty()
            val os = detectedOS()
            classifiers.forEach {
                returnValue = returnValue || from(it) == os
            }
            return returnValue
        }
    }
}
