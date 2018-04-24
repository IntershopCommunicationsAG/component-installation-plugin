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

import com.intershop.gradle.component.installation.utils.OSType.Companion.checkClassifierForOS

/**
 * Data class for artifacts.
 *
 * @property artifact   name of the artifact.
 * @property type       type of artifact.
 * @property ext        extensions of artifact.
 * @property classifier classifier of the artifact.
 */
data class Artifact @JvmOverloads constructor(val artifact: String,
                                              val type: String,
                                              val ext: String,
                                              val classifier: String = "") {

    companion object {
        /**
         * Get an artifact for the specified parameters. The classifier is
         * verified with current OS.
         *
         * @param name          name of the artifact.
         * @param type          type of artifact.
         * @param extension     extensions of artifact.
         * @param classifier    classifier of the artifact.
         */
        @JvmStatic
        fun getArtifact(name: String, type: String,  extension: String, classifier: String) : Artifact{
            return if (checkClassifierForOS(classifier)) {
                Artifact(name, type, extension, classifier)
            } else {
                Artifact(name, type, extension)
            }
        }
    }
}
