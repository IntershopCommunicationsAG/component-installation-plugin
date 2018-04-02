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
package com.intershop.gradle.component.installation

import com.intershop.gradle.test.AbstractIntegrationSpec

import static org.gradle.testkit.runner.TaskOutcome.SUCCESS

class MavenSnapshotTest extends AbstractIntegrationSpec {

    def "test maven local maven repo with snapshots"() {
        given:
        String projectName = "testdeployment"
        createSettingsGradle(projectName)

        buildFile << """
        plugins {
            id 'java'
            id 'maven-publish'
        }

        group 'com.intershop.test'
        version = '1.0.0'
        
        publishing {
            publications {
                mavenJava(MavenPublication) {
                    from components.java
                }
            }
            repositories {
                maven {
                    // change to point to your repo, e.g. http://my.org/repo
                    url "\$buildDir/repo"
                }
            }
        }

        """.stripIndent()
        writeJavaTestClass("com.intershop.Test")

        when:
        def result1 = getPreparedGradleRunner()
                .withArguments('publish', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':publish').outcome == SUCCESS

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments('publish', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':publish').outcome == SUCCESS

        when:
        buildFile.text = ""
        buildFile << """
        plugins {
            id 'java'
            id 'maven-publish'
        }

        group 'com.intershop.test'
        version = '1.1.0'
        
        publishing {
            publications {
                mavenJava(MavenPublication) {
                    from components.java
                }
            }
            repositories {
                maven {
                    // change to point to your repo, e.g. http://my.org/repo
                    url "\$buildDir/repo"
                }
            }
        }

        """.stripIndent()
        def result3 = getPreparedGradleRunner()
                .withArguments('publish', '-s')
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result3.task(':publish').outcome == SUCCESS

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
}
