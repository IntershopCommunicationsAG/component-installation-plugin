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

package com.intershop.gradle.component.installation.tasks

import org.gradle.api.file.FileTreeElement
import org.gradle.api.file.FileVisitDetails
import org.gradle.api.file.FileVisitor
import org.gradle.api.file.RelativePath
import org.gradle.api.internal.file.collections.DirectoryFileTreeFactory
import org.gradle.api.internal.file.copy.CopyAction
import org.gradle.api.internal.file.copy.CopyActionProcessingStream
import org.gradle.api.specs.Spec
import org.gradle.api.tasks.WorkResult
import org.gradle.api.tasks.WorkResults
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import org.gradle.util.GFileUtils
import java.io.File
import java.util.*

class SyncCopyActionDecorator(val baseDestDir: File,
                              val delegate: CopyAction,
                              val preserveSpec: PatternFilterable,
                              val directoryFileTreeFactory: DirectoryFileTreeFactory,
                              val defaultTime: Long) : CopyAction {

    override fun execute(stream: CopyActionProcessingStream) : WorkResult {
        val visited = HashMap<RelativePath,Long>()

        val didWork = delegate.execute { streamAction ->
            stream.process { details ->
                visited[details.relativePath] = details.lastModified
                streamAction.processFile(details)
            }
        }

        val fileVisitor = SyncCopyActionDecoratorFileVisitor(visited, preserveSpec, defaultTime)

        val walker = directoryFileTreeFactory.create(baseDestDir).postfix()
        walker.visit(fileVisitor)
        visited.clear()

        return WorkResults.didWork(didWork.didWork || fileVisitor.didWork)
    }

    private class SyncCopyActionDecoratorFileVisitor(val visited: Map<RelativePath, Long>,
                                                     preserveSpec: PatternFilterable?,
                                                     val defaultTime: Long) : FileVisitor {

        var didWork = false
        val preserveSet = PatternSet()
        var preserveSpec: Spec<FileTreeElement>

        init {
            if(preserveSpec != null)  {
                preserveSet.include(preserveSpec.includes)
                preserveSet.exclude(preserveSpec.excludes)
            }
            this.preserveSpec = preserveSet.asSpec
        }

        override fun visitDir(dirDetails: FileVisitDetails) {
            maybeDelete(dirDetails, true);
        }

        override fun visitFile(fileDetails: FileVisitDetails) {
            maybeDelete(fileDetails, false);
        }

        private fun maybeDelete(fileDetails: FileVisitDetails, isDir: Boolean) {
            val path = fileDetails.relativePath

            if (!visited.keys.contains(path)) {
                if (preserveSet.isEmpty() || !this.preserveSpec.isSatisfiedBy(fileDetails)) {
                    if (isDir) {
                        GFileUtils.deleteDirectory(fileDetails.file)
                    } else {
                        GFileUtils.deleteQuietly(fileDetails.file)
                    }
                }
            } else {
                fileDetails.file.setLastModified(visited[path] ?: defaultTime)
            }

            didWork = true
        }
    }

}