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

import java.util.*

/**
 * Data class for http(s) credentials. This is used
 * for basic authentication.
 *
 * @property username the login name of the user
 * @property password password of the user
 */
data class Credentials(val username: String, val password: String) {

    /**
     * Read only variable for authentication string
     * of the basic http(s) authentication.
     *
     * @property authString base64 encoded string of joined username and password.
     */
    val authString: String
        get() = Base64.getEncoder().encodeToString("$username:$password".toByteArray())

}
