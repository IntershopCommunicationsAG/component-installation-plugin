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
package com.intershop.gradle.component.installation.filter

import org.gradle.api.Transformer
import org.gradle.api.tasks.util.PatternSet

/**
 * PlaceholderReplacement filter reader edits each line of a line.
 * It replaces placeholders with a specified value. The placeholder
 * and replacements are stored in a map. The begin and end tokens
 * for the placeholders can be configured. The default values are
 * "<@" and "@>".
 *
 * @constructor initializes an patternset and a transformer in one class.
 */
class PlaceholderReplacementFilter: PatternSet(),  Transformer<String, String> {

    /**
     * Map with all placeholder and replacements.
     *
     * @property placeholders map with placeholder and replacements.
     */
    val placeholders = mutableMapOf<String, Any>()

    /**
     * Begin token for placeholders. The
     * default value is "<@".
     *
     * @property beginToken the token string
     */
    val beginToken = "<@"

    /**
     * End token for placeholders. The
     * default value is "@>".
     *
     * @property endToken the token string
     */
    val endToken = "@>"

    /**
     * Adds a key value pair to the placeholders map.
     *
     * @param placeholder the placeholder without any tokens
     * @param replacement the replacement. A String object representation is used for the replacement.
     */
    fun add(placeholder: String, replacement: Any) {
        placeholders[placeholder] = replacement
    }

    /**
     * Transforms the given object, and returns the transformed value.
     *
     * @param inputString The object to transform.
     * @return The transformed object.
     */
    override fun transform(inputString: String): String {
        var working = inputString
        placeholders.forEach {
            working = working.replace("$beginToken${it.key.trim()}$endToken", it.value.toString())
        }
        return working
    }
}
