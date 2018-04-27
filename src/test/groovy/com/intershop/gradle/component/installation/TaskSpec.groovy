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
import org.gradle.testkit.runner.TaskOutcome

import java.nio.file.Files

class TaskSpec extends AbstractIntegrationSpec {

    def "Test install task"() {
        setup:
        copyResources("startscripts.zip", "startscripts.zip")

        def testFile = new File(testProjectDir, "install/local/bin/startscript4.sh")
        testFile.parentFile.mkdirs()
        testFile.createNewFile()
        testFile << "test"

        def unpackagedFile = new File(testProjectDir, 'install/local/abina/startscript1.sh')

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.installation'
        }
        
        import com.intershop.gradle.component.installation.tasks.InstallTask

        task installTest(type: InstallTask) {
            from(project.zipTree('startscripts.zip')) {
                eachFile { details ->
                    details.path = details.path.replace("bin", "abina")
                }
                
                into 'local'
            }
            into 'install'
        }
        """.stripIndent()

        when:
        List<String> args = ['installTest', '-s']

        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .build()

        then:
        result1.task(":installTest").outcome == TaskOutcome.SUCCESS
        ! testFile.exists()
        unpackagedFile.exists()

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .build()

        then:
        result2.task(":installTest").outcome == TaskOutcome.UP_TO_DATE
    }

    def "Test link task"() {
        setup:

        def target = new File(testProjectDir, "targetDir")
        target.mkdirs()

        def linkName = new File(testProjectDir, "linkName")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.installation'
        }
        
        import com.intershop.gradle.component.installation.tasks.LinkTask

        task linkTest(type: LinkTask) {
            addLink("${linkName.absolutePath}", "${target.absolutePath}")
        }
        """.stripIndent()

        when:
        List<String> args = ['linkTest', '-s']

        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .build()

        then:
        result1.task(":linkTest").outcome == TaskOutcome.SUCCESS
        linkName.exists()
        Files.readSymbolicLink(linkName.toPath()).equals(target.toPath())

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .build()

        then:
        result2.task(":linkTest").outcome == TaskOutcome.SUCCESS
    }

    def "Test link task - link exists"() {
        setup:

        def target = new File(testProjectDir, "targetDir")
        target.mkdirs()

        def linkName = new File(testProjectDir, "linkName")

        Files.createSymbolicLink(linkName.toPath(), target.toPath())

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.installation'
        }
        
        import com.intershop.gradle.component.installation.tasks.LinkTask

        task linkTest(type: LinkTask) {
            addLink("${linkName.absolutePath}", "${target.absolutePath}")
        }
        """.stripIndent()

        when:
        List<String> args = ['linkTest', '-s']

        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .build()

        then:
        result1.task(":linkTest").outcome == TaskOutcome.SUCCESS
        linkName.exists()
        Files.readSymbolicLink(linkName.toPath()).equals(target.toPath())

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .build()

        then:
        result2.task(":linkTest").outcome == TaskOutcome.SUCCESS
    }

    def "Test link task - link exists, with different target"() {
        setup:

        def target = new File(testProjectDir, "targetDir")
        target.mkdirs()

        def target2 = new File(testProjectDir, "targetDir2")
        target2.mkdirs()

        def linkName = new File(testProjectDir, "linkName")

        Files.createSymbolicLink(linkName.toPath(), target2.toPath())

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.installation'
        }
        
        import com.intershop.gradle.component.installation.tasks.LinkTask

        task linkTest(type: LinkTask) {
            addLink("${linkName.absolutePath}", "${target.absolutePath}")
        }
        """.stripIndent()

        when:
        List<String> args = ['linkTest', '-s']

        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .build()

        then:
        result1.task(":linkTest").outcome == TaskOutcome.SUCCESS
        linkName.exists()
        Files.readSymbolicLink(linkName.toPath()).equals(target.toPath())

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .build()

        then:
        result2.task(":linkTest").outcome == TaskOutcome.SUCCESS
    }

    def "Test link task - link exists, with different target, target does not exists"() {
        setup:

        def target = new File(testProjectDir, "targetDir")
        target.mkdirs()

        def target2 = new File(testProjectDir, "targetDir2")

        def linkName = new File(testProjectDir, "linkName")

        Files.createSymbolicLink(linkName.toPath(), target2.toPath())

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.installation'
        }
        
        import com.intershop.gradle.component.installation.tasks.LinkTask

        task linkTest(type: LinkTask) {
            addLink("${linkName.absolutePath}", "${target.absolutePath}")
        }
        """.stripIndent()

        when:
        List<String> args = ['linkTest', '-s']

        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .build()

        then:
        result1.task(":linkTest").outcome == TaskOutcome.SUCCESS
        linkName.exists()
        Files.readSymbolicLink(linkName.toPath()).equals(target.toPath())

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .build()

        then:
        result2.task(":linkTest").outcome == TaskOutcome.SUCCESS
    }

    def "Test directory task"() {
        setup:

        def target = new File(testProjectDir, "targetDir")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.installation'
        }
        
        import com.intershop.gradle.component.installation.tasks.DirectoryTask

        task directoryTest(type: DirectoryTask) {
            directoryPath = "${target.absolutePath}"
        }
        """.stripIndent()

        when:
        List<String> args = ['directoryTest', '-s']

        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .build()

        then:
        result1.task(":directoryTest").outcome == TaskOutcome.SUCCESS
        target.exists() && target.isDirectory()

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .build()

        then:
        result2.task(":directoryTest").outcome == TaskOutcome.UP_TO_DATE
    }

    def "Test directory task - content changed"() {
        setup:

        def target = new File(testProjectDir, "targetDir")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.installation'
        }
        
        import com.intershop.gradle.component.installation.tasks.DirectoryTask

        task directoryTest(type: DirectoryTask) {
            directoryPath = "${target.absolutePath}"
        }
        """.stripIndent()

        when:
        List<String> args = ['directoryTest', '-s']

        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .build()

        then:
        result1.task(":directoryTest").outcome == TaskOutcome.SUCCESS
        target.exists() && target.isDirectory()

        when:
        def content = new File(target, "new.file")
        content.createNewFile()
        content << "content"

        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .build()

        then:
        result2.task(":directoryTest").outcome == TaskOutcome.UP_TO_DATE
    }

    def "Test directory task - install file removed"() {
        setup:

        def target = new File(testProjectDir, "targetDir")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.installation'
        }
        
        import com.intershop.gradle.component.installation.tasks.DirectoryTask

        task directoryTest(type: DirectoryTask) {
            directoryPath = "${target.absolutePath}"
        }
        """.stripIndent()

        when:
        List<String> args = ['directoryTest', '-s']

        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .build()

        then:
        result1.task(":directoryTest").outcome == TaskOutcome.SUCCESS
        target.exists() && target.isDirectory()

        when:
        def installFile = new File(target, ".install")
        installFile.delete()

        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .build()

        then:
        result2.task(":directoryTest").outcome == TaskOutcome.SUCCESS
    }

    def "Test directory task - directory removed"() {
        setup:

        def target = new File(testProjectDir, "targetDir")

        buildFile << """
        plugins {
            id 'com.intershop.gradle.component.installation'
        }
        
        import com.intershop.gradle.component.installation.tasks.DirectoryTask

        task directoryTest(type: DirectoryTask) {
            directoryPath = "${target.absolutePath}"
        }
        """.stripIndent()

        when:
        List<String> args = ['directoryTest', '-s']

        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .build()

        then:
        result1.task(":directoryTest").outcome == TaskOutcome.SUCCESS
        target.exists() && target.isDirectory()

        when:
        target.deleteDir()

        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .build()

        then:
        result2.task(":directoryTest").outcome == TaskOutcome.SUCCESS
    }
}
