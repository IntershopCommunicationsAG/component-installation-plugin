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

import groovy.util.XmlParser
import org.gradle.api.Action
import org.gradle.api.GradleException
import org.gradle.api.XmlProvider
import org.gradle.internal.xml.XmlTransformer
import java.io.FilterReader
import java.io.IOException
import java.io.Reader
import java.io.StringReader
import java.io.StringWriter

/**
 * XML filter reader edits a file based on the xml
 * content. The content is represented by a XmlProvider.
 *
 * @property reader class for reading character streams
 *
 * @constructor initializes an instances of a xml content filter reader
 */
class XMLFilterReader (reader: Reader) : FilterReader(DeferringReader(reader)) {

    init {
        (`in` as DeferringReader).parent = this
    }

    /**
     * Action to change the content of the file stored
     * in the XmlProvider instance.
     *
     * @property action changes the content
     */
    lateinit var action: Action<in XmlProvider>

    /**
     * Changes the file source stream.
     *
     * @param source class for reading character streams
     */
    fun filterReader(source: Reader): Reader {
        try {
            /*
             * Do not use namespace processing, as this moves namespace
             * declarations to the first usage of the namespace, bloating the
             * resulting output file.
             *
             * This setting will treat namespace declarations as normal
             * attributes, thereby preserving their positions.
             */
            val original = XmlParser(false, false).parse(source)
            val filtered = StringWriter()

            val trans = XmlTransformer()

            trans.addAction(action)

            trans.transform(original, filtered)
            return StringReader(filtered.toString())
        }
        catch(ex: IOException) {
            throw  GradleException("Unable to transform XML", ex)
        }
    }

    // the reader class to call the change method.
    internal class DeferringReader(private val source: Reader) : Reader() {

        lateinit var parent: XMLFilterReader

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
