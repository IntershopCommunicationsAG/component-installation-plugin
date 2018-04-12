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

import com.intershop.gradle.component.installation.utils.data.Credentials
import com.intershop.gradle.component.installation.utils.data.Dependency
import com.intershop.gradle.component.installation.utils.data.Repository
import com.intershop.gradle.component.installation.utils.data.RepositoryType
import com.intershop.gradle.test.builder.TestIvyRepoBuilder
import com.intershop.gradle.test.builder.TestMavenRepoBuilder
import com.intershop.gradle.test.util.TestDir
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.apache.commons.io.FileUtils
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.testfixtures.ProjectBuilder
import org.junit.Rule
import org.w3c.dom.Element
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

    @Rule
    public final MockWebServer server = new MockWebServer()

    private final Project project = ProjectBuilder.builder().build()

    private final Credentials emptyCredentials = new Credentials("","")
    private def pattern = [DescriptorManager.INTERSHOP_PATTERN, IvyArtifactRepository.GRADLE_ARTIFACT_PATTERN, IvyArtifactRepository.IVY_ARTIFACT_PATTERN] as Set<String>

    def setup() {
        File repoDir = new File(repoProjectDir, "repo")
        createRepo(repoDir)

        project.repositories {
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
                url "file://${repoDir.absolutePath.replace('\\\\', '/')}"
            }

        }
    }

    def "test VersioningNodeFromMaven"() {
        when:
        copyResources("maven-metadata/versions.xml", "maven-metadata.xml")
        File metadata = new File(testProjectDir, "maven-metadata.xml")
        Element element = DescriptorManager.getVersioningNodeFromMaven(metadata)

        then:
        element.getTagName() == "versioning"
    }

    def "test getLatestVersionFromMaven"() {
        when:
        copyResources("maven-metadata/versions.xml", "maven-metadata.xml")
        File metadata = new File(testProjectDir, "maven-metadata.xml")
        String version = DescriptorManager.getVersionFromMaven(metadata)

        then:
        version == "1.5.2"
    }

    def "test getLatestVersionFromMaven with pattern"() {
        when:
        copyResources("maven-metadata/versions.xml", "maven-metadata.xml")
        File metadata = new File(testProjectDir, "maven-metadata.xml")
        String version = DescriptorManager.getVersionFromMaven(metadata, "1\\.3\\..*")

        then:
        version == "1.3.2"
    }

    def "test getSnapshotVersionFromMaven"() {
        when:
        copyResources("maven-metadata/snapshot.xml", "maven-metadata.xml")
        File metadata = new File(testProjectDir, "maven-metadata.xml")
        String snapshotVersion = DescriptorManager.getSnapshotVersionFromMaven(metadata, "1.6.0-SNAPSHOT")

        then:
        snapshotVersion == "1.6.0-20180312.090758-7"
    }

    def 'test ivy listing'() {
        when:
        copyResources("ivy-listing/listing.html", "index.html")
        File index = new File(testProjectDir, "index.html")
        String version = DescriptorManager.getVersionFromIvyIndex(index)

        then:
        version == "14.0.11"
    }

    def 'test ivy listing with pattern'() {
        when:
        copyResources("ivy-listing/listing.html", "index.html")
        File index = new File(testProjectDir, "index.html")
        String version = DescriptorManager.getVersionFromIvyIndex(index, "12\\.0\\..*")

        then:
        version == "12.0.39"
    }

    def 'test artifact simple path from maven'() {
        setup:
        String path = 'com/intershop/test/test/maven-metadata.xml'
        String urlStr = server.url("mvnrepo/${path}").toString()
        String hostURL = urlStr - path
        copyResources("maven-metadata/versions.xml", "maven-metadata.xml")
        File metadata = new File(testProjectDir, "maven-metadata.xml")

        Repository repo = new Repository(RepositoryType.MAVEN, hostURL.toURI(), emptyCredentials)
        Dependency dependency = new Dependency("com.intershop.test", "test", "1.3.0")

        def descrMgr = new DescriptorManager((RepositoryHandler)project.getRepositories(), dependency, pattern)

        when:
        server.enqueue(new MockResponse().setBody(metadata.text))
        String version = descrMgr.addMavenArtifactPath(repo)

        then:
        repo.artifactPath == "${hostURL}com/intershop/test/test/1.3.0/test-1.3.0"
        version == "1.3.0"
    }

    def 'test artifact snapshot path from maven'() {
        setup:
        String path = 'com/intershop/test/test/1.6.0-SNAPSHOT/maven-metadata.xml'
        String urlStr = server.url("mvnrepo/${path}").toString()
        String hostURL = urlStr - path
        copyResources("maven-metadata/snapshot.xml", "2-maven-metadata.xml")
        copyResources("maven-metadata/versions-snapshots.xml", "1-maven-metadata.xml")
        File metadata1 = new File(testProjectDir, "1-maven-metadata.xml")
        File metadata2 = new File(testProjectDir, "2-maven-metadata.xml")

        Repository repo = new Repository(RepositoryType.MAVEN, hostURL.toURI(), emptyCredentials)
        Dependency dependency = new Dependency("com.intershop.test", "test", "1.6.0-SNAPSHOT")

        def descrMgr = new DescriptorManager((RepositoryHandler)project.getRepositories(), dependency, pattern)

        when:
        server.enqueue(new MockResponse().setBody(metadata1.text))
        server.enqueue(new MockResponse().setBody(metadata2.text))

        String version = descrMgr.addMavenArtifactPath(repo)

        then:
        repo.artifactPath == "${hostURL}com/intershop/test/test/1.6.0-SNAPSHOT/test-1.6.0-20180312.090758-7"
        version == "1.6.0-SNAPSHOT"
    }

    def 'test artifact path from latest with pattern'() {
        setup:
        String path = 'com/intershop/test/test/maven-metadata.xml'
        String urlStr = server.url("mvnrepo/${path}").toString()
        String hostURL = urlStr - path
        copyResources("maven-metadata/versions.xml", "maven-metadata.xml")
        File metadata = new File(testProjectDir, "maven-metadata.xml")

        Repository repo = new Repository(RepositoryType.MAVEN, hostURL.toURI(), emptyCredentials)
        Dependency dependency = new Dependency("com.intershop.test", "test", "1.3.+")

        def descrMgr = new DescriptorManager((RepositoryHandler)project.getRepositories(), dependency, pattern)

        when:
        server.enqueue(new MockResponse().setBody(metadata.text))

        String version = descrMgr.addMavenArtifactPath(repo)

        then:
        repo.artifactPath == "${hostURL}com/intershop/test/test/1.3.2/test-1.3.2"
        version == "1.3.2"
    }

    def 'test artifact path from latest'() {
        setup:
        String path = 'com/intershop/test/test/maven-metadata.xml'
        String urlStr = server.url("mvnrepo/${path}").toString()
        String hostURL = urlStr - path
        copyResources("maven-metadata/versions.xml", "maven-metadata.xml")
        File metadata = new File(testProjectDir, "maven-metadata.xml")

        Repository repo = new Repository(RepositoryType.MAVEN, hostURL.toURI(), emptyCredentials)
        Dependency dependency = new Dependency("com.intershop.test", "test", "+")

        def descrMgr = new DescriptorManager((RepositoryHandler)project.getRepositories(), dependency, pattern)

        when:
        server.enqueue(new MockResponse().setBody(metadata.text))

        String version = descrMgr.addMavenArtifactPath(repo)

        then:
        repo.artifactPath == "${hostURL}com/intershop/test/test/1.5.2/test-1.5.2"
        version == "1.5.2"
    }

    def 'test ivy version from latest - file url'() {
        setup:
        String path = 'com.intershop.test/test'
        String filePath = createIvyFileRepo(testProjectDir).absolutePath

        String urlStr =  "file://$filePath"
        Repository repo = new Repository(RepositoryType.IVY, urlStr.toURI(), emptyCredentials, DescriptorManager.INTERSHOP_PATTERN)
        Dependency dependency = new Dependency("com.intershop.test", "test", "+")

        def descrMgr = new DescriptorManager((RepositoryHandler)project.getRepositories(), dependency, pattern)

        when:
        String version = descrMgr.addIvyVersion(repo)

        then:
        version == "2.1.0"
    }

    def 'test ivy version from latest with pattern'() {
        setup:
        String path = 'com.intershop.test/test'
        String urlStr = server.url("ivyrepo/${path}").toString()
        String hostURL = urlStr - path
        copyResources("ivy-listing/listing.html", "index.html")
        File index = new File(testProjectDir, "index.html")

        Repository repo = new Repository(RepositoryType.IVY, hostURL.toURI(), emptyCredentials, DescriptorManager.INTERSHOP_PATTERN)
        Dependency dependency = new Dependency("com.intershop.test", "test", "12.0.+")

        def descrMgr = new DescriptorManager((RepositoryHandler)project.getRepositories(), dependency, pattern)

        when:
        server.enqueue(new MockResponse().setBody(index.text))

        String version = descrMgr.addIvyVersion(repo)

        then:
        version == "12.0.39"
    }

    def 'test ivy version from latest with pattern - file url'() {
        setup:
        String path = 'com.intershop.test/test'
        String filePath = createIvyFileRepo(testProjectDir).absolutePath

        String urlStr =  "file://$filePath"

        Repository repo = new Repository(RepositoryType.IVY, urlStr.toURI(), emptyCredentials, DescriptorManager.INTERSHOP_PATTERN)
        Dependency dependency = new Dependency("com.intershop.test", "test", "1.+")

        def descrMgr = new DescriptorManager((RepositoryHandler)project.getRepositories(), dependency, pattern)

        when:
        String version = descrMgr.addIvyVersion(repo)

        then:
        version == "1.3.0"
    }

    def 'test ivy version from latest'() {
        setup:
        String path = 'com.intershop.test/test'
        String urlStr = server.url("ivyrepo/${path}").toString()
        String hostURL = urlStr - path
        copyResources("ivy-listing/listing.html", "index.html")
        File index = new File(testProjectDir, "index.html")

        Repository repo = new Repository(RepositoryType.IVY, hostURL.toURI(), emptyCredentials, DescriptorManager.INTERSHOP_PATTERN)
        Dependency dependency = new Dependency("com.intershop.test", "test", "+")

        def descrMgr = new DescriptorManager((RepositoryHandler)project.getRepositories(), dependency, pattern)

        when:
        server.enqueue(new MockResponse().setBody(index.text))

        String version = descrMgr.addIvyVersion(repo)

        then:
        version == "14.0.11"
    }

    def 'test ivy version'() {
        setup:
        String path = 'com.intershop.test/test'
        String urlStr = server.url("ivyrepo/${path}").toString()
        String hostURL = urlStr - path
        copyResources("ivy-listing/listing.html", "index.html")
        File index = new File(testProjectDir, "index.html")

        Repository repo = new Repository(RepositoryType.IVY, hostURL.toURI(), emptyCredentials, DescriptorManager.INTERSHOP_PATTERN)
        Dependency dependency = new Dependency("com.intershop.test", "test", "11.1.11")

        def descrMgr = new DescriptorManager((RepositoryHandler)project.getRepositories(), dependency, pattern)

        when:
        server.enqueue(new MockResponse().setBody(index.text))

        String version = descrMgr.addIvyVersion(repo)

        then:
        version == "11.1.11"
    }

    @Unroll
    def 'Test repository handling - find version in repo'(){
        setup:
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

    private File createIvyFileRepo(File dir) {
        File repoDir = new File(dir, 'repo')

        new TestIvyRepoBuilder().repository( ivyPattern: DescriptorManager.INTERSHOP_IVY_PATTERN, artifactPattern: DescriptorManager.INTERSHOP_PATTERN ) {
            module(org: 'com.intershop.test', name: 'test', rev: '1.0.0') {
                artifact name: 'testmodule1', type: 'cartridge', ext: 'zip', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testfiles/test1.file', content: 'test1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testconf/test2.conf', content: 'test2.conf'),
                        TestIvyRepoBuilder.ArchiveDirectoryEntry.newInstance(path: 'testmodule/empttestdir')
                ]
                artifact name: 'testmodule1', type: 'jar', ext: 'jar', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test1.file', content: 'interntest1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test2.file', content: 'interntest2.file')
                ]
            }
            module(org: 'com.intershop.test', name: 'test', rev: '1.1.0') {
                artifact name: 'testmodule1', type: 'cartridge', ext: 'zip', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testfiles/test1.file', content: 'test1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testconf/test2.conf', content: 'test2.conf'),
                        TestIvyRepoBuilder.ArchiveDirectoryEntry.newInstance(path: 'testmodule/empttestdir')
                ]
                artifact name: 'testmodule1', type: 'jar', ext: 'jar', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test1.file', content: 'interntest1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test2.file', content: 'interntest2.file')
                ]
            }
            module(org: 'com.intershop.test', name: 'test', rev: '2.0.0') {
                artifact name: 'testmodule1', type: 'cartridge', ext: 'zip', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testfiles/test1.file', content: 'test1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testconf/test2.conf', content: 'test2.conf'),
                        TestIvyRepoBuilder.ArchiveDirectoryEntry.newInstance(path: 'testmodule/empttestdir')
                ]
                artifact name: 'testmodule1', type: 'jar', ext: 'jar', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test1.file', content: 'interntest1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test2.file', content: 'interntest2.file')
                ]
            }
            module(org: 'com.intershop.test', name: 'test', rev: '2.1.0') {
                artifact name: 'testmodule1', type: 'cartridge', ext: 'zip', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testfiles/test1.file', content: 'test1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testconf/test2.conf', content: 'test2.conf'),
                        TestIvyRepoBuilder.ArchiveDirectoryEntry.newInstance(path: 'testmodule/empttestdir')
                ]
                artifact name: 'testmodule1', type: 'jar', ext: 'jar', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test1.file', content: 'interntest1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test2.file', content: 'interntest2.file')
                ]
            }
            module(org: 'com.intershop.test', name: 'test', rev: '1.3.0') {
                artifact name: 'testmodule1', type: 'cartridge', ext: 'zip', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testfiles/test1.file', content: 'test1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'testmodule/testconf/test2.conf', content: 'test2.conf'),
                        TestIvyRepoBuilder.ArchiveDirectoryEntry.newInstance(path: 'testmodule/empttestdir')
                ]
                artifact name: 'testmodule1', type: 'jar', ext: 'jar', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test1.file', content: 'interntest1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/intern/test2.file', content: 'interntest2.file')
                ]
            }
        }.writeTo(repoDir)

        return repoDir
    }

    def createRepo(File repoDir) {
        copyResources("descriptors/component-1.component", "component-1.component")
        copyResources("descriptors/component-2.component", "component-2.component")
        copyResources("descriptors/component-3.component", "component-3.component")
        copyResources("descriptors/component-4.component", "component-4.component")

        new TestIvyRepoBuilder().repository( ivyPattern: DescriptorManager.INTERSHOP_IVY_PATTERN, artifactPattern: DescriptorManager.INTERSHOP_PATTERN ) {
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
                artifact name: 'test', type: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME,
                        content: replaceContent(new File(tempProjectDir, "component-1.component"), ['@group@': 'com.intershop.test', '@module@': 'test', '@version@': '1.0.0'])
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
                artifact name: 'test', type: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME,
                        content: replaceContent(new File(tempProjectDir, "component-2.component"), ['@group@': 'com.intershop.test', '@module@': 'test', '@version@': '2.0.0'])
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
                artifact name: 'test', type: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME,
                        content: replaceContent(new File(tempProjectDir, "component-3.component"), ['@group@': 'com.intershop.test', '@module@': 'test', '@version@': '2.1.0'])
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
                artifact name: 'test', type: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME,
                        content: replaceContent(new File(tempProjectDir, "component-4.component"), ['@group@': 'com.intershop.test', '@module@': 'test', '@version@': '2.0.1'])
            }
        }.writeTo(repoDir)


        new TestMavenRepoBuilder().repository {
            project(groupId: 'com.intershop.test', artifactId: 'test', version: '1.0.0') {
                artifact entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test1.file', content: 'test1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test2.file', content: 'test2.file'),
                ]
                artifact classifier: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME,
                        content: replaceContent(new File(tempProjectDir, "component-1.component"), ['@group@': 'com.intershop.test', '@module@': 'test', '@version@': '1.0.0'])
            }
            project(groupId: 'com.intershop.test', artifactId: 'test', version: '2.2.0') {
                artifact entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test1.file', content: 'test1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test2.file', content: 'test2.file'),
                ]
                artifact classifier: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME,
                        content: replaceContent(new File(tempProjectDir, "component-2.component"), ['@group@': 'com.intershop.test', '@module@': 'test', '@version@': '2.2.0'])
            }
            project(groupId: 'com.intershop.test', artifactId: 'test', version: '3.0.0') {
                artifact entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test1.file', content: 'test1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'com/class/test2.file', content: 'test2.file'),
                ]
                artifact classifier: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME,
                        content: replaceContent(new File(tempProjectDir, "component-3.component"), ['@group@': 'com.intershop.test', '@module@': 'test', '@version@': '3.0.0'])
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
        ClassLoader classLoader = getClass().getClassLoader()
        URL resource = classLoader.getResource(srcDir)
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
