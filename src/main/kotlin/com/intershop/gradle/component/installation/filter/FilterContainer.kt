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

import com.intershop.gradle.component.installation.tasks.InstallTask
import com.intershop.gradle.component.installation.utils.filter
import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.Namer
import org.gradle.api.Project
import org.gradle.api.Transformer
import org.gradle.api.XmlProvider
import org.gradle.api.internal.ClosureBackedAction
import org.gradle.api.internal.DefaultNamedDomainObjectList
import org.gradle.api.tasks.util.PatternFilterable
import org.gradle.api.tasks.util.PatternSet
import org.gradle.internal.reflect.Instantiator
import java.util.*
import javax.inject.Inject

/**
 * This container is used for the definition of all filters. The name of the filter must be unique
 * and the order of specified filters must be always the same.
 *
 * @property instantiator an object that can create new instances of a given type, which may be decorated
 * in some fashion.
 *
 * @constructor creates an container for filter declarations.
 */
class FilterContainer @Inject constructor(private val project: Project,  instantiator: Instantiator) :
        DefaultNamedDomainObjectList<FilterSpec>(FilterSpec::class.java, instantiator, FilterSpecNamer()) {

    // Action helper method for initialization.
    private val addLastAction = Action<FilterSpec> { super@FilterContainer.add(it) }

    // Helper method to check for names
    private fun addFilterSpec(filterspec: FilterSpec): FilterSpec {
        assertCanAdd(filterspec.name)
        addLastAction.execute(filterspec)
        return filterspec
    }

    /**
     * Adds an 'OverrideProperties' filter to all installation tasks. The configuration
     * is based on formatted properties.
     *
     * @param name the name of the filter configuration.
     * @param include file filter for included files in ANT file filter style.
     * @param properties Closure for the configuration of properties.
     */
    fun overrideProperties(name: String, include: String, properties: Closure<Properties>)
            = overrideProperties(name, mutableSetOf(include), properties)

    /**
     * Adds an 'OverrideProperties' filter to all installation tasks. The configuration
     * is based on formatted properties.
     *
     * @param name the name of the filter configuration.
     * @param includes set of file filters for included files in ANT file filter style.
     * @param properties Closure for the configuration of properties.
     */
    fun overrideProperties(name: String, includes: Set<String>, properties: Closure<Properties>)
            = overrideProperties(name, includes, ClosureBackedAction<Properties>(properties))

    /**
     * Adds an 'OverrideProperties' filter to all installation tasks. The configuration
     * is based on formatted properties.
     *
     * @param name the name of the filter configuration.
     * @param include file filter for included files in ANT file filter style.
     * @param properties action for the configuration of properties.
     */
    fun overrideProperties(name: String, include: String, properties: Action<in Properties>)
            = overrideProperties(name, mutableSetOf(include), properties)

    /**
     * Adds an 'OverrideProperties' filter to all installation tasks. The configuration
     * is based on formatted properties.
     *
     * @param name the name of the filter configuration.
     * @param includes set of file filters for included files in ANT file filter style.
     * @param properties action for the configuration of properties.
     */
    fun overrideProperties(name: String, includes: Set<String>, properties: Action<in Properties>) {
        val spec = addFilterSpec(FilterSpec(name))
        spec.include(includes)

        addOverridePropertiesEditor(spec, properties)
    }

    /**
     * Adds an 'OverrideProperties' filter to all installation tasks. The configuration
     * is based on formatted properties.
     *
     * @param name the name of the filter configuration.
     * @param pattern A PatternFilterable represents some file container which Ant-style include and exclude
     * patterns or specs can be applied to.
     * @param properties closure for the configuration of properties.
     */
    fun overrideProperties(name: String, pattern: PatternFilterable, properties: Closure<Properties>)
            = overrideProperties(name, pattern, ClosureBackedAction<Properties>(properties))

    /**
     * Adds an 'OverrideProperties' filter to all installation tasks. The configuration
     * is based on formatted properties.
     *
     * @param name the name of the filter configuration.
     * @param pattern A PatternFilterable represents some file container which Ant-style include and exclude
     * patterns or specs can be applied to.
     * @param properties closure for the configuration of properties.
     */
    fun overrideProperties(name: String, pattern: PatternFilterable, properties: Action<in Properties>) {
        val spec = addFilterSpec(FilterSpec(name))
        spec.copyFrom(pattern)

        addOverridePropertiesEditor(spec, properties)
    }

    // adds the 'OverrideProperties' filter to the task
    private fun addOverridePropertiesEditor(pattern: PatternSet, properties: Action<in Properties>) {
        project.tasks.withType(InstallTask::class.java) { cp ->
            cp.eachFile { fc ->
                if(pattern.asSpec.isSatisfiedBy(fc)) {
                    fc.filter<PropertiesFilterReader>("action" to properties)
                }
            }
        }
    }

    /**
     * Adds the 'XMLContent' filter to all installation tasks. The configuration
     * is based on XmlProvider.
     *
     * @param name the name of the filter configuration.
     * @param include file filter for included files in ANT file filter style.
     * @param xml Closure for the configuration of the xml node.
     */
    fun xmlContent(name: String, include: String, xml: Closure<XmlProvider>)
            = xmlContent(name, mutableSetOf(include), xml)

    /**
     * Adds the 'XMLContent' filter to all installation tasks. The configuration
     * is based on XmlProvider.
     *
     * @param name the name of the filter configuration.
     * @param includes set of file filters for included files in ANT file filter style.
     * @param xml Closure for the configuration of the xml node.
     */
    fun xmlContent(name: String, includes: Set<String>, xml: Closure<XmlProvider>)
            = xmlContent(name, includes, ClosureBackedAction<XmlProvider>(xml))

    /**
     * Adds the 'XMLContent' filter to all installation tasks. The configuration
     * is based on XmlProvider.
     *
     * @param name the name of the filter configuration.
     * @param include file filter for included files in ANT file filter style.
     * @param xml action for the configuration of the xml node.
     */
    fun xmlContent(name: String, include: String, xml: Action<in XmlProvider>)
            = xmlContent(name, mutableSetOf(include), xml)

    /**
     * Adds the 'XMLContent' filter to all installation tasks. The configuration
     * is based on XmlProvider.
     *
     * @param name the name of the filter configuration.
     * @param includes set of file filters for included files in ANT file filter style.
     * @param xml action for the configuration of the xml node.
     */
    fun xmlContent(name: String, includes: Set<String>, xml: Action<in XmlProvider>) {
        val spec = addFilterSpec(FilterSpec(name))
        spec.include(includes)

        addXmlContentEditor(spec, xml)
    }

    /**
     * Adds the 'XMLContent' filter to all installation tasks. The configuration
     * is based on XmlProvider.
     *
     * @param name the name of the filter configuration.
     * @param pattern A PatternFilterable represents some file container which Ant-style include and exclude
     * patterns or specs can be applied to.
     * @param xml closure for the configuration of the xml node.
     */
    fun xmlContent(name: String, pattern: PatternFilterable, xml: Closure<XmlProvider>)
            = xmlContent(name, pattern, ClosureBackedAction<XmlProvider>(xml))

    /**
     * Adds the 'XMLContent' filter to all installation tasks. The configuration
     * is based on XmlProvider.
     *
     * @param name the name of the filter configuration.
     * @param pattern A PatternFilterable represents some file container which Ant-style include and exclude
     * patterns or specs can be applied to.
     * @param xml closure for the configuration of the xml node.
     */
    fun xmlContent(name: String, pattern: PatternFilterable, xml: Action<in XmlProvider>) {
        val spec = addFilterSpec(FilterSpec(name))
        spec.copyFrom(pattern)

        addXmlContentEditor(spec, xml)
    }

    // adds the 'XMLContent' filter to the task
    private fun addXmlContentEditor(pattern: PatternSet, xml: Action<in XmlProvider>) {
        project.tasks.withType(InstallTask::class.java) { cp ->
            cp.eachFile { fc ->
                if(pattern.asSpec.isSatisfiedBy(fc)) {
                    fc.filter<XMLFilterReader>("action" to xml)
                }
            }
        }
    }

    /**
     * Adds the 'FullContent' filter to all installation tasks. The configuration
     * is based on StringBuilder.
     *
     * @param name the name of the filter configuration.
     * @param include file filter for included files in ANT file filter style.
     * @param content Closure for the configuration of the content string.
     */
    fun fullContent(name: String, include: String, content: Closure<StringBuilder>)
            = fullContent(name, mutableSetOf(include), content)

    /**
     * Adds an 'FullContent' filter to all installation tasks. The configuration
     * is based on StringBuilder.
     *
     * @param name the name of the filter configuration.
     * @param includes set of file filters for included files in ANT file filter style.
     * @param content Closure for the configuration of the content string.
     */
    fun fullContent(name: String, includes: Set<String>, content: Closure<StringBuilder>)
            = fullContent(name, includes, ClosureBackedAction<StringBuilder>(content))

    /**
     * Adds the 'FullContent' filter to all installation tasks. The configuration
     * is based on StringBuilder.
     *
     * @param name the name of the filter configuration.
     * @param include file filter for included files in ANT file filter style.
     * @param content action for the configuration of the content string.
     */
    fun fullContent(name: String, include: String, content: Action<in StringBuilder>)
            = fullContent(name, mutableSetOf(include), content)

    /**
     * Adds the 'FullContent' filter to all installation tasks. The configuration
     * is based on StringBuilder.
     *
     * @param name the name of the filter configuration.
     * @param includes set of file filters for included files in ANT file filter style.
     * @param content action for the configuration of the content string.
     */
    fun fullContent(name: String, includes: Set<String>, content: Action<in StringBuilder>) {
        val spec = addFilterSpec(FilterSpec(name))
        spec.include(includes)

        addFullContentEditor(spec, content)
    }

    /**
     * Adds an 'FullContent' filter to all installation tasks. The configuration
     * is based on StringBuilder.
     *
     * @param name the name of the filter configuration.
     * @param pattern A PatternFilterable represents some file container which Ant-style include and exclude
     * patterns or specs can be applied to.
     * @param content closure for the configuration of the content string.
     */
    fun fullContent(name: String, pattern: PatternFilterable, content: Closure<StringBuilder>)
            = fullContent(name, pattern, ClosureBackedAction<StringBuilder>(content))

    /**
     * Adds the 'FullContent' filter to all installation tasks. The configuration
     * is based on StringBuilder.
     *
     * @param name the name of the filter configuration.
     * @param pattern A PatternFilterable represents some file container which Ant-style include and exclude
     * patterns or specs can be applied to.
     * @param content closure for the configuration of the content string.
     */
    fun fullContent(name: String, pattern: PatternFilterable, content: Action<in StringBuilder>) {
        val spec = addFilterSpec(FilterSpec(name))
        spec.copyFrom(pattern)

        addFullContentEditor(spec, content)
    }

    // adds the 'FullContent' filter to the task
    private fun addFullContentEditor(pattern: PatternSet, content: Action<in StringBuilder>) {
        project.tasks.withType(InstallTask::class.java) { cp ->
            cp.eachFile { fc ->
                if(pattern.asSpec.isSatisfiedBy(fc)) {
                    fc.filter<FullContentFilterReader>("action" to content)
                }
            }
        }
    }

    /**
     * Adds the 'ReplacePlaceholder' filter to all installation tasks. The configuration
     * is based on PlaceholderReplacementFilter.
     *
     * @param name the name of the filter configuration.
     * @param replacePlaceHolder Closure for the configuration of replacePlaceHolder filter.
     */
    fun replacePlaceholders(name: String, replacePlaceHolder: Closure<PlaceholderReplacementFilter>) {
        val spec = addFilterSpec(FilterSpec(name))
        val filter = PlaceholderReplacementFilter()

        project.configure(filter, replacePlaceHolder)

        spec.copyFrom(filter)

        project.tasks.withType(InstallTask::class.java) { cp ->
            cp.eachFile { fc ->
                if (spec.asSpec.isSatisfiedBy(fc)) {
                    fc.filter(filter)
                }
            }
        }
    }

    /**
     * Adds a 'Transformer' to all installation tasks. This transformer class will change the file based on each line.
     *
     * @param name the name of the filter configuration.
     * @param include file filter for included files in ANT file filter style.
     * @param transformer specifies the instance of a transformer class.
     */
    fun addTransformer(name: String, include: String, transformer: Transformer<String, String>)
            = addTransformer(name, mutableSetOf(include), transformer)

    /**
     * Adds a 'Transformer' to all installation tasks. This transformer class will change the file based on each line.
     *
     * @param name the name of the filter configuration.
     * @param includes set of file filters for included files in ANT file filter style.
     * @param transformer specifies the instance of a transformer class.
     */
    fun addTransformer(name: String, includes: Set<String>, transformer: Transformer<String, String>) {
        val spec = addFilterSpec(FilterSpec(name))
        spec.include(includes)

        addTransformerEditor(spec, transformer)
    }

    /**
     * Adds a 'Transformer' to all installation tasks. This transformer class will change the file based on each line.
     *
     * @param name the name of the filter configuration.
     * @param pattern A PatternFilterable represents some file container which Ant-style include and exclude
     * patterns or specs can be applied to.
     * @param transformer specifies the instance of a transformer class.
     */
    fun addTransformer(name: String, pattern: PatternFilterable, transformer: Transformer<String, String>) {
        val spec = addFilterSpec(FilterSpec(name))
        spec.copyFrom(pattern)

        addTransformerEditor(spec, transformer)
    }

    // add the transformer to the task.
    private fun addTransformerEditor(pattern: PatternSet, transformer: Transformer<String, String>) {
        project.tasks.withType(InstallTask::class.java) { cp ->
            cp.eachFile { fc ->
                if (pattern.asSpec.isSatisfiedBy(fc)) {
                    fc.filter(transformer)
                }
            }
        }
    }

    /**
     * Adds a 'Closure' to all installation tasks. This closure class will change the file based on each line.
     *
     * @param name the name of the filter configuration.
     * @param include file filter for included files in ANT file filter style.
     * @param closure specifies the closure.
     */
    fun addClosure(name: String, include: String, closure: Closure<*>)
            = addClosure(name, mutableSetOf(include), closure)

    /**
     * Adds a 'Closure' to all installation tasks. This closure class will change the file based on each line.
     *
     * @param name the name of the filter configuration.
     * @param includes set of file filters for included files in ANT file filter style.
     * @param closure specifies the closure.
     */
    fun addClosure(name: String, include: Set<String>, closure: Closure<*>) {
        val spec = addFilterSpec(FilterSpec(name))
        spec.include(include)

        addClosureEditor(spec, closure)
    }

    /**
     * Adds a 'Closure' to all installation tasks. This closure class will change the file based on each line.
     *
     * @param name the name of the filter configuration.
     * @param pattern A PatternFilterable represents some file container which Ant-style include and exclude
     * patterns or specs can be applied to.
     * @param closure specifies the closure.
     */
    fun addClosure(name: String, pattern: PatternFilterable, closure: Closure<*>) {
        val spec = addFilterSpec(FilterSpec(name))
        spec.copyFrom(pattern)

        addClosureEditor(spec, closure)
    }

    // add the closure to the task.
    private fun addClosureEditor(pattern: PatternSet, closure: Closure<*>) {
        project.tasks.withType(InstallTask::class.java) { cp ->
            cp.eachFile { fc ->
                if (pattern.asSpec.isSatisfiedBy(fc)) {
                    fc.filter(closure)
                }
            }
        }
    }

    // helper class for filter configuration
    private class FilterSpecNamer : Namer<FilterSpec> {
        override fun determineName(r: FilterSpec): String {
            return r.name
        }
    }
}
