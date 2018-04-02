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
package com.intershop.gradle.component.installation

import com.intershop.gradle.test.AbstractIntegrationSpec
import okhttp3.mockwebserver.MockWebServer
import org.junit.Rule
import spock.lang.Ignore

@Ignore
class RepoTestSpec extends AbstractIntegrationSpec {

    @Rule
    public final MockWebServer server = new MockWebServer()

    def 'test repo server - #gradleVersion'(gradleVersion) {
        when:
        def str = ""
        then:
        //server.enqueue(new MockResponse().setBody());
        true

    }
}
