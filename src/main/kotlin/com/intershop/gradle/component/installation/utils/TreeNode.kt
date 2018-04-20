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

import java.io.File

class TreeNode(val name: String) {

    var target:Boolean = false

    var parent: TreeNode? = null
    var children:MutableSet<TreeNode> = mutableSetOf()

    fun addChild(node: TreeNode) : TreeNode {
        var rnode: TreeNode? = children.find { it.name == node.name }

        if(rnode == null) {
            children.add(node)
            rnode = node
        }

        rnode.parent = this
        return rnode
    }

    fun addPath(path: String) : TreeNode {
        var node = this
        if(path.isNotBlank()) {
            path.split("/").forEach {
                node = node.addChild(TreeNode(it))
            }
        }
        return node
    }

    fun addPaths(vararg pathEntry: String): TreeNode {
        var node = this
        pathEntry.forEach {
            if(it.isNotBlank()) {
                node = node.addPath(it)
            }
        }
        return node
    }

    override fun toString(): String {
        return name
    }

    fun getPath(): String {
        val reversPath =  mutableListOf<String>()
        reversPath.add(name)
        var p = parent

        while(p != null) {
            reversPath.add(p.toString())
            p = p.parent
        }
        reversPath.reverse()
        return reversPath.joinToString("/")
    }

    fun isChild(childname: String): Boolean {
        return children.any { it.name == childname }
    }

    fun getChild(childname: String): TreeNode? {
        return children.find { it.name == childname }
    }

    fun intersectNodes(nodenames: List<String>): Collection<String> {
        return nodenames.intersect(children.map { it.name })
    }

    fun intersectFiles(rootDir: File): Collection<File> {
        val distList = intersectNodes(rootDir.listFiles()?.map { it.name } ?: mutableListOf())
        return rootDir.listFiles({ _, name -> distList.contains(name) })?.toList() ?: mutableListOf()
    }

    fun distinctNodes(nodenames: List<String>): Collection<String> {
        return nodenames.minus(nodenames.intersect(children.map { it.name }))
    }

    fun distinctFiles(rootDir: File) : Collection<File> {
        val distList = distinctNodes(rootDir.listFiles()?.mapNotNull { it.name } ?: mutableListOf())
        return rootDir.listFiles({ _, name -> distList.contains(name) })?.toList() ?: mutableListOf()
    }

    fun isTarget(dir: File): Boolean {
        val node = children.find { it.name == dir.name }
        return if(node != null) {
            node.target
        } else {
            false
        }
    }
}