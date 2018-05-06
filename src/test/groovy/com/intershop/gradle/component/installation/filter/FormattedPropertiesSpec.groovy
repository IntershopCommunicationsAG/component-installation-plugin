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

import com.intershop.gradle.test.util.TestDir
import spock.lang.Specification

class FormattedPropertiesSpec extends Specification
{
    @TestDir
    File testDir
    
    def "Added properties appear in the result, existing properties remain, removed properties are commented out"()
    {
        given:
        File propertiesFile = new File(testDir, 'test.properties')
        
        propertiesFile << 
        '''existing.property = existingPropertyValue
        removed.property = removedPropertyValue'''.stripIndent()
                       
        when:
        FormattedProperties properties = new FormattedProperties()
        propertiesFile.withReader  { reader ->
            properties.load(reader)
        }

        properties['added.property'] = 'addedPropertyValue'
        properties.remove('removed.property')
        propertiesFile.withWriter() { writer ->
            properties.store(writer)
        }
        
        then:
        propertiesFile.readLines().find { it =~ /^existing.property\W*=\W*existingPropertyValue$/ }
        propertiesFile.readLines().find { it =~ /^added.property\W*=\W*addedPropertyValue$/ }
        propertiesFile.readLines().find { it =~ /^# removed.property\W*=\W*removedPropertyValue$/ }
        propertiesFile.readLines().size() == 3
    }       
    
    def "Property is only written once if set twice"() 
    {
        given:
        File propertiesFile = new File(testDir, 'test.properties')
        
        when:        
        FormattedProperties properties = new FormattedProperties()
        
        properties['some.property'] = 'initial'
        properties['some.property'] = 'other'
                
        propertiesFile.withWriter() { writer ->
            properties.store(writer)
        }
        
        then:
        propertiesFile.readLines().findAll { it.startsWith 'some.property' }.size == 1
    }

    def "Other types of CharSequences are handled properly"()
    {
        given:
        File propertiesFile = new File(testDir, 'test.properties')

        when:
        FormattedProperties properties = new FormattedProperties()

        properties["${'G'}String"] = 'foo'
        properties['StringBuilder'] = new StringBuilder('ba').append('r')

        propertiesFile.withWriter { writer ->
            properties.store(writer)
        }

        then:
        propertiesFile.readLines().find { it =~ /^GString\W*=\W*foo$/ }
        propertiesFile.readLines().find { it =~ /^StringBuilder\W*=\W*bar$/ }
    }

    def "Double quotes will be not esacaped"()
    {
        given:
        File propertiesFile = new File(testDir, 'test.properties')

        when:
        FormattedProperties properties = new FormattedProperties()

        properties['testProperty'] = '"Property with double quotes"'

        propertiesFile.withWriter { writer ->
            properties.store(writer)
        }

        then:
        propertiesFile.readLines().find { it =~ /^testProperty\W*=\W*"Property with double quotes"$/ }
    }
}
