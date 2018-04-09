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

import com.intershop.gradle.component.installation.utils.DescriptorManager
import com.intershop.gradle.test.AbstractIntegrationSpec
import com.intershop.gradle.test.builder.TestIvyRepoBuilder
import com.intershop.gradle.test.builder.TestMavenRepoBuilder
import com.intershop.gradle.test.util.TestDir
import spock.lang.Unroll

class DeplyomentIntSpec extends AbstractIntegrationSpec {

    @TestDir
    File tempProjectDir

    @Unroll
    def 'Test plugin happy path'() {
        given:
        String projectName = "testdeployment"
        createSettingsGradle(projectName)

        File dir = new File(testProjectDir,"installation/bin")
        dir.mkdirs()
        File test = new File(dir, "startscript1.sh")
        test << "wrong file"

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.installation'
        }

        group 'com.intershop.test'
        version = '1.0.0'
   
        installation {
            environment('production')

            add('com.intershop.test:testcomponent:1.0.0')
            installDir = file('installation')
        }
       
        ${createRepo(testProjectDir)}

        """.stripIndent()

        when:
        List<String> args1 = ['install', '-s', '-i']

        def result1 = getPreparedGradleRunner()
                .withArguments(args1)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        true

        when:
        List<String> args2 = ['update', '-s', '-i']

        def result2 = getPreparedGradleRunner()
                .withArguments(args2)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        true

        when:
        List<String> args3 = ['update', '-s', '-i']

        def result3 = getPreparedGradleRunner()
                .withArguments(args2)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        true

        where:
        gradleVersion << supportedGradleVersions

    }

    File createSettingsGradle(String projectName) {
        File settingsFile = new File(testProjectDir, 'settings.gradle')
        settingsFile << """
        rootProject.name = '${projectName}'
        """.stripIndent()

        return settingsFile
    }

    String createRepo(File dir) {
        File repoDir = new File(dir, 'repo')

        copyResources("descriptors/component-1.component", "component-1.component", tempProjectDir)

        new TestIvyRepoBuilder().repository( ivyPattern: DescriptorManager.INTERSHOP_IVY_PATTERN, artifactPattern: DescriptorManager.INTERSHOP_PATTERN ) {
            module(org: 'com.intershop', name: 'testmodule1', rev: '1.0.0') {
                artifact name: 'testmodule1', type: 'cartridge', ext: 'zip', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testfiles/test1.file', content: 'test1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testconf/test2.conf', content: 'test2.conf'),
                        TestIvyRepoBuilder.ArchiveDirectoryEntry.newInstance(path: 'testmodule/empttestdir')
                ]
                artifact name: 'testmodule1', type: 'jar', ext: 'jar', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test1.file', content: 'interntest1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test2.file', content: 'interntest2.file')
                ]
                artifact name: 'extlib', type: 'jar', ext: 'jar', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/ext/test1.file', content: 'exttest1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/ext/test2.file', content: 'exttest2.file')
                ]
                dependency org: 'com.intershop', name: 'library1', rev: '1.0.0'
                dependency org: 'com.intershop', name: 'library2', rev: '1.0.0'
            }
            module(org: 'com.intershop', name: 'testmodule2', rev: '1.0.0') {
                artifact name: 'testmodule2', type: 'cartridge', ext: 'zip', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testfiles/test21.file', content: 'test21.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testconf/test22.conf', content: 'test22.conf'),
                        TestIvyRepoBuilder.ArchiveDirectoryEntry.newInstance(path: 'testmodule/empttestdir')
                ]
                artifact name: 'testmodule1', type: 'jar', ext: 'jar', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test1.file', content: 'interntest1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test2.file', content: 'interntest2.file')
                ]
                dependency org: 'com.intershop', name: 'library3', rev: '1.0.0'
            }

            module(org: 'com.intershop.test', name: 'testcomponent', rev: '1.0.0') {
                artifact name: 'testcomponent', type: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME, content: new File(tempProjectDir, "component-1.component")
                artifact name: 'startscripts', type: 'bin', ext: 'zip', classifier: 'linux', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'bin/startscript1.sh', content: 'interntest1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'bin/startscript2.sh', content: 'interntest2.file')
                        ]
                artifact name: 'startscripts', type: 'bin', ext: 'zip', classifier: 'macos', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'bin/startscript1.sh', content: 'interntest1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'bin/startscript2.sh', content: 'interntest2.file')
                ]
                artifact name: 'startscripts', type: 'bin', ext: 'zip', classifier: 'win', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'bin/startscript1.bat', content: 'interntest1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'bin/startscript2.bat', content: 'interntest2.file')
                ]
                artifact name: 'share', type: 'sites', ext: 'zip', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'share/sites/org1/import.properties', content: 'interntest1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'share/sites/org2/import.properties', content: 'interntest2.file')
                ]
                artifact name: 'test1', type: 'properties', ext: 'properties', content: 'property1 = value1'
                artifact name: 'test2', type: 'properties', ext: 'properties', content: 'property2 = value2'
            }
        }.writeTo(repoDir)


        new TestMavenRepoBuilder().repository {
            project(groupId: 'com.intershop', artifactId: 'library1', version: '1.0.0') {
                artifact entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test1.file', content: 'test1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test2.file', content: 'test2.file'),
                ]
            }
            project(groupId: 'com.intershop', artifactId: 'library2', version: '1.0.0'){
                artifact entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test1.file', content: 'test1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test2.file', content: 'test2.file'),
                ]
            }
            project(groupId: 'com.intershop', artifactId: 'library3', version: '1.0.0'){
                artifact entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test1.file', content: 'test1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test2.file', content: 'test2.file'),
                ]
            }
            project(groupId: 'com.intershop', artifactId: 'library4', version: '1.0.0'){
                artifact entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test1.file', content: 'test1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test2.file', content: 'test2.file'),
                ]
            }
        }.writeTo(repoDir)


        String repostr = """
            repositories {
                jcenter()
                ivy {
                    name 'ivyLocal'
                    url "file://${repoDir.absolutePath.replace('\\', '/')}"
                    layout('pattern') {
                        ivy "${DescriptorManager.INTERSHOP_IVY_PATTERN}"
                        artifact "${DescriptorManager.INTERSHOP_PATTERN}"
                        artifact "${DescriptorManager.INTERSHOP_IVY_PATTERN}"
                    }
                }
                maven {
                    url "file://${repoDir.absolutePath.replace('\\', '/')}"
                }
            }""".stripIndent()
    }
}
