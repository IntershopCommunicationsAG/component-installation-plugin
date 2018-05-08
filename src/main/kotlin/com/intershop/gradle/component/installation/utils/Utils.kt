/*
 * Copyright 2018 Intershop Communications AG.
 *
 * Licensed under the Apache License, VersionComparator 2.0 (the "License");
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

import org.gradle.api.file.ContentFilterable
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import java.io.File
import java.io.FilterReader
import java.io.InputStream
import kotlin.reflect.KProperty

/**
 * Provides 'set' functional extension for the Property object.
 */
operator fun <T> Property<T>.setValue(receiver: Any?, property: KProperty<*>, value: T) = set(value)
/**
 * Provides 'get' functional extension for the Property object.
 */
operator fun <T> Property<T>.getValue(receiver: Any?, property: KProperty<*>): T = get()

/**
 * Provides 'set' functional extension for the SetProperty object.
 */
operator fun <T> SetProperty<T>.setValue(receiver: Any?, property: KProperty<*>, value: Set<T>) = set(value)
/**
 * Provides 'get' functional extension for the SetProperty object.
 */
operator fun <T> SetProperty<T>.getValue(receiver: Any?, property: KProperty<*>): Set<T> = get()

/**
 * Provides functional extension for primitve objects.
 */
inline fun <reified T> ObjectFactory.property(): Property<T> = property(T::class.java)

/**
 * Extends File with an method to copy a stream to the file.
 */
fun File.copyInputStreamToFile(inputStream: InputStream) {
    inputStream.use { input ->
        this.outputStream().use { fileOut ->
            input.copyTo(fileOut)
        }
    }
}

/**
 * Adds a content filter to be used during the copy.
 * Multiple calls add additional filters to the filter chain.
 * Each filter should implement [FilterReader].
 * Import `org.apache.tools.ant.filters.*` for access to all the standard Ant filters.
 *
 * Examples:
 *
 * ```
 * filter<StripJavaComments>()
 * filter<com.mycompany.project.CustomFilter>()
 * filter<HeadFilter>("lines" to 25, "skip" to 2)
 * filter<ReplaceTokens>("tokens" to mapOf("copyright" to "2009", "version" to "2.3.1"))
 * ```
 *
 * @param T type of the filter to add
 * @param properties map of filter properties
 * @return this
 */
inline
fun <reified T : FilterReader> ContentFilterable.filter(vararg properties: Pair<String, Any?>): ContentFilterable =
        if (properties.isEmpty()) filter(T::class.java)
        else filter(mapOf(*properties), T::class.java)

