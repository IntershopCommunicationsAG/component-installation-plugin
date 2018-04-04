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

import com.intershop.gradle.component.installation.utils.data.Dependency
import com.intershop.gradle.component.installation.utils.data.RepositoryType
import com.intershop.gradle.test.builder.TestIvyRepoBuilder
import com.intershop.gradle.test.builder.TestMavenRepoBuilder
import com.intershop.gradle.test.util.TestDir
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.testfixtures.ProjectBuilder
import spock.lang.Specification
import spock.lang.Unroll

class DescriptorManagerSpec extends Specification {

    /**
     * Project directory for tests
     */
    @TestDir
    File repoProjectDir

    @TestDir
    File tempProjectDir

    @TestDir
    File testProjectDir

    private final Project project = ProjectBuilder.builder().build()

    def setup() {
        File repoDir = new File(repoProjectDir, "repo")
        createRepo(repoDir)

        project.repositories {
            jcenter()
            ivy {
                name 'ivyLocal'
                url "file://${repoDir.absolutePath.replace('\\', '/')}"
                layout('pattern') {
                    ivy "${RepositoryUtil.INTERSHOP_IVY_PATTERN}"
                    artifact "${RepositoryUtil.INTERSHOP_PATTERN}"
                    artifact "${RepositoryUtil.INTERSHOP_IVY_PATTERN}"
                }
            }
            maven {
                url "file://${repoDir.absolutePath.replace('\\\\', '/')}"
            }

        }
    }

    @Unroll
    def 'Test repository handling - find version in repo'(){
        setup:
        def pattern = [RepositoryUtil.INTERSHOP_PATTERN, IvyArtifactRepository.GRADLE_ARTIFACT_PATTERN, IvyArtifactRepository.IVY_ARTIFACT_PATTERN] as Set<String>
        def dependency  = new Dependency("com.intershop.test", "test", inputVersion)
        def descrMgr = new DescriptorManager((RepositoryHandler)project.getRepositories(), dependency, pattern)

        when:
        def repo = descrMgr.getDescriptorRepository()

        then:
        repo != null
        repo.version == outputVersion
        repo.type == repoType

        where:
        inputVersion | outputVersion | repoType
        "+"          | "3.0.0"       | RepositoryType.MAVEN
        "2.0.0"      | "2.0.0"       | RepositoryType.IVY
        "1.0.0"      | "1.0.0"       | RepositoryType.IVY
        "1.0.+"      | "1.0.0"       | RepositoryType.IVY
        "2.+"        | "2.2.0"       | RepositoryType.MAVEN
        "2.0.+"      | "2.0.1"       | RepositoryType.IVY
    }

    @Unroll
    def 'Load descriptor from repo'() {
        setup:
        def pattern = [RepositoryUtil.INTERSHOP_PATTERN, IvyArtifactRepository.GRADLE_ARTIFACT_PATTERN, IvyArtifactRepository.IVY_ARTIFACT_PATTERN] as Set<String>
        def dependency  = new Dependency("com.intershop.test", "test", inputVersion)
        def descrMgr = new DescriptorManager((RepositoryHandler)project.getRepositories(), dependency, pattern)
        def component = new File(testProjectDir, "build/component.component")

        when:
        def repo = descrMgr.getDescriptorRepository()
        descrMgr.loadDescriptorFile(repo, component)

        then:
        component.exists()

        where:
        inputVersion | outputVersion | repoType
        "+"          | "3.0.0"       | RepositoryType.MAVEN
        "2.0.0"      | "2.0.0"       | RepositoryType.IVY
    }

    def createRepo(File repoDir) {

        copyResources("descriptors/component-1.component", "component-1.component")
        copyResources("descriptors/component-2.component", "component-2.component")
        copyResources("descriptors/component-3.component", "component-3.component")
        copyResources("descriptors/component-4.component", "component-4.component")

        new TestIvyRepoBuilder().repository( ivyPattern: RepositoryUtil.INTERSHOP_IVY_PATTERN, artifactPattern: RepositoryUtil.INTERSHOP_PATTERN ) {
            module(org: 'com.intershop.test', name: 'test', rev: '1.0.0') {
                artifact name: 'test', type: 'test', ext: 'zip', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testfiles/test1.file', content: 'test1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testconf/test2.conf', content: 'test2.conf'),
                        TestIvyRepoBuilder.ArchiveDirectoryEntry.newInstance(path: 'testmodule/empttestdir')
                ]
                artifact name: 'test', type: 'jar', ext: 'jar', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test1.file', content: 'interntest1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test2.file', content: 'interntest2.file')
                ]
                artifact name: 'test', type: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME, content: new File(tempProjectDir, "component-1.component")
            }
            module(org: 'com.intershop.test', name: 'test', rev: '2.0.0') {
                artifact name: 'test', type: 'test', ext: 'zip', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testfiles/test1.file', content: 'test1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testconf/test2.conf', content: 'test2.conf'),
                        TestIvyRepoBuilder.ArchiveDirectoryEntry.newInstance(path: 'testmodule/empttestdir')
                ]
                artifact name: 'test', type: 'jar', ext: 'jar', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test1.file', content: 'interntest1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test2.file', content: 'interntest2.file')
                ]
                artifact name: 'test', type: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME, content: new File(tempProjectDir, "component-2.component")
            }
            module(org: 'com.intershop.test', name: 'test', rev: '2.1.0') {
                artifact name: 'test', type: 'test', ext: 'zip', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testfiles/test1.file', content: 'test1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testconf/test2.conf', content: 'test2.conf'),
                        TestIvyRepoBuilder.ArchiveDirectoryEntry.newInstance(path: 'testmodule/empttestdir')
                ]
                artifact name: 'test', type: 'jar', ext: 'jar', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test1.file', content: 'interntest1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test2.file', content: 'interntest2.file')
                ]
                artifact name: 'test', type: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME, content: new File(tempProjectDir, "component-3.component")
            }
            module(org: 'com.intershop.test', name: 'test', rev: '2.0.1') {
                artifact name: 'test', type: 'test', ext: 'zip', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testfiles/test1.file', content: 'test1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testconf/test2.conf', content: 'test2.conf'),
                        TestIvyRepoBuilder.ArchiveDirectoryEntry.newInstance(path: 'testmodule/empttestdir')
                ]
                artifact name: 'test', type: 'jar', ext: 'jar', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test1.file', content: 'interntest1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test2.file', content: 'interntest2.file')
                ]
                artifact name: 'test', type: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME, content: new File(tempProjectDir, "component-4.component")
            }
        }.writeTo(repoDir)


        new TestMavenRepoBuilder().repository {
            project(groupId: 'com.intershop.test', artifactId: 'test', version: '1.0.0') {
                artifact entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test1.file', content: 'test1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test2.file', content: 'test2.file'),
                ]
                artifact classifier: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME, content: new File(tempProjectDir, "component-1.component")
            }
            project(groupId: 'com.intershop.test', artifactId: 'test', version: '2.2.0') {
                artifact entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test1.file', content: 'test1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test2.file', content: 'test2.file'),
                ]
                artifact classifier: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME, content: new File(tempProjectDir, "component-2.component")
            }
            project(groupId: 'com.intershop.test', artifactId: 'test', version: '3.0.0') {
                artifact entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test1.file', content: 'test1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test2.file', content: 'test2.file'),
                ]
                artifact classifier: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME, content: new File(tempProjectDir, "component-3.component")
            }
        }.writeTo(repoDir)

        File metadataDir = new File(repoDir, "com.intershop.test/test".replace(".", "/"))
        File metadataFile = new File(metadataDir, "maven-metadata.xml")
        metadataFile << """<?xml version="1.0" encoding="UTF-8"?>
        <metadata modelVersion="1.1.0">
          <groupId>com.intershop.test</groupId>
          <artifactId>test</artifactId>
          <version>3.0.0</version>
          <versioning>
            <latest>3.0.0</latest>
            <release>3.0.0</release>
            <versions>
              <version>1.0.0</version>
              <version>2.2.0</version>
              <version>3.0.0</version>
            </versions>
            <lastUpdated>20180308033824</lastUpdated>
          </versioning>
        </metadata>
        """.stripIndent()
    }

    private void copyResources(String srcDir, String target = '', File baseDir = tempProjectDir) {
        ClassLoader classLoader = getClass().getClassLoader();
        URL resource = classLoader.getResource(srcDir);
        if (resource == null) {
            throw new RuntimeException("Could not find classpath resource: $srcDir")
        }

        File resourceFile = new File(resource.toURI())
        if (resourceFile.file) {
            if(target) {
                FileUtils.copyFile(resourceFile, new File(baseDir, target))
            } else {
                FileUtils.copyFile(resourceFile, baseDir)
            }
        } else {
            if(target) {
                FileUtils.copyDirectory(resourceFile, new File(baseDir, target))
            } else {
                FileUtils.copyDirectory(resourceFile, baseDir)
            }
        }
    }
}
