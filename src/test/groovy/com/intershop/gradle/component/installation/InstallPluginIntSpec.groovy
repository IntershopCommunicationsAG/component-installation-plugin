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
import com.intershop.gradle.test.util.TestDir
import org.gradle.testkit.runner.TaskOutcome
import spock.lang.Shared
import spock.lang.Unroll

class InstallPluginIntSpec extends AbstractIntegrationSpec {

    @TestDir
    File tempProjectDir

    @Shared
    def repoConfig = new CentralTestRepo(System.getProperty("centralrepo"))

    @Unroll
    def 'Test plugin - production environment with update - #gradleVersion'(gradleVersion){
        given:
        String projectName = "testdeployment"
        createSettingsGradle(projectName)

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.installation'
            id "com.dorongold.task-tree" version "1.3"
        }

        group 'com.intershop.test'
        version = '1.0.0'
   
        taskTree{
            noRepeat = true
        }

        installation {
            environment('production')

            add("com.intershop.test:testcomponent:\${project.ext.installv}")
            installDir = file('installation')
        }
       
        ${repoConfig.getRepoConfig()}
        """.stripIndent()

        when:
        List<String> argsTasks = [ 'taskTree', 'install', "-Pinstallv=1.0.0"]

        def resultTasks = getPreparedGradleRunner()
                .withArguments(argsTasks)
                .withGradleVersion(gradleVersion)
                .build()
        then:
        resultTasks.output.contains("\\--- :preInstallTestcomponent")
        resultTasks.output.contains("\\--- :preInstall")

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
        List<String> args2 = ['install', '-s', "-i", "-Pinstallv=1.1.0"]

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

    @Unroll
    def 'Test plugin - poduction environment with two components - #gradleVersion'(gradleVersion){
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

            add("com.intershop.test:testcomponentA:1.0.0")
            add("com.intershop.test:testcomponentB:1.0.0")
            installDir = file('installation')
        }
       
        ${repoConfig.getRepoConfig()}
        """.stripIndent()

        when:
        List<String> args1 = ['install', '-s', "-i", "-Pinstallv=1.0.0"]

        def result1 = getPreparedGradleRunner()
                .withArguments(args1)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':installTestcomponentA').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentB').outcome == TaskOutcome.SUCCESS

        new File(testProjectDir, 'installation/testcompA').exists()
        new File(testProjectDir, 'installation/testcompB').exists()

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args1)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':installTestcomponentA').outcome == TaskOutcome.SUCCESS
        result2.task(':installTestcomponentB').outcome == TaskOutcome.SUCCESS

        result2.task(':installTestcomponentBComponentDescriptor').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentALibs').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentBLibs').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentBModuleTestmodule3').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentBModuleTestmodule4').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentBModuleTestmodule5').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentBPkgStartscriptsBin').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentAComponentDescriptor').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentAModuleTestmodule1').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentAModuleTestmodule2').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentAPkgStartscriptsBin').outcome == TaskOutcome.UP_TO_DATE

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test configuration - exclude - #gradleVersion'(gradleVersion) {
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

            add("com.intershop.test:testcomponent:\${project.ext.installv}") {
                exclude("**/**/test1.file")
            }
            installDir = file('installation') 
        }
       
       ${repoConfig.getRepoConfig()}
        """.stripIndent()

        when:
        List<String> args1 = ['install', '-s', '-i', "-Pinstallv=1.0.0"]

        def result1 = getPreparedGradleRunner()
                .withArguments(args1)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':installTestcomponentModuleTestmodule1').outcome == TaskOutcome.SUCCESS
        ! new File(testProjectDir, 'installation/testcomp/testmodule1/testmodule/testfiles/test1.file').exists()
        new File(testProjectDir, 'installation/testcomp/testmodule1/testmodule/testconf/test2.conf').exists()

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test configuration - exclude and preserve - #gradleVersion'(gradleVersion) {
        given:
        String projectName = "testdeployment"
        createSettingsGradle(projectName)

        File testFile = new File(testProjectDir, 'installation/testcomp/share/system/config/test1.properties')

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.installation'
        }

        group 'com.intershop.test'
        version = '1.0.0'
   
        installation {
            environment('production')

            add("com.intershop.test:testcomponent:\${project.ext.installv}") {
            }
            installDir = file('installation')
        }
       
        ${repoConfig.getRepoConfig()}
        """.stripIndent()

        when:
        List<String> args1 = ['install', '-s', '-i', "-Pinstallv=1.1.0"]

        def result1 = getPreparedGradleRunner()
                .withArguments(args1)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':installTestcomponent').outcome == TaskOutcome.SUCCESS

        when:
        testFile.text = "property1 = value3"

        buildFile.delete()
        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.installation'
        }

        group 'com.intershop.test'
        version = '1.0.0'
   
        installation {
            environment('production')

            add("com.intershop.test:testcomponent:\${project.ext.installv}") {
                // file will be not copied ...
                exclude("**/**/test1.properties")
                // ... but not deleted
                preserve {
                    include "**/**/test1.properties"
                }
            }
            installDir = file('installation')
        }

        ${repoConfig.getRepoConfig()}
        """.stripIndent()

        def result2 = getPreparedGradleRunner()
                .withArguments(args1)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':installTestcomponent').outcome == TaskOutcome.SUCCESS
        testFile.text == "property1 = value3"

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test configuration - exclude and preserve item configuration - #gradleVersion'(gradleVersion) {
        given:
        String projectName = "testdeployment"
        createSettingsGradle(projectName)

        def testFile1 = new File(testProjectDir, 'installation/testcomp/share/sites/org1/.switch')
        def testFile2 = new File(testProjectDir, 'installation/testcomp/share/sites/org2/.switch')


        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.installation'
        }

        group 'com.intershop.test'
        version = '1.0.0'
   
        installation {
            environment('production')

            add("com.intershop.test:testcomponent:\${project.ext.installv}") {
            }
            installDir = file('installation')
        }
       
        ${repoConfig.getRepoConfig()}
        """.stripIndent()

        when:
        List<String> args1 = ['install', '-s', '-i', "-Pinstallv=1.1.0"]

        def result1 = getPreparedGradleRunner()
                .withArguments(args1)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':installTestcomponent').outcome == TaskOutcome.SUCCESS
        testFile1.text == "1"
        testFile2.text == "1"

        when:
        testFile1.text = "2"
        testFile2.text = "2"

        def result2 = getPreparedGradleRunner()
                .withArguments(args1)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':installTestcomponent').outcome == TaskOutcome.SUCCESS
        testFile1.text == "2"
        testFile2.text == "2"

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test configuration - exclude and preserve comp configuration - #gradleVersion'(gradleVersion) {
        given:
        String projectName = "testdeployment"
        createSettingsGradle(projectName)

        def testFile1 = new File(testProjectDir, 'installation/testcomp/share/sites/org1/.switch')
        def testFile2 = new File(testProjectDir, 'installation/testcomp/share/sites/org2/.switch')

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.installation'
        }

        group 'com.intershop.test'
        version = '1.0.0'
   
        installation {
            environment('production')

            add("com.intershop.test:testcomponent:\${project.ext.installv}") {
            }
            installDir = file('installation')
        }
       
        ${repoConfig.getRepoConfig()}
        """.stripIndent()

        when:
        List<String> args1 = ['install', '-s', '-i', "-Pinstallv=1.2.0"]

        def result1 = getPreparedGradleRunner()
                .withArguments(args1)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':installTestcomponent').outcome == TaskOutcome.SUCCESS
        testFile1.text == "1"
        testFile2.text == "1"

        when:
        testFile1.text = "2"
        testFile2.text = "2"

        def result2 = getPreparedGradleRunner()
                .withArguments(args1)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':installTestcomponent').outcome == TaskOutcome.SUCCESS
        testFile1.text == "2"
        testFile2.text == "2"

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test configuration - updatable container - #gradleVersion'(gradleVersion) {
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

            add("com.intershop.test:testcomponent:\${project.ext.installv}") {
            }
            installDir = file('installation')
        }
       
        ${repoConfig.getRepoConfig()}
        """.stripIndent()

        when:
        List<String> args1 = ['install', '-s', '-i', "-Pinstallv=1.3.0"]

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
        result1.task(':installTestcomponentComponentDescriptor').outcome == TaskOutcome.SUCCESS
        result1.task(':cleanupTestcomponentCleanUp').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentLibs').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentPkgStartscriptsBin').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentPkgShareSites').outcome == TaskOutcome.SUCCESS

        new File(testProjectDir,'installation/testcomp/share/system/config/test1.properties').exists()

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args1)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':installTestcomponent').outcome == TaskOutcome.SUCCESS
        result2.task(':installTestcomponentModuleTestmodule1').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentModuleTestmodule2').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentModuleTestmodule3').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentModuleTestmodule4').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentComponentDescriptor').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':cleanupTestcomponentCleanUp').outcome == TaskOutcome.SUCCESS
        result2.task(':installTestcomponentLibs').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentPkgStartscriptsBin').outcome == TaskOutcome.UP_TO_DATE
        result2.tasks.find { it.path == ':installTestcomponentPkgShareSites' } == null

        new File(testProjectDir,'installation/testcomp/share/system/config/test1.properties').exists()

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test configuration - updatable file - #gradleVersion'(gradleVersion) {
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

            add("com.intershop.test:testcomponent:\${project.ext.installv}") {
            }
            installDir = file('installation')
        }
       
        ${repoConfig.getRepoConfig()}
        """.stripIndent()

        when:
        List<String> args1 = ['install', '-s', '-i', "-Pinstallv=1.4.0"]

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
        result1.task(':installTestcomponentComponentDescriptor').outcome == TaskOutcome.SUCCESS
        result1.task(':cleanupTestcomponentCleanUp').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentLibs').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentPkgStartscriptsBin').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentPkgShareSites').outcome == TaskOutcome.SUCCESS

        new File(testProjectDir,'installation/testcomp/share/system/config/test1.properties').text == 'property1 = value1'

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args1)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':installTestcomponent').outcome == TaskOutcome.SUCCESS
        result2.task(':installTestcomponentModuleTestmodule1').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentModuleTestmodule2').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentModuleTestmodule3').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentModuleTestmodule4').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentComponentDescriptor').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':cleanupTestcomponentCleanUp').outcome == TaskOutcome.SUCCESS
        result2.task(':installTestcomponentLibs').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentPkgStartscriptsBin').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentPkgShareSites').outcome == TaskOutcome.SUCCESS

        new File(testProjectDir,'installation/testcomp/share/system/config/test1.properties').text == 'changed --- fromZip.file'

        where:
        gradleVersion << supportedGradleVersions
    }

    @Unroll
    def 'Test configuration - updatable module - #gradleVersion'(gradleVersion) {
        given:
        String projectName = "testdeployment"
        createSettingsGradle(projectName)

        def testFile = new File(testProjectDir, 'installation/testcomp/testmodule5/testmodule/testconf/test52.conf')

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.installation'
        }

        group 'com.intershop.test'
        version = '1.0.0'
   
        installation {
            environment('production')

            add("com.intershop.test:testcomponent:\${project.ext.installv}") {
            }
            installDir = file('installation')
        }
       
        ${repoConfig.getRepoConfig()}
        """.stripIndent()

        when:
        List<String> args1 = ['install', '-s', '-i', "-Pinstallv=1.5.0"]

        def result1 = getPreparedGradleRunner()
                .withArguments(args1)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(':installTestcomponent').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentModuleTestmodule1').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentModuleTestmodule2').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentModuleTestmodule3').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentModuleTestmodule5').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentComponentDescriptor').outcome == TaskOutcome.SUCCESS
        result1.task(':cleanupTestcomponentCleanUp').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentLibs').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentPkgStartscriptsBin').outcome == TaskOutcome.SUCCESS
        result1.task(':installTestcomponentPkgShareSites').outcome == TaskOutcome.SUCCESS

        testFile.text == 'test52.conf'

        when:
        testFile.text = 'changed by other process'

        def result2 = getPreparedGradleRunner()
                .withArguments(args1)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(':installTestcomponent').outcome == TaskOutcome.SUCCESS
        result2.task(':installTestcomponentModuleTestmodule1').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentModuleTestmodule2').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentModuleTestmodule3').outcome == TaskOutcome.UP_TO_DATE
        result2.tasks.find { it.path == ':installTestcomponentModuleTestmodule5' } == null
        result2.task(':installTestcomponentComponentDescriptor').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':cleanupTestcomponentCleanUp').outcome == TaskOutcome.SUCCESS
        result2.task(':installTestcomponentLibs').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentPkgStartscriptsBin').outcome == TaskOutcome.UP_TO_DATE
        result2.task(':installTestcomponentPkgShareSites').outcome == TaskOutcome.SUCCESS

        testFile.text == 'changed by other process'

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
