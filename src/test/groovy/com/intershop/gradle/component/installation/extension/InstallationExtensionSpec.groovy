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

import com.intershop.gradle.component.installation.ComponentInstallPlugin
import com.intershop.gradle.component.installation.filter.FilterContainer
import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification

class InstallationExtensionSpec extends Specification {

    private final Project project = ProjectBuilder.builder().build()
    InstallationExtension extension

    def setup() {
        project.pluginManager.apply(ComponentInstallPlugin)
        extension = project.extensions.getByType(InstallationExtension)
    }

    def 'extension should be added to project'() {
        expect:
        extension != null
    }

    def 'check extension'() {
        expect:
        extension.installConfig instanceof InstallConfiguration
        extension.environment instanceof Set<String>
        extension.components instanceof Set<Component>
    }

    def 'add component'() {
        when:
        extension.add("com.test:testcomp:1.0.0")

        then:
        extension.components.size() == 1
    }

    def 'add component with simple dependency map - 1'() {
        when:
        extension.add(group: 'com.intershop', name: 'testcomp', version: '1.0.0')

        then:
        extension.components.size() == 1
        extension.components.first().module == 'testcomp'
        extension.components.first().group == 'com.intershop'
        extension.components.first().version == '1.0.0'
    }

    def 'add component with simple dependency map - 2'() {
        when:
        extension.add([group: 'com.intershop', name: 'testcomp', version: '1.0.0'])

        then:
        extension.components.size() == 1
        extension.components.first().module == 'testcomp'
        extension.components.first().group == 'com.intershop'
        extension.components.first().version == '1.0.0'
    }

    def 'add component with simple dependency map and path'() {
        when:
        extension.add([group: 'com.intershop', name: 'testcomp', version: '1.0.0'], 'testPath')

        then:
        extension.components.size() == 1
        extension.components.first().module == 'testcomp'
        extension.components.first().group == 'com.intershop'
        extension.components.first().version == '1.0.0'
        extension.components.first().path == 'testPath'
    }

    def 'add components'() {
        when:
        extension.add("com.test:testcomp1:1.0.0")
        extension.add("com.test:testcomp2:1.0.0")

        then:
        extension.components.size() == 2
    }

    def 'add component twice'() {
        when:
        extension.add("com.test:testcomp:1.0.0")
        extension.add("com.test:testcomp:2.0.0")

        then:
        thrown(org.gradle.api.InvalidUserDataException)
    }

    def 'add component twice with the same path'() {
        when:
        extension.add("com.test:testcomp:1.0.0", 'wa1')
        extension.add("com.test:testcomp:2.0.0", 'wa1')

        then:
        thrown(org.gradle.api.InvalidUserDataException)
    }

    def 'add component twice with different paths'() {
        when:
        extension.add("com.test:testcomp:1.0.0", 'wa1')
        extension.add("com.test:testcomp:2.0.0", 'wa2')

        then:
        extension.components.size() == 2
    }

    def 'add component with local files'() {
        when:
        extension.add("com.test:testcomp:1.0.0", {
            it.fileItems.add(new File("test.file"), "installPath")
        })

        then:
        extension.components.first().fileItems.localFileItems.size() == 1
    }

    def 'filters configuration is correct configured'() {
        expect:
        extension.filters instanceof FilterContainer
    }
}
