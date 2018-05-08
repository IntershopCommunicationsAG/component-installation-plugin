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

import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.File
import java.io.FileReader
import java.io.FileWriter
import java.io.IOException
import java.io.Reader
import java.io.Writer
import java.util.*

/**
 * This class provides a properties class that also take comments into account.
 * E.g. Properties are commented without removing them directly.
 *
 * @constructor initialize an empty properties class
 */
class FormattedProperties(): Properties() {

    private val serialVersionUID = 1L

    private val delimiters = HashMap<String, Char>()

    companion object {
        /*
         * These are the hexadecimal digits.
         */
        private val hexDigit = charArrayOf('0', '1', '2', '3', '4', '5', '6', '7', '8',
                '9', 'A', 'B', 'C', 'D', 'E', 'F')

        /**
         * The equal sign character.
         */
        const val CHAR_EQUAL_SIGN = '='

        /**
         * The colon character.
         */
        const val CHAR_COLON = ':'
    }

    /**
     * The default delimiter character.
     */
    private var defaultCharacter: Char = CHAR_EQUAL_SIGN

    /**
     * This member holds the lines of the properties file.
     */
    private val lines = ArrayList<Line>()

    @Throws(IOException::class)
    constructor(propertiesFile: File): this() {
        FileReader(propertiesFile).use { r -> load(r) }
    }

    /**
     * This method checks whether the given line continues on the next one (line
     * continuation with "\\").
     *
     * @param aLine The line to be checked.
     *
     * @return <CODE>true</CODE> if the line continues on the next line.
     */
    private fun continueLine(aLine: String): Boolean {
        var i = 0

        var j = aLine.length - 1
        while (j >= 0 && aLine[j--] == '\\') {
            i++
        }

        return i % 2 == 1
    }

    /**
     * This method load this properties from the given input stream.
     *
     * @param originalReader The stream to read the properties from.
     *
     * @exception java.io.IOException if the stream could not be read.
     */
    @Synchronized
    @Throws(IOException::class)
    override fun load(originalReader: Reader) {
        // Build reader based on input stream.
        val reader = BufferedReader(originalReader)

        // Repeat till we're done.
        do {
            val c: Char

            // Get line.
            var line: String = reader.readLine() ?: return

            // No line - exit.
            // Is line empty?
            if (line.trim().isEmpty()) {
                // Yes, empty line.
                lines.add(EmptyLine())
            } else {
                // Not empty, get first character.
                c = line[0]

                // Is line commented out?
                if (c == '#' || c == '!') {
                    // Is a property included? (Only single Line properties will
                    // be supported.)

                    var delimiter: Char = CHAR_EQUAL_SIGN
                    var pos = line.indexOf(delimiter)
                    if (pos < 1) {
                        delimiter = CHAR_COLON
                        pos = line.indexOf(delimiter)
                    }
                    if (pos > 1) {
                        var startText = 1
                        var p = line[startText]
                        while ((p == '#' || c == '!') && startText < pos) {
                            startText++
                            p = line[startText]
                        }

                        val value = if (line.length > pos + 1) line.substring(pos + 1) else ""
                        val cpl = CommentPropertyLine(
                                    line.substring(0, startText).trim { it <= ' ' },
                                        loadConvert(line.substring(startText, pos).trim()),
                                        loadConvert(value.trim()), delimiter)
                        lines.add(cpl)
                    } else {
                        // Yes, comment line.
                        lines.add(CommentLine(line))
                    }
                } else {
                    // Property line.
                    var s1: String?
                    var s2: String

                    // Process line continuation.
                    while (continueLine(line)) {
                        s1 = reader.readLine() ?: ""
                        s2 = line.substring(0, line.length - 1)

                        var k = 0
                        while (k < s1.length) {
                            if (" \t\r\n".indexOf(s1[k]) == -1)
                                break
                            k++
                        }

                        s1 = s1.substring(k, s1.length)
                        line = s2 + s1
                    }

                    val i = line.length
                    var j = 0
                    while (j < i) {
                        if (" \t\r\n".indexOf(line[j]) == -1)
                            break
                        j++
                    }

                    var l: Int
                    l = j
                    while (l < i) {
                        val c1 = line[l]
                        if (c1 == '\\') {
                            l++
                            l++
                            continue
                        }
                        if ("=: \t\r\n".indexOf(c1) != -1)
                            break
                        l++
                    }

                    var i1: Int
                    i1 = l
                    while (i1 < i) {
                        if (" \t\r\n".indexOf(line[i1]) == -1) {
                            break
                        }
                        i1++
                    }

                    var delimiter: Char = CHAR_EQUAL_SIGN
                    if (i1 < i && '=' == line[i1]) {
                        i1++
                    } else if (i1 < i && ":".indexOf(line[i1]) != -1) {
                        i1++
                        delimiter = CHAR_COLON
                    }

                    while (i1 < i) {
                        if (" \t\r\n".indexOf(line[i1]) == -1) {
                            break
                        }
                        i1++
                    }

                    // Get key/value pair.
                    var key = line.substring(j, l)
                    var value = if (l >= i) "" else line.substring(i1, i)
                    key = loadConvert(key)
                    value = loadConvert(value)
                    if (super.containsKey(key)) {
                        for (sl in lines.indices) {
                            val pline = lines[sl]
                            if (pline is PropertyLine) {
                                if (pline.key == key) {
                                    lines[sl] = CommentPropertyLine("#", pline.key, pline.value ?: "", '"')
                                }
                            }
                        }
                    }
                    this[key] = value

                    // store delimiter of property
                    delimiters[key] = delimiter

                    defaultCharacter = delimiter
                }
            }

        } while (true)
    }

    /**
     * This method does the conversion of strings during loading of the
     * properties. This means replacing unicode literals by their original form.
     *
     * @param aString The string to be converted.
     *
     * @return The converted string.
     */
    @Suppress("MagicNumber")
    private fun loadConvert(aString: String): String {

        // Initialization.
        val length = aString.length
        val result = StringBuffer(length)

        // Process each character.
        var j = 0
        while (j < length) {
            // Get character at current position.
            var c = aString[j++]

            // Is it a backslash?
            if (c == '\\' && j + 1 < length) {
                // Get next character.
                c = aString[j++]

                // Unicode literal?
                if (c == 'u') {
                    var k = 0
                    for (l in 0..3) {
                        c = aString[j++]
                        // Depending on the character:
                        k = when (c) {
                            in '0'..'9' -> (k shl 4) + c.toInt() - '0'.toInt()
                            in 'a'..'f' -> (k shl 4) + 10 + c.toInt() - 'a'.toInt()
                            in 'A'..'F' -> (k shl 4) + 10 + c.toInt() - 'A'.toInt()
                            else -> throw IllegalArgumentException("Malformed \\uxxxx encoding.")
                        }
                    }

                    result.append(k.toChar())
                } else {
                    when (c) {
                        't' -> result.append('\t')
                        'r' -> result.append('\r')
                        'n' -> result.append('\n')
                        '\\' -> result.append('\\')
                        else -> result.append(c)
                    }
                }
            } else {
                result.append(c)
            }
        }
        return result.toString()
    }

    /**
     * This method sets a property with a given name to the given value.
     *
     * @param aKey The key of the property to set.
     *
     * @param aValue The value to set for the property.
     *
     * @return The previous value for the key.
     */
    @Synchronized
    fun put(aKey: String, aValue: String): Any? {
        // Call super.
        val result = super.put(aKey, aValue)

        var searchResult: PropertyLine? = null

        // Try to find a line that contains the key.
        for (i in lines.indices) {

            // Get Line.
            val line = lines[i]

            // Is it a property line?
            if (line is PropertyLine) {

                if (line.key == aKey) {
                    // Key found
                    line.value = aValue
                    if (line is CommentPropertyLine) {
                        if (searchResult == null && searchResult !is PropertyLine) {
                            searchResult = line
                        }
                    } else {
                        searchResult = line
                    }

                }
            }
        }

        if (searchResult != null && searchResult is CommentPropertyLine) {
            searchResult.comment = ""
        } else if (searchResult == null) {
            lines.add(PropertyLine(aKey, aValue))
        }
        // Return object returned from super.
        return result
    }

    /**
     * This method sets the property with the given key to the given value.
     *
     * @param aKey The key of the property to set.
     *
     * @param aValue The value to set for the property.
     *
     * @return The previous value for the key.
     */
    @Synchronized
    override fun setProperty(aKey: String, aValue: String): Any? {
        return put(aKey, aValue)
    }

    /**
     * Maps the specified <code>key</code> to the specified
     * <code>value</code> in this hashtable. Neither the key nor the
     * value can be <code>null</code>. <p>
     *
     * The value can be retrieved by calling the <code>get</code> method
     * with a key that is equal to the original key.
     *
     * @param      key     the hashtable key
     * @param      value   the value
     * @return     the previous value of the specified key in this hashtable,
     *             or <code>null</code> if it did not have one
     */
    override fun put(key: Any, value: Any?): Any? {
        return if (value != null)
            put(key.toString(), value.toString())
        else
            remove(key)
    }

    /**
     * This method sets a new property with the given key to the given value
     * with an comment.
     *
     * @param aKey The key of the property to set.
     *
     * @param aValue The value to set for the property.
     *
     * @param comments of comments.
     *
     * @return The previous value for the key.
     */
    @Synchronized
    fun setProperty(aKey: String, aValue: String, comments: Array<String>): Any? {
        val result = setProperty(aKey, aValue)
        addComment(aKey, aValue, comments, true)
        return result
    }

    /**
     * Add a commented property without a value.
     *
     * @param key The key of a property.
     * @param comment Comment of a property.
     */
    fun addComment(key: String, comment: Array<String>) {
        addComment(key, null, comment, false)
    }

    /**
     * Add a commented property with a value.
     *
     * @param key The key of a property.
     * @param value The value of the property. (It is also possible to set a null value.)
     * @param comment This string array is used for the comment.
     * @param changeValue The value of an existing property will be changed.
     */
    fun addComment(key: String, value: String?, comment: Array<String>, changeValue: Boolean) {
        var changed = false
        for (i in lines.indices) {
            changed = changed || changeLine(lines, key, value, i, comment, changeValue)
        }

        if (!changed) {
            for (j in comment.indices) {
                lines.add(CommentLine("# " + comment[j]))
            }

            lines.add(CommentPropertyLine("#", key, "", '='))
        }
    }

    private fun changeLine(linesVector: MutableList<Line>,
                           aKey: String,
                           aValue: String?,
                           pos: Int,
                           comment: Array<String>?,
                           changeValue: Boolean): Boolean {
        val line = linesVector[pos]

        // Is it a property line?
        if (line is PropertyLine) {
            if (line.key == aKey) {
                // Key found
                if (comment != null && comment.isNotEmpty()) {
                    var addComment = true
                    if (pos - comment.size > 0) {
                        for (i in pos - comment.size until pos) {
                            val cl = linesVector[i]
                            if (cl is CommentLine) {
                                addComment = addComment && cl.comment != "# " + comment[i - pos + comment.size]
                            }
                        }
                    }
                    if (addComment) {
                        for (i in comment.indices) {
                            linesVector.add(pos + i, CommentLine("# " + comment[i]))
                        }
                    }
                }
                if (changeValue) {
                    line.value = aValue
                }
                return true
            }
        }
        return false
    }

    /**
     * This method save this properties to the given output stream.
     *
     * @param outputFile The file to save the properties to.
     *
     * @exception java.io.IOException if the stream could not be written.
     */
    @Throws(IOException::class)
    fun store(outputFile: File) {
        FileWriter(outputFile).use { w -> store(w) }
    }

    /**
     * This method save this properties to the given output stream.
     *
     * @param originalWriter The writer to save the properties to.
     *
     * @param aHeader A string to be written into the header.
     *
     * @exception java.io.IOException if the stream could not be written.
     */
    @Synchronized
    @Throws(IOException::class)
    fun store(originalWriter: Writer, vararg aHeader: String) {
        val writer = BufferedWriter(originalWriter)

        // Do we have a header to write?
        if (aHeader.isNotEmpty()) {
            var pos = -1
            for (i in lines.indices) {
                if (lines[i] is CommentLine) {
                    val cl = lines[i]
                    if (cl is CommentLine && cl.comment == "# " + aHeader[0]) {
                        pos = i
                    }
                    break
                }
            }

            if (pos < 0) {
                writeln(writer, "# " + aHeader[0])
                writeln(writer, "# " + Date().toString())
                writeln(writer, "")
            } else {
                lines.add(pos + 1, CommentLine("# " + Date().toString()))
            }
        }

        // Process each line.
        for (i in lines.indices) {
            // Get the particular line.
            val line = lines[i]
            writeLine(writer, line)
        }

        writer.close()
    }

    @Throws(IOException::class)
    private fun writeLine(writer: BufferedWriter, line: Line) {
        // Depending on the type of the line.
        when (line) {
            is CommentPropertyLine -> {
                val buf = StringBuffer()
                if (line.comment.trim() != "") {
                    buf.append(line.comment.trim()).append(" ")
                }
                buf.append(storeConvert(line.key)).append(line.delimiter).append(storeConvert(line.value ?: ""))
                writeln(writer, buf.toString())
            }
            is PropertyLine -> {

                var delimiter: Char? = delimiters[line.key]
                if (delimiter == null) {
                    delimiter = defaultCharacter
                }

                val buf = StringBuilder(storeConvert(line.key))
                buf.append(" ").append(delimiter).append(" ")
                buf.append(storeConvert(line.value ?: ""))

                writeln(writer, buf.toString())
            }
            is CommentLine -> writeln(writer, line.comment)
            is EmptyLine -> writeln(writer, "")
        }
    }

    /**
     * This method does the conversion of strings during storing of the
     * properties. This means replacing special characters with unicode
     * literals.
     *
     * @param aString The string to be converted.
     *
     * @return The converted string.
     */
    @Suppress("MagicNumber")
    private fun storeConvert(aString: String): String {
        // Initialization.
        val length = aString.length
        val result = StringBuffer(length * 2)

        // For each character...
        var j = 0
        while (j < length) {
            val c = aString[j++]

            // Depending on the character type:
            when (c) {
                '\\' -> result.append('\\')
                '\t' -> result.append('\t')
                '\n' -> result.append('\n')
                '\r' -> result.append('\r')
                else -> {
                    if (c < '\u0014' || c > '\u007f') {
                        result.append('\\')
                        result.append('u')
                        result.append(toHex(c.toInt() shr 12 and 0xf))
                        result.append(toHex(c.toInt() shr 8 and 0xf))
                        result.append(toHex(c.toInt() shr 4 and 0xf))
                        result.append(toHex(c.toInt() and 0xf))
                    }
                    if ("\t\r\n".indexOf(c) != -1) {
                        result.append('\\')
                    }
                    result.append(c)
                }
            }
        }

        return result.toString()
    }

    /**
     * Removes the mapping specified by the given key from the property set.
     *
     * Note: Comment lines belonging to a property will not be deleted.
     *
     * @param key The key of the property to remove.
     *
     * @return The value for the key.
     */
    @Synchronized
    override fun remove(key: Any): Any? {
        val value = super.remove(key)
        if (value != null) {
            // Try to find a line that contains the key.
            for (i in lines.indices) {
                // Get Line.
                val line = lines[i]

                // Is it a property line?
                if (line is PropertyLine) {
                    if (line.key == key) {
                        val delimiter: Char = delimiters[line.key] ?: defaultCharacter

                        // Key found -> remove the corresponding line
                        lines[i] = CommentPropertyLine("#", line.key, line.value ?: "", delimiter)
                    }
                }
            }
        }

        return value
    }

    /**
     * Removes the mapping specified by the given key from the property set.
     *
     * Note: Comment lines belonging to a property will not be deleted.
     *
     * @param aKey The key of the property to remove.
     * @param comment This string array is used for the comment.
     *
     * @return The value for the key.
     */
    @Synchronized
    fun remove(aKey: Any, comment: Array<String>): Any? {
        if (super.containsKey(aKey)) {
            addComment(aKey.toString(), comment)
        }

        return remove(aKey)
    }

    /**
     * This helper method converts a given integer to a hex digit.
     *
     * @param anInt The integer to convert.
     *
     * @return The hex digit for the given integer.
     */
    private fun toHex(anInt: Int): Char {
        return hexDigit[anInt and 0xf]
    }

    /**
     * This method writes the given string to the given writer, followed by a
     * newline.
     *
     * @param aWriter The writer to write to.
     *
     * @param aString The string to be written.
     */
    @Throws(IOException::class)
    private fun writeln(aWriter: BufferedWriter, aString: String) {
        aWriter.write(aString)
        aWriter.newLine()
    }

    /**
     * This inner class represents a line in a properties file.
     */
    internal abstract inner class Line

    /**
     * This inner class represents an empty line in a properties file.
     */
    internal open inner class EmptyLine : Line() {
        override fun equals(other: Any?): Boolean = if(other is EmptyLine) true else false
        override fun hashCode(): Int = javaClass.hashCode()
    }

    /**
     * This inner class represents a comment line in a properties file.
     */
    internal open inner class CommentLine(var comment: String) : Line() {

        override fun equals(other: Any?): Boolean {
            if (other != null && other is CommentLine) {
                if (comment == other.comment) {
                    return true
                }
            }
            return false
        }

        override fun hashCode(): Int {
            return comment.hashCode()
        }
    }

    /**
     * This inner class represents a property line in a properties file.
     */
    internal open inner class PropertyLine(val key: String, var value: String?) : Line() {

        override fun equals(other: Any?): Boolean {
            if (other != null && other is PropertyLine) {
                if (key == other.key && value == other.value) {
                    return true
                }
            }
            return false
        }

        @Suppress("MagicNumber")
        override fun hashCode(): Int {
            var result = key.hashCode()
            result = 31 * result + (value?.hashCode() ?: 0)
            return result
        }
    }

    /**
     * This inner class represents an comment line with a property.
     */
    internal inner class CommentPropertyLine(var comment: String,
                                             private val aKey: String,
                                             private var aValue: String,
                                             val delimiter: Char) : PropertyLine(aKey, aValue) {

        override fun equals(other: Any?): Boolean {
            if (other != null && other is CommentPropertyLine) {
                if (comment == other.comment && aKey == other.key && aValue == other.value) {
                    return true
                }
            }
            return false
        }

        @Suppress("MagicNumber")
        override fun hashCode(): Int {
            var result = comment.hashCode()
            result = 31 * result + aKey.hashCode()
            result = 31 * result + aValue.hashCode()
            result = 31 * result + delimiter.hashCode()
            return result
        }
    }
}
