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
package com.intershop.gradle.component.installation.filter

import com.intershop.gradle.test.AbstractIntegrationSpec
import org.gradle.testkit.runner.TaskOutcome

class FilterIntSpec extends AbstractIntegrationSpec {

    String buildFileTemplate = """
        plugins {
            id 'com.intershop.gradle.component.installation'
        }

        import org.gradle.api.internal.project.ProjectInternal
        import org.gradle.internal.reflect.Instantiator 
        import com.intershop.gradle.component.installation.tasks.InstallTask
        import com.intershop.gradle.component.installation.filter.FilterContainer
        import org.w3c.dom.Element
        
        class FilterExtension {
        
            private Project project
            
            def services
            def filters
            
            FilterExtension(Project project) {
                this.project = project
                services = (project as ProjectInternal).services
                filters = new FilterContainer(project, services.get(Instantiator.class))
            }
         
            def filters(Action<? super FilterContainer> action) {
                action.execute(filters)
            }       
        }

        class FilterPlugin implements Plugin<Project> {
            void apply(Project project) {
                def extension = project.extensions.create('filter', FilterExtension, project)
            }
        }
        
        apply plugin: FilterPlugin
        
        def localDirectory = "dirReplacment"
        
        filter {
            filters {
                %filterConfig%
            }
        }
        
        task installTest(type: InstallTask) {
            from 'test'
            into 'install'
        }
        
        """.stripIndent()

    def 'filter test overrideProperties'() {
        setup:

        def setupFile = new File(testProjectDir, 'test/file.properties')
        setupFile.getParentFile().mkdirs()
        setupFile.createNewFile()

        def targetFile = new File(testProjectDir, 'install/file.properties')

        setupFile << """
        test1.prop = test1
        test2.prop = test2
        """.stripIndent()

        String filterConfig = """
        overrideProperties("test1", "**/**/*.properties") {
            setProperty("test3.test", "test3")
            setProperty("test4.test", "test4")
        }
        """.stripIndent()

        buildFile << buildFileTemplate.replace("%filterConfig%", filterConfig)

        when:
        List<String> args = ['installTest', '-s']

        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(":installTest").outcome == TaskOutcome.SUCCESS

        targetFile.exists()
        targetFile.text.contains("test3.test = test3")
        targetFile.text.contains("test4.test = test4")

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(":installTest").outcome == TaskOutcome.UP_TO_DATE

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'filter test xmlContent'() {
        setup:

        def setupFile = new File(testProjectDir, 'test/file.xml')
        setupFile.getParentFile().mkdirs()
        setupFile.createNewFile()

        def targetFile = new File(testProjectDir, 'install/file.xml')

        setupFile << """
        <foo fizz="buzz">
            <bar />
        </foo>
        """.stripIndent()

        String filterConfig = """
        xmlContent("test1", "**/**/*.xml") {
            Element root = asElement()
            root.setAttribute("fizz", "baz")
            Element e = root.ownerDocument.createElement("baz")
            e.textContent = "fiz"
            root.appendChild(e)
        }
        """.stripIndent()

        buildFile << buildFileTemplate.replace("%filterConfig%", filterConfig)

        when:
        List<String> args = ['installTest', '-s']

        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(":installTest").outcome == TaskOutcome.SUCCESS

        targetFile.exists()
        targetFile.text.contains('fizz="baz"')
        targetFile.text.contains("<baz>fiz</baz>")

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(":installTest").outcome == TaskOutcome.UP_TO_DATE

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'filter test fullContent'() {
        setup:

        def setupFile = new File(testProjectDir, 'test/file.text')
        setupFile.getParentFile().mkdirs()
        setupFile.createNewFile()

        def targetFile = new File(testProjectDir, 'install/file.text')

        setupFile << """
        <configuration file>
            test = 3
        </configuration file>
        #content text
        """.stripIndent()

        String filterConfig = """
        fullContent("test1", "**/**/*.text") {
            append('bar')
        } 
        """.stripIndent()

        buildFile << buildFileTemplate.replace("%filterConfig%", filterConfig)

        when:
        List<String> args = ['installTest', '-s']

        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(":installTest").outcome == TaskOutcome.SUCCESS

        targetFile.exists()
        targetFile.text.contains('#content text')
        targetFile.text.contains("bar")

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(":installTest").outcome == TaskOutcome.UP_TO_DATE

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'filter test replacePlaceholders'() {
        setup:

        def setupFile = new File(testProjectDir, 'test/file.text')
        setupFile.getParentFile().mkdirs()
        setupFile.createNewFile()

        def targetFile = new File(testProjectDir, 'install/file.text')

        setupFile << """
        ----
        That is an test <@TEST1@>
        <@TEST2@> will be replaced
        dir = <@TEST3@>
        ----
        """.stripIndent()

        String filterConfig = """
        replacePlaceholders("test1") {
            include("**/**/*.text")
                    
            add("TEST1", "replacement1")
            add("TEST2", "replacement2")
                    
            placeholders['TEST3'] = localDirectory
        }
        """.stripIndent()

        buildFile << buildFileTemplate.replace("%filterConfig%", filterConfig)

        when:
        List<String> args = ['installTest', '-s']

        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(":installTest").outcome == TaskOutcome.SUCCESS

        targetFile.exists()
        targetFile.text.contains('That is an test replacement1')
        targetFile.text.contains('replacement2 will be replaced')
        targetFile.text.contains('dir = dirReplacment')

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(":installTest").outcome == TaskOutcome.UP_TO_DATE

        where:
        gradleVersion << supportedGradleVersions
    }

    def 'filter test closure'() {
        setup:

        def setupFile = new File(testProjectDir, 'test/file.closure')
        setupFile.getParentFile().mkdirs()
        setupFile.createNewFile()

        def targetFile = new File(testProjectDir, 'install/file.closure')

        setupFile << """
        foo
        bar
        ServerAdmin
        baz
        """.stripIndent()

        String filterConfig = """
        addClosure("test5", "**/**/*.closure") {
            String line -> line =~ /^\\w*ServerAdmin/ ? 'ServerAdmin admin@customer.com' : line
        }
        """.stripIndent()

        buildFile << buildFileTemplate.replace("%filterConfig%", filterConfig)

        when:
        List<String> args = ['installTest', '-s']

        def result1 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result1.task(":installTest").outcome == TaskOutcome.SUCCESS

        targetFile.exists()
        targetFile.text.contains('bar')
        targetFile.text.contains('ServerAdmin admin@customer.com')
        targetFile.text.contains('baz')

        when:
        def result2 = getPreparedGradleRunner()
                .withArguments(args)
                .withGradleVersion(gradleVersion)
                .build()

        then:
        result2.task(":installTest").outcome == TaskOutcome.UP_TO_DATE

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
