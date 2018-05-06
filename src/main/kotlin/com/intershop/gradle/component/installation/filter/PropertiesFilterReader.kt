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
import java.io.StringWriter

/**
 * Properties filter reader edits a file based on the formated properties.
 * The content of all the file is represented by a formated properties instance.
 *
 * @property reader class for reading character streams
 *
 * @constructor initializes an instances of a properties filter reader
 */
class PropertiesFilterReader (reader: Reader) : FilterReader(DeferringReader(reader)) {

    init {
        (`in` as DeferringReader).parent = this
    }

    /**
     * Action to change the content of the file stored
     * in the FormatedPropertis instance.
     *
     * @property action changes the content
     */
    lateinit var action: Action<FormattedProperties>

    /**
     * Changes the file source stream.
     *
     * @param source class for reading character streams
     */
    fun filterReader(source: Reader): Reader {

        val properties: FormattedProperties = FormattedProperties()
        properties.load(source)

        action.execute(properties)

        val writer = StringWriter()
        properties.store(writer)

        return StringReader(writer.toString())
    }

    // the reader class to call the change method.
    internal class DeferringReader(private val source: Reader) : Reader() {

        lateinit var parent: PropertiesFilterReader

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
