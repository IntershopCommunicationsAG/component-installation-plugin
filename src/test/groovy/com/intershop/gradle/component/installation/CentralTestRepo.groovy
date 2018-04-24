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

import com.intershop.gradle.component.installation.utils.DescriptorManager
import com.intershop.gradle.test.builder.TestIvyRepoBuilder
import com.intershop.gradle.test.builder.TestMavenRepoBuilder
import org.apache.commons.io.FileUtils

class CentralTestRepo {

    private File repoDir

    private String intRepoconfig

    CentralTestRepo(String path) {
        repoDir = new File(path)

        if(repoDir.exists()) {
            repoDir.deleteDir()
            repoDir.mkdirs()
        }
    }

    String getRepoConfig() {
        if(! intRepoconfig) {
            intRepoconfig = createRepo(repoDir)
        }
        return intRepoconfig
    }

    private String createRepo(File dir) {

        dir.deleteDir()
        dir.mkdirs()

        File repoDir = new File(dir, 'repo')

        copyResources("descriptors/component-1.component", "component-1.component", dir)
        copyResources("descriptors/component-1.1.component", "component-1.1.component", dir)
        copyResources("descriptors/component-1.2.component", "component-1.2.component", dir)
        copyResources("descriptors/component-1.3.component", "component-1.3.component", dir)
        copyResources("descriptors/component-1.4.component", "component-1.4.component", dir)
        copyResources("descriptors/component-1.5.component", "component-1.5.component", dir)
        copyResources("descriptors/component-A.component", "component-A.component", dir)
        copyResources("descriptors/component-B.component", "component-B.component", dir)

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
                        content: replaceContent(new File(dir, "component-1.component"), ['@group@': 'com.intershop.test', '@module@': 'testcomponent', '@version@': '1.0.0'], dir)
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
                        content: replaceContent(new File(dir, "component-1.1.component"), ['@group@': 'com.intershop.test', '@module@': 'testcomponent', '@version@': '1.1.0'], dir)
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
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'share/system/config/test1.properties', content: 'changed --- fromZip.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'share/sites/org1/.switch', content: '1'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'share/sites/org2/.switch', content: '1')
                ]
                artifact name: 'test1', type: 'properties', ext: 'properties', content: 'property1 = value1'
                artifact name: 'test2', type: 'properties', ext: 'properties', classifier: 'linux', content: 'property2 = value2'
            }
            module(org: 'com.intershop.test', name: 'testcomponent', rev: '1.2.0') {
                artifact name: 'testcomponent', type: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME,
                        content: replaceContent(new File(dir, "component-1.2.component"), ['@group@': 'com.intershop.test', '@module@': 'testcomponent', '@version@': '1.2.0'], dir)
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
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'share/system/config/test1.properties', content: 'changed --- fromZip.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'share/sites/org1/.switch', content: '1'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'share/sites/org2/.switch', content: '1')
                ]
                artifact name: 'test1', type: 'properties', ext: 'properties', content: 'property1 = value1'
                artifact name: 'test2', type: 'properties', ext: 'properties', classifier: 'linux', content: 'property2 = value2'
            }
            module(org: 'com.intershop.test', name: 'testcomponent', rev: '1.3.0') {
                artifact name: 'testcomponent', type: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME,
                        content: replaceContent(new File(dir, "component-1.3.component"), ['@group@': 'com.intershop.test', '@module@': 'testcomponent', '@version@': '1.3.0'], dir)
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
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'share/system/config/test1.properties', content: 'changed --- fromZip.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'share/sites/org1/.switch', content: '1'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'share/sites/org2/.switch', content: '1')
                ]
                artifact name: 'test1', type: 'properties', ext: 'properties', content: 'property1 = value1'
                artifact name: 'test2', type: 'properties', ext: 'properties', classifier: 'linux', content: 'property2 = value2'
            }
            module(org: 'com.intershop.test', name: 'testcomponent', rev: '1.4.0') {
                artifact name: 'testcomponent', type: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME,
                        content: replaceContent(new File(dir, "component-1.4.component"), ['@group@': 'com.intershop.test', '@module@': 'testcomponent', '@version@': '1.4.0'], dir)
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
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'share/system/config/test1.properties', content: 'changed --- fromZip.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'share/sites/org1/.switch', content: '1'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'share/sites/org2/.switch', content: '1')
                ]
                artifact name: 'test1', type: 'properties', ext: 'properties', content: 'property1 = value1'
                artifact name: 'test2', type: 'properties', ext: 'properties', classifier: 'linux', content: 'property2 = value2'
            }
            module(org: 'com.intershop.test', name: 'testcomponent', rev: '1.5.0') {
                artifact name: 'testcomponent', type: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME,
                        content: replaceContent(new File(dir, "component-1.5.component"), ['@group@': 'com.intershop.test', '@module@': 'testcomponent', '@version@': '1.5.0'], dir)
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
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'share/system/config/test1.properties', content: 'changed --- fromZip.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'share/sites/org1/.switch', content: '1'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'share/sites/org2/.switch', content: '1')
                ]
                artifact name: 'test1', type: 'properties', ext: 'properties', content: 'property1 = value1'
                artifact name: 'test2', type: 'properties', ext: 'properties', classifier: 'linux', content: 'property2 = value2'
            }
            module(org: 'com.intershop.test', name: 'testcomponentA', rev: '1.0.0') {
                artifact name: 'testcomponentA', type: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME,
                        content: replaceContent(new File(dir, "component-A.component"), ['@group@': 'com.intershop.test', '@module@': 'testcomponentA', '@version@': '1.0.0'], dir)
                artifact name: 'startscripts', type: 'bin', ext: 'zip', classifier: 'linux', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'bin/startscriptA1.sh', content: 'interntestA1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'bin/startscriptA2.sh', content: 'interntestA2.file')
                ]
                artifact name: 'startscripts', type: 'bin', ext: 'zip', classifier: 'macos', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'bin/startscriptA1.sh', content: 'interntestA1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'bin/startscriptA2.sh', content: 'interntestA2.file')
                ]
                artifact name: 'startscripts', type: 'bin', ext: 'zip', classifier: 'win', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'bin/startscriptA1.bat', content: 'interntestA1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'bin/startscriptA2.bat', content: 'interntestA2.file')
                ]}
            module(org: 'com.intershop.test', name: 'testcomponentB', rev: '1.0.0') {
                artifact name: 'testcomponentB', type: DescriptorManager.DESCRIPTOR_NAME, ext: DescriptorManager.DESCRIPTOR_NAME,
                        content: replaceContent(new File(dir, "component-B.component"), ['@group@': 'com.intershop.test', '@module@': 'testcomponentB', '@version@': '1.0.0'], dir)
                artifact name: 'startscripts', type: 'bin', ext: 'zip', classifier: 'linux', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'bin/startscriptB1.sh', content: 'interntestB1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'bin/startscriptB2.sh', content: 'interntestB2.file')
                ]
                artifact name: 'startscripts', type: 'bin', ext: 'zip', classifier: 'macos', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'bin/startscriptB1.sh', content: 'interntestB1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'bin/startscriptB2.sh', content: 'interntestB2.file')
                ]
                artifact name: 'startscripts', type: 'bin', ext: 'zip', classifier: 'win', entries: [
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'bin/startscriptB1.bat', content: 'interntestB1.file'),
                        TestIvyRepoBuilder.ArchiveFileEntry.newInstance(path: 'bin/startscriptB2.bat', content: 'interntestB2.file')
                ]}

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

        return repostr
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

    private void copyResources(String srcDir, String target = '', File baseDir = testProjectDir) {
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

