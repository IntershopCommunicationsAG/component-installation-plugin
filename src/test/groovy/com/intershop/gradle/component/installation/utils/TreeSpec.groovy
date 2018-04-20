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

import com.intershop.gradle.test.util.TestDir
import spock.lang.Specification

class TreeSpec extends Specification {

    @TestDir
    File tempProjectDir

    def "test tree path output"()  {
        setup:
        def root = new TreeNode('root')
        def childList = ['child_1', 'child_2', 'child_3', 'child_4']
        def childList1 = ['child_1_1', 'child_1_2', 'child_1_3', 'child_1_4']
        def childList2 = ['child_2_1', 'child_2_2', 'child_2_3', 'child_2_4']
        def childList3 = ['child_3_1', 'child_3_2', 'child_3_3', 'child_3_4']
        def childList4 = ['child_4_1', 'child_2_2', 'child_4_3', 'child_4_4']

        def childList11 = ['child_1_1_1', 'child_1_1_2', 'child_1_1_3', 'child_1_1_4']
        def childList21 = ['child_2_1_1', 'child_2_1_2', 'child_2_1_3', 'child_2_1_4']

        childList.forEach {
            root.addChild(new TreeNode(it))
        }
        def child1 = root.children.find { it.name == 'child_1'}
        childList1.forEach {
            child1.addChild(new TreeNode(it))
        }
        def child2 = root.children.find { it.name == 'child_2'}
        childList2.forEach {
            child2.addChild(new TreeNode(it))
        }
        def child3 = root.children.find { it.name == 'child_3'}
        childList3.forEach {
            child3.addChild(new TreeNode(it))
        }
        def child4 = root.children.find { it.name == 'child_4'}
        childList4.forEach {
            child4.addChild(new TreeNode(it))
        }
        def child11 = child1.children.find { it.name == 'child_1_1'}
        childList11.forEach {
            child11.addChild(new TreeNode(it))
        }
        def child21 = child2.children.find { it.name == 'child_2_1'}
        childList21.forEach {
            child21.addChild(new TreeNode(it))
        }
        def child213 = child21.children.find { it.name == 'child_2_1_3'}

        when:
        def testPath = child213.getPath()

        then:
        testPath == "root/child_2/child_2_1/child_2_1_3"
    }

    def "test tree add path to root"()  {
        setup:
        def root = new TreeNode('root')
        
        when:
        def child = root.addPath("child1/child2/child3")
        
        then:
        child.getPath() == "root/child1/child2/child3"
    }

    def "test tree add paths to root"() {
        setup:
        def root = new TreeNode('root')

        when:
        def child = root.addPaths("child1", "", "child2/child3", "child4", "", "child5")
        def child1 = root.getChild("child1")

        then:
        child.getPath() == "root/child1/child2/child3/child4/child5"
        child1 != null
        child1.children.size() == 1
    }

    def "test tree add dublicates"() {
        setup:
        def root = new TreeNode('root')

        when:
        def child1 = root.addPath("child1/child2/child3")
        def child2 = root.addPath("child1/child2/child4")

        then:
        child1.getPath() == "root/child1/child2/child3"
        child2.getPath() == "root/child1/child2/child4"
    }

    def "test tree isChild and getChild"() {
        setup:
        def root = new TreeNode('root')
        def child1 = root.addPath("child1/child11/child111")
        def child2 = root.addPath("child1/child12/child121")
        def child3 = root.addPath("child2/child21/child211")

        when:
        def testchild = root.getChild("child1")

        then:
        testchild != null
        testchild.isChild("child12")
        ! testchild.isChild("child21")

    }

    def "test tree distinct"() {
        setup:
        def root = new TreeNode('root')
        root.addPath("child1/child11/child111")
        root.addPath("child2/child21/child211")
        root.addPath("child3/child21/child211")
        root.addPath("child4/child21/child211")

        def list = ["child3", "child1", "child6", "child2", "child5"]

        when:
        def delList = root.distinctNodes(list)

        then:
        ["child5", "child6"].containsAll(delList)
    }

    def "test tree distinct files and intersect files"() {
        setup:
        def rootdir = new File(tempProjectDir, "rootdir")
        rootdir.mkdirs()
        def file5 = new File(rootdir, "child5")
        file5.mkdirs()
        def file1 = new File(rootdir, "child1")
        file1.mkdirs()
        def file6 = new File(rootdir, "child6")
        file6.mkdirs()
        def file3 = new File(rootdir, "child3")
        file3.mkdirs()
        def file2 = new File(rootdir, "child2")
        file2.mkdirs()
        def file7 = new File(rootdir, "child7")
        file7.createNewFile()

        def root = new TreeNode('root')
        root.addPath("child1/child11/child111")
        root.addPath("child2/child21/child211")
        root.addPath("child3/child21/child211")
        root.addPath("child4/child21/child211")

        when:
        def delList = root.distinctFiles(rootdir)
        def nextList = root.intersectFiles(rootdir)

        then:
        delList.contains(file5)
        delList.contains(file6)
        delList.contains(file7)

        nextList.contains(file1)
        nextList.contains(file2)
        nextList.contains(file3)
    }
}
