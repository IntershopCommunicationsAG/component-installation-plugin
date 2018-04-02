/*
 * Copyright 2018 Intershop Communications AG.
 *
 * Licensed under the Apache License, VersionComparator 2.0 (the "License");
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

/**
 * Comparator for version strings.
 * As long the version follow semantiv version scheme
 * and all parts must be digits.
 */
class VersionComparator : Comparator<String> {

    companion object {

        @Suppress("ReturnCount")
        private fun compareVersion(version1: String, version2: String): Int {
            val arr1 = version1.split(".").dropLastWhile { it.isEmpty() }.map { d -> d.toInt() }
            val arr2 = version2.split(".").dropLastWhile { it.isEmpty() }.map { d -> d.toInt() }

            var i = 0
            while (i < arr1.size || i < arr2.size) {
                if (i < arr1.size && i < arr2.size) {
                    val r1 = arr1[i].compareTo(arr2[i])
                    if(r1 != 0) {
                        return r1
                    } else {
                        val result = specialCompare(arr1, arr2)
                        if (result != 0) {
                            return result
                        }
                    }
                } else if (i < arr1.size) {
                    if (arr1[i] != 0) {
                        return 1
                    }
                } else if (i < arr2.size) {
                    if (arr2[i] != 0) {
                        return -1
                    }
                }

                i++
            }

            return 0
        }

        @Suppress("ReturnCount")
        private fun specialCompare(version1: List<Int>, version2: List<Int>): Int {
            for (i in 1 until version1.size) {
                if (version1[i] != 0) {
                    return 0
                }
            }
            for (j in 1 until version2.size) {
                if (version2[j] != 0) {
                    return 0
                }
            }
            return if (version1.size < version2.size) -1 else 1
        }
    }

    /**
     * Compares its two versions for order.  Returns a negative integer,
     * zero, or a positive integer as the first argument is less than, equal
     * to, or greater than the second.
     *
     * @param version1 the first version to be compared.
     * @param version2 the second version to be compared.
     */
    override fun compare(version1: String?, version2: String?): Int {
        return if (version1.isNullOrBlank() && version2.isNullOrBlank()) {
            0
        } else if (version1.isNullOrBlank() && ! version2.isNullOrBlank()) {
            -1
        } else if (! version1.isNullOrBlank() && version2.isNullOrBlank()) {
            1
        } else {
            val versRegex = "[\\d*\\.]*".toRegex()

            if(!((version1 ?: "").matches(versRegex) && (version2 ?: "").matches(versRegex))) {
                (version1 ?: "").compareTo(version2 ?: "")
            } else {
                compareVersion(version1.toString(), version2.toString())
            }
        }
    }


}
