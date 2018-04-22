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
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Unroll

class DeplyomentIntSpec extends AbstractIntegrationSpec {

    @TestDir
    File tempProjectDir

    @Unroll
    def 'Test plugin - production environment with update'() {
        given:
        String projectName = "testdeployment"
        createSettingsGradle(projectName)

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.installation'
        }

        group 'com.intershop.test'
        version = '1.0.0'
   
        installation {
            environment('production')

            add("com.intershop.test:testcomponent:\${project.ext.installv}")
            installDir = file('installation')
        }
       
        ${createRepo(testProjectDir)}

        """.stripIndent()

        when:
        List<String> args1 = ['install', '-s', '-i', "-Pinstallv=1.0.0"]

        def result1 = getPreparedGradleRunner()
                .withArguments(args1)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':installTestcomponent').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentModuleTestmodule1').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentModuleTestmodule2').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentModuleTestmodule3').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentModuleTestmodule4').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentModuleTestmodule5').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentComponentDescriptor').outcome == TaskOutcome.SUCCESS
        result1.task(':cleanupTestcomponentCleanUp').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentLibs').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentPkgStartscriptsBin').outcome == TaskOutcome.SUCCESS

        new File(testProjectDir, 'installation/testcomp/bin/.install').text == 'IMMUTABLE'
        new File(testProjectDir, 'installation/testcomp/component/.install').text == 'IMMUTABLE'
        new File(testProjectDir, 'installation/testcomp/lib/release/libs/.install').text == 'IMMUTABLE'
        new File(testProjectDir, 'installation/testcomp/testmodule1/.install').text == 'IMMUTABLE'
        new File(testProjectDir, 'installation/testcomp/testmodule2/.install').text == 'IMMUTABLE'
        new File(testProjectDir, 'installation/testcomp/testmodule3/.install').text == 'IMMUTABLE'
        new File(testProjectDir, 'installation/testcomp/testmodule4/.install').text == 'IMMUTABLE'
        new File(testProjectDir, 'installation/testcomp/testmodule5/.install').text == 'IMMUTABLE'

        new File(testProjectDir, 'installation/testcomp/lib/release/libs/com.intershop_library1_1.0.0.jar').exists()
        new File(testProjectDir, 'installation/testcomp/lib/release/libs/com.intershop_library1_1.0.0.pom').exists()
        new File(testProjectDir, 'installation/testcomp/testmodule1/libs/extlib-jar-1.0.0.jar').exists()
        new File(testProjectDir, 'installation/testcomp/testmodule1/libs/testmodule1-jar-1.0.0.jar').exists()
        new File(testProjectDir, 'installation/testcomp/testmodule1/testmodule/testconf/test2.conf').exists()
        new File(testProjectDir, 'installation/testcomp/testmodule5/testmodule/testconf/test52.conf').exists()

        when:
        List<String> args2 = ['install', '-s', "-Pinstallv=1.1.0"]

        def result2 = getPreparedGradleRunner()
                .withArguments(args2)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':installTestcomponent').outcome == TaskOutcome.SUCCESS
        result2.task(':installTestcomponentModuleTestmodule1').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentModuleTestmodule2').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentModuleTestmodule3').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentModuleTestmodule4').outcome == TaskOutcome.UP_TO_DATE
        result2.tasks.find { it.path == ':installTestcomponentModuleTestmodule5'} == null
        result2.task(':installTestcomponentComponentDescriptor').outcome == TaskOutcome.SUCCESS
        result2.task(':cleanupTestcomponentCleanUp').outcome == TaskOutcome.SUCCESS
        result2.task(':installTestcomponentLibs').outcome == TaskOutcome.SUCCESS
        result2.task(':installTestcomponentPkgStartscriptsBin').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentPkgShareSites').outcome == TaskOutcome.SUCCESS

        new File(testProjectDir, 'installation/testcomp/share/system/config/test1.properties').exists()
        new File(testProjectDir, 'installation/testcomp/share/system/config/test1.properties').text.contains("property1 = value1")

        when:
        def result3 = getPreparedGradleRunner()
                .withArguments(args2)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result3.task(':installTestcomponent').outcome == TaskOutcome.SUCCESS
        result3.task(':installTestcomponentModuleTestmodule1').outcome == TaskOutcome.UP_TO_DATE
        result3.task(':installTestcomponentModuleTestmodule2').outcome == TaskOutcome.UP_TO_DATE
        result3.task(':installTestcomponentModuleTestmodule3').outcome == TaskOutcome.UP_TO_DATE
        result3.task(':installTestcomponentModuleTestmodule4').outcome == TaskOutcome.UP_TO_DATE
        result3.task(':installTestcomponentComponentDescriptor').outcome == TaskOutcome.UP_TO_DATE
        result3.task(':cleanupTestcomponentCleanUp').outcome == TaskOutcome.SUCCESS
        result3.task(':installTestcomponentLibs').outcome == TaskOutcome.UP_TO_DATE
        result3.task(':installTestcomponentPkgStartscriptsBin').outcome == TaskOutcome.UP_TO_DATE
        result3.task(':installTestcomponentPkgShareSites').outcome == TaskOutcome.UP_TO_DATE

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
        copyResources("descriptors/component-1.1.component", "component-1.1.component", tempProjectDir)

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
                artifact name: 'testmodule2', type: 'jar', ext: 'jar', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test1.file', content: 'interntest1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test2.file', content: 'interntest2.file')
                ]
                dependency org: 'com.intershop', name: 'library3', rev: '1.0.0'
            }
            module(org: 'com.intershop', name: 'testmodule3', rev: '1.0.0') {
                artifact name: 'testmodule3', type: 'local', ext: 'zip', classifier: 'linux', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'local/linux/test21.file', content: 'test21.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'local/linux/test22.conf', content: 'test22.conf'),
                        TestIvyRepoBuilder.ArchiveDirectoryEntry.newInstance(path: 'local/linux/empttestdir')
                ]
                artifact name: 'testmodule3', type: 'local', ext: 'zip', classifier: 'win', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'local/win/test21.file', content: 'test21.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'local/win/test22.conf', content: 'test22.conf'),
                        TestIvyRepoBuilder.ArchiveDirectoryEntry.newInstance(path: 'local/win/empttestdir')
                ]
                artifact name: 'testmodule3', type: 'local', ext: 'zip', classifier: 'macos', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'local/macos/test21.file', content: 'test21.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'local/macos/test22.conf', content: 'test22.conf'),
                        TestIvyRepoBuilder.ArchiveDirectoryEntry.newInstance(path: 'local/macos/empttestdir')
                ]
            }
            module(org: 'com.intershop', name: 'testmodule4', rev: '1.0.0') {
                artifact name: 'testmodule4', type: 'local', ext: 'zip', classifier: 'solaris', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'local/solaris/test21.file', content: 'test21.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'local/solaris/test22.conf', content: 'test22.conf'),
                        TestIvyRepoBuilder.ArchiveDirectoryEntry.newInstance(path: 'local/solaris/empttestdir')
                ]
            }
            module(org: 'com.intershop', name: 'testmodule5', rev: '1.0.0') {
                artifact name: 'testmodule5', type: 'cartridge', ext: 'zip', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testfiles/test51.file', content: 'test51.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testconf/test52.conf', content: 'test52.conf'),
                        TestIvyRepoBuilder.ArchiveDirectoryEntry.newInstance(path: 'testmodule/empttestdir')
                ]
                artifact name: 'testmodule5', type: 'jar', ext: 'jar', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test1.file', content: 'interntest1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test2.file', content: 'interntest2.file')
                ]
                dependency org: 'com.intershop', name: 'library3', rev: '1.0.0'
            }

            module(org: 'com.intershop.test', name: 'testcomponent', rev: '1.0.0') {
                artifact name: 'testcomponent', type: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME,
                        content: replaceContent(new File(tempProjectDir, "component-1.component"), ['@group@': 'com.intershop.test', '@module@': 'testcomponent', '@version@': '1.0.0'])
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
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'share/sites/org2/import.properties', content: 'interntest2.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'share/system/config/test1.properties', content: 'fromZip.file')
                ]
                artifact name: 'test1', type: 'properties', ext: 'properties', content: 'property1 = value1'
                artifact name: 'test2', type: 'properties', ext: 'properties', classifier: 'linux', content: 'property2 = value2'
            }
            module(org: 'com.intershop.test', name: 'testcomponent', rev: '1.1.0') {
                artifact name: 'testcomponent', type: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME,
                        content: replaceContent(new File(tempProjectDir, "component-1.1.component"), ['@group@': 'com.intershop.test', '@module@': 'testcomponent', '@version@': '1.1.0'])
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
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'share/sites/org2/import.properties', content: 'interntest2.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'share/system/config/test1.properties', content: 'changed --- fromZip.file')
                ]
                artifact name: 'test1', type: 'properties', ext: 'properties', content: 'property1 = value1'
                artifact name: 'test2', type: 'properties', ext: 'properties', classifier: 'linux', content: 'property2 = value2'
            }

            module(org: 'com.intershop', name: 'library4', rev: '1.0.0'){
                artifact name: 'library4', type: 'jar', ext: 'jar', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test1.file', content: 'interntest1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test2.file', content: 'interntest2.file')
                ]
            }
        }.writeTo(repoDir)


        new TestMavenRepoBuilder().repository {
            project(groupId: 'com.intershop', artifactId: 'library1', version: '1.0.0') {
                artifact entries: [
                        TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test1.file', content: 'test1.file'),
                        TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test2.file', content: 'test2.file'),
                ]
            }
            project(groupId: 'com.intershop', artifactId: 'library2', version: '1.0.0'){
                artifact entries: [
                        TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test1.file', content: 'test1.file'),
                        TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test2.file', content: 'test2.file'),
                ]
            }
            project(groupId: 'com.intershop', artifactId: 'library3', version: '1.0.0'){
                artifact entries: [
                        TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test1.file', content: 'test1.file'),
                        TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test2.file', content: 'test2.file'),
                ]
            }
            project(groupId: 'com.other', artifactId: 'library4', version: '1.0.0'){
                artifact entries: [
                        TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test1.file', content: 'test1.file'),
                        TestMavenRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test2.file', content: 'test2.file'),
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

    private File replaceContent(File orgFile, Map<String, String> replacements, File baseDir = tempProjectDir) {
        File newFile = new File(baseDir, "${orgFile.name}.2")
        newFile.withWriter { w ->
            orgFile.eachLine { line ->
                def newLine = line
                replacements.each { key, value ->
                    newLine = newLine.replaceAll(key, value)
                }
                w << newLine + '\n'
            }
        }

        return newFile
    }
}
