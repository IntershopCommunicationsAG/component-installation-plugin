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
import com.intershop.gradle.test.builder.TestIvyRepoBuilder
import com.intershop.gradle.test.builder.TestMavenRepoBuilder
import spock.lang.Ignore
import spock.lang.Unroll

class DeplyomentIntSpec extends AbstractIntegrationSpec {

    public final static String ivyPattern = '[organisation]/[module]/[revision]/[type]s/ivy-[revision].xml'
    public final static String artifactPattern = '[organisation]/[module]/[revision]/[ext]s/[artifact]-[type](-[classifier])-[revision].[ext]'

    @Ignore
    @Unroll
    def 'Test plugin happy path'() {
        given:
        String projectName = "testdeployment"
        createSettingsGradle(projectName)

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.installation'
        }

        group 'com.intershop.test'
        version = '1.0.0'
   
        deploy {
            component("test/test/test", {
                from('com.intershop.test:testcomponent:1.0.0')
            })

            deploymentTarget = new File(project.buildDir, 'install')
            repositoryURL = "file://${testProjectDir.absolutePath.replace('\\\\\\\\', '/')}/repo"
            repositoryPattern = "${artifactPattern}"
        }
       
        ${createRepo(testProjectDir)}

        """.stripIndent()

        when:
        List<String> args = ['tasks', '-s', '-i']

        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        true
/**
        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        true
**/
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

        String simpleDescriptor = """
        {
          "displayName" : "testcomponent",
          "componentDescription" : "",
          "modules" : {
            "testmodule1" : {
              "name" : "testmodule1",
              "targetPath" : "testmodule1",
              "dependency" : {
                "group" : "com.intershop",
                "module" : "testmodule1",
                "version" : "1.0.0"
              },
              "pkgs" : [ "testmodule1" ],
              "jars" : [ "extlib", "testmodule1" ],
              "types" : [ "" ],
              "targetIncluded" : true,
              "classifiers" : [ ]
            },
            "testmodule2" : {
              "name" : "testmodule2",
              "targetPath" : "testmodule2",
              "dependency" : {
                "group" : "com.intershop",
                "module" : "testmodule2",
                "version" : "1.0.0"
              },
              "pkgs" : [ "testmodule2" ],
              "jars" : [ "testmodule1" ],
              "types" : [ "" ],
              "targetIncluded" : true,
              "classifiers" : [ ]
            }
          },
          "libs" : {
            "com.intershop:library1:1.0.0" : {
              "dependency" : {
                "group" : "com.intershop",
                "module" : "library1",
                "version" : "1.0.0"
              },
              "targetName" : "com.intershop_library1_1.0.0",
              "types" : [ ]
            }
          },
          "packages" : { }
        }
        """.stripIndent()

        new TestIvyRepoBuilder().repository( ivyPattern: ivyPattern, artifactPattern: artifactPattern ) {
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
                artifact name: 'testcomponent', type: 'component', ext: 'component', content: simpleDescriptor
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
                ivy {
                    name 'ivyLocal'
                    url "file://${repoDir.absolutePath.replace('\\', '/')}"
                    layout('pattern') {
                        ivy "${ivyPattern}"
                        artifact "${artifactPattern}"
                        artifact "${ivyPattern}"
                    }
                    credentials {
                        username = 'joe'
                        password = 'secret'
                    }
                }
                maven {
                    url "file://${repoDir.absolutePath.replace('\\\\', '/')}"
credentials {
            username = 'joe'
            password = 'secret'
        }
                }
                jcenter()
            }""".stripIndent()
    }
}
