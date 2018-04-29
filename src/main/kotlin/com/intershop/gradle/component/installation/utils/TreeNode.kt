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

/**
 * An implementation for a tree data to configure
 * the clean up task.
 *
 * @param name name of the tree node
 */
class TreeNode(val name: String) {

    /**
     * If this value is true the node is a configured target item.
     *
     * @property target true if the tree node a configured a target.
     */
    var target:Boolean = false

    /**
     * The parent node of this node.
     *
     * @property parent the parent object.
     */
    var parent: TreeNode? = null

    /**
     * Set of child objects of this tree node.
     *
     * @property children set of child tree nodes
     */
    var children:MutableSet<TreeNode> = mutableSetOf()

    /**
     * Add a child node to the current node, if the
     * node does not exists in the list.
     *
     * @param node tree node instance
     *
     * @return the node that was added or the available node
     */
    fun addChild(node: TreeNode) : TreeNode {
        var rnode: TreeNode? = children.find { it.name == node.name }

        if(rnode == null) {
            children.add(node)
            rnode = node
        }

        rnode.parent = this
        return rnode
    }

    /**
     * Add path to the current node. The path seperator is a slash.
     * If path is an empty string, no new node is added.
     *
     * @param path string with slashes
     *
     * @return the latest added node
     */
    fun addPath(path: String) : TreeNode {
        var node = this
        if(path.isNotBlank()) {
            path.split("/").forEach {
                node = node.addChild(TreeNode(it))
            }
        }
        return node
    }

    /**
     * Add path elements to the current node.
     *
     * @param pathEntry a list of path elements.
     *
     * @return the latest added node.
     */
    fun addPaths(vararg pathEntry: String): TreeNode {
        var node = this
        pathEntry.forEach {
            if(it.isNotBlank()) {
                node = node.addPath(it)
            }
        }
        return node
    }

    /**
     * Returns the name of the node.
     *
     * @return the string representation of the node
     */
    override fun toString(): String {
        return name
    }

    /**
     * Returns the path of a this node.
     *
     * @return path of the current node.
     */
    fun getPath(): String {
        val reversPath =  mutableListOf<String>()
        reversPath.add(name)
        var intParent = parent

        while(intParent != null) {
            reversPath.add(intParent.toString())
            intParent = intParent.parent
        }
        reversPath.reverse()
        return reversPath.joinToString("/")
    }

    /**
     * Verify if the name a child of the current node.
     *
     * @param childname the name of the searched child
     *
     * @return true if the name is a child
     */
    fun isChild(childname: String): Boolean {
        return children.any { it.name == childname }
    }

    /**
     * Returns the tree node with the given child
     * name or null, if the child name is not available.
     *
     * @param childname name of the searched child
     *
     * @return the node or null
     */
    fun getChild(childname: String): TreeNode? {
        return children.find { it.name == childname }
    }

    /**
     * Compares the current child set with a
     * list of node names. Nodes will be returned
     * that are in booth list.
     *
     * @param nodenames a list of nodes
     *
     * @return a collection of the node names
     */
    fun intersectNodes(nodenames: List<String>): Collection<String> {
        return nodenames.intersect(children.map { it.name })
    }

    /**
     * Compares the current child set with a
     * list of node directories. Directories
     * will be returned that are in booth list.
     *
     * @param rootDir root directory
     *
     * @return a collection of files and directories
     */
    fun intersectFiles(rootDir: File): Collection<File> {
        val distList = intersectNodes(rootDir.listFiles()?.map { it.name } ?: mutableListOf())
        return rootDir.listFiles({ _, name -> distList.contains(name) })?.toList() ?: mutableListOf()
    }

    /**
     * Compares the current child set with a
     * list of node names. Nodes will be returned
     * that are not in the current list.
     *
     * @param nodenames a list of nodes
     *
     * @return a collection of the node names
     */
    fun distinctNodes(nodenames: List<String>): Collection<String> {
        return nodenames.minus(nodenames.intersect(children.map { it.name }))
    }

    /**
     * Compares the current child set with a
     * list of node directories. Directories
     * will be returned that are not in the list.
     *
     * @param rootDir root directory
     *
     * @return a collection of files and directories
     */
    fun distinctFiles(rootDir: File) : Collection<File> {
        val distList = distinctNodes(rootDir.listFiles()?.mapNotNull { it.name } ?: mutableListOf())
        return rootDir.listFiles({ _, name -> distList.contains(name) })?.toList() ?: mutableListOf()
    }

    /**
     * Returns true if the directory is
     * a configured target.
     *
     * @param dir child directory
     */
    fun isTarget(dir: File): Boolean {
        val node = children.find { it.name == dir.name }
        return node?.target ?: false
    }
}
