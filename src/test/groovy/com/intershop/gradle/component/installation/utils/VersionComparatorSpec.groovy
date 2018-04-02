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

import spock.lang.Specification

class VersionComparatorSpec extends Specification {

    def 'Test comparator for correct sematic versions'() {
        given:
        def versionList = ["1.2.0", "1.1.1", "1.0.0", "2.0.0"]

        when:
        versionList.sort(new VersionComparator())

        then:
        versionList == ["1.0.0", "1.1.1", "1.2.0", "2.0.0"]
    }

    def 'Test comparator for sematic versions'() {
        given:
        def versionList = ["1.2", "1.1.1", "1", "2"]

        when:
        versionList.sort(new VersionComparator())

        then:
        versionList == ["1", "1.1.1", "1.2", "2"]
    }

    def 'Test comparator for string versions'() {
        given:
        def versionList = ["f.g.h", "c.d.e", "a.b.c", "b.c.d"]

        when:
        versionList.sort(new VersionComparator())

        then:
        versionList == ["a.b.c", "b.c.d", "c.d.e", "f.g.h"]
    }

    def 'Test comparator for versions'() {
        given:
        def versionList = ["1.2-a", "1.1.1-c", "1", "2"]

        when:
        versionList.sort(new VersionComparator())

        then:
        versionList == ["1", "1.1.1-c", "1.2-a", "2"]
    }

    def 'Test latest from ivy'() {
        given:
        def versionList = ["0.2a", "0.2_b", "0.2rc1", "0.2-final",
        "1.0-dev1", "1.0-dev2", "1.0-alpha1", "1.0-alpha2", "1.0-beta1", "1.0-beta2",
        "1.0-gamma", "1.0-rc1", "1.0-rc2", "1.0", "1.0.1", "2.0"]

        when:
        versionList.sort(new VersionComparator())

        then:
        versionList.last() == "2.0"
    }
}
