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

import org.gradle.api.Action
import java.io.FilterReader
import java.io.Reader
import java.io.StringReader

/**
 * FullContent filter reader edits a file based on the full
 * content. The content is represented by a StringBuilder.
 *
 * @property reader class for reading character streams
 *
 * @constructor initializes an instances of a full content filter reader
 */
class FullContentFilterReader (reader: Reader) : FilterReader(DeferringReader(reader)) {

    init {
        (`in` as DeferringReader).parent = this
    }

    /**
     * Action to change the content of the file stored
     * in the StringBuilder instance.
     *
     * @property action changes the content
     */
    lateinit var action: Action<StringBuilder>

    /**
     * Changes the file source stream.
     *
     * @param source class for reading character streams
     */
    fun filterReader(source: Reader): Reader {
        val content = StringBuilder( source.readText() )
        action.execute(content)
        return StringReader(content.toString())
    }

    // the reader class to call the change method.
    internal class DeferringReader(private val source: Reader) : Reader() {

        lateinit var parent: FullContentFilterReader

        private
        var delegate: Reader? = null

        override fun read(cbuf: CharArray, off: Int, len: Int): Int {

            if (delegate == null) {
                delegate = parent.filterReader(source)
            }

            return delegate!!.read(cbuf, off, len)
        }

        override fun close() {
            source.close()
        }
    }
}
