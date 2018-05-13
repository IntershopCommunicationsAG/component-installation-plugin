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

import com.intershop.gradle.component.installation.filter.FormattedProperties
import org.gradle.api.Action

/**
 * Data object for the configuration of property
 * configurations from descriptor.
 */
class PropertyConfiguration {

    /**
     * Properties map to use for filter configuration.
     *
     * @property properties the key value map
     */
    val properties = mutableMapOf<String,String>()

    /**
     * Creates an action to configure FormattedProperties
     * with properties map.
     */
    fun getAction(): Action<FormattedProperties> {
        return Action { fp ->
            properties.forEach { p ->
                fp.setProperty(p.key, p.value)
            }
        }
    }
}
