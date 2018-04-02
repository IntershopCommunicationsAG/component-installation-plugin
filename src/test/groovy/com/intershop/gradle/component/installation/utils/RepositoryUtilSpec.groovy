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
import com.intershop.gradle.test.util.TestDir
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.apache.commons.io.FileUtils
import org.junit.Rule
import org.w3c.dom.Element
import spock.lang.Specification

class RepositoryUtilSpec extends Specification {

    @TestDir
    File testProjectDir

    @Rule
    public final MockWebServer server = new MockWebServer()

    def "test VersioningNodeFromMaven"() {
        when:
        copyResources("maven-metadata/versions.xml", "maven-metadata.xml")
        File metadata = new File(testProjectDir, "maven-metadata.xml")
        Element element = RepositoryUtil.getVersioningNodeFromMaven(metadata)

        then:
        element.getTagName() == "versioning"
    }

    def "test getLatestVersionFromMaven"() {
        when:
        copyResources("maven-metadata/versions.xml", "maven-metadata.xml")
        File metadata = new File(testProjectDir, "maven-metadata.xml")
        String version = RepositoryUtil.getLatestVersionFromMaven(metadata)

        then:
        version == "1.5.2"
    }

    def "test getLatestVersionFromMaven with pattern"() {
        when:
        copyResources("maven-metadata/versions.xml", "maven-metadata.xml")
        File metadata = new File(testProjectDir, "maven-metadata.xml")
        String version = RepositoryUtil.getLatestVersionFromMaven(metadata, "1\\.3\\..*")

        then:
        version == "1.3.2"
    }

    def "test getSnapshotVersionFromMaven"() {
        when:
        copyResources("maven-metadata/snapshot.xml", "maven-metadata.xml")
        File metadata = new File(testProjectDir, "maven-metadata.xml")
        String snapshotVersion = RepositoryUtil.getSnapshotVersionFromMaven(metadata, "1.6.0-SNAPSHOT")

        then:
        snapshotVersion == "1.6.0-20180312.090758-7"
    }

    def 'test ivy listing'() {
        when:
        copyResources("ivy-listing/listing.html", "index.html")
        File index = new File(testProjectDir, "index.html")
        String version = RepositoryUtil.getLatestVersionFromIvyIndex(index)

        then:
        version == "14.0.11"
    }

    def 'test ivy listing with pattern'() {
        when:
        copyResources("ivy-listing/listing.html", "index.html")
        File index = new File(testProjectDir, "index.html")
        String version = RepositoryUtil.getLatestVersionFromIvyIndex(index, "12\\.0\\..*")

        then:
        version == "12.0.39"
    }

    def 'test artifact simple path from maven'() {
        when:

        Repository repo = new Repository(RepoType.MAVEN_REMOTE, "http://repohost.test.com/repo", new Credentials("",""))
        Dependency dep = new Dependency("com.intershop.test", "test", "1.6.0")

        String version = RepositoryUtil.addMavenArtifactPath(dep, repo)

        then:
        repo.artifactPath == "http://repohost.test.com/repo/com/intershop/test/test/1.6.0/test-1.6.0"
        version == "1.6.0"
    }

    def 'test artifact snapshot path from maven'() {
        setup:
        String path = 'com/intershop/test/test/1.6.0-SNAPSHOT/maven-metadata.xml'
        String urlStr = server.url("mvnrepo/${path}").toString()
        String hostURL = urlStr - path
        copyResources("maven-metadata/snapshot.xml", "maven-metadata.xml")
        File metadata = new File(testProjectDir, "maven-metadata.xml")

        when:
        server.enqueue(new MockResponse().setBody(metadata.text))

        Repository repo = new Repository(RepoType.MAVEN_REMOTE, hostURL, new Credentials("",""))
        Dependency dep = new Dependency("com.intershop.test", "test", "1.6.0-SNAPSHOT")

        String version = RepositoryUtil.addMavenArtifactPath(dep, repo)

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

        when:
        server.enqueue(new MockResponse().setBody(metadata.text))

        Repository repo = new Repository(RepoType.MAVEN_REMOTE, hostURL, new Credentials("",""))
        Dependency dep = new Dependency("com.intershop.test", "test", "1.3.+")

        String version = RepositoryUtil.addMavenArtifactPath(dep, repo)

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

        when:
        server.enqueue(new MockResponse().setBody(metadata.text))

        Repository repo = new Repository(RepoType.MAVEN_REMOTE, hostURL, new Credentials("",""))
        Dependency dep = new Dependency("com.intershop.test", "test", "+")

        String version = RepositoryUtil.addMavenArtifactPath(dep, repo)

        then:
        repo.artifactPath == "${hostURL}com/intershop/test/test/1.5.2/test-1.5.2"
        version == "1.5.2"
    }

    def 'test ivy version from latest with pattern'() {
        setup:
        String path = 'com.intershop.test/test'
        String urlStr = server.url("ivyrepo/${path}").toString()
        String hostURL = urlStr - path
        copyResources("ivy-listing/listing.html", "index.html")
        File index = new File(testProjectDir, "index.html")

        when:
        server.enqueue(new MockResponse().setBody(index.text))

        Repository repo = new Repository(RepoType.IVY_REMOTE, hostURL, new Credentials("",""))
        repo.pattern = RepositoryUtil.INTERSHOP_PATTERN

        Dependency dep = new Dependency("com.intershop.test", "test", "12.0.+")

        String version = RepositoryUtil.getIvyVersion(dep, repo)

        then:
        version == "12.0.39"
    }

    def 'test ivy version from latest'() {
        setup:
        String path = 'com.intershop.test/test'
        String urlStr = server.url("ivyrepo/${path}").toString()
        String hostURL = urlStr - path
        copyResources("ivy-listing/listing.html", "index.html")
        File index = new File(testProjectDir, "index.html")

        when:
        server.enqueue(new MockResponse().setBody(index.text))

        Repository repo = new Repository(RepoType.IVY_REMOTE, hostURL, new Credentials("",""))
        repo.pattern = RepositoryUtil.INTERSHOP_PATTERN

        Dependency dep = new Dependency("com.intershop.test", "test", "+")

        String version = RepositoryUtil.getIvyVersion(dep, repo)

        then:
        version == "14.0.11"
    }

    def 'test ivy version'() {
        when:
        Repository repo = new Repository(RepoType.IVY_REMOTE, "http://test.com/ivy", new Credentials("",""))
        repo.pattern = RepositoryUtil.INTERSHOP_PATTERN

        Dependency dep = new Dependency("com.intershop.test", "test", "15.0.0")

        String version = RepositoryUtil.getIvyVersion(dep, repo)

        then:
        version == "15.0.0"
    }

    protected void copyResources(String srcDir, String target = '', File baseDir = testProjectDir) {
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
