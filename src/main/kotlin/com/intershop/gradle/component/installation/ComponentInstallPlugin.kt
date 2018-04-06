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
package com.intershop.gradle.component.installation

import com.intershop.gradle.component.descriptor.util.ComponentUtil
import com.intershop.gradle.component.installation.extension.InstallationExtension
import com.intershop.gradle.component.installation.extension.OSType
import com.intershop.gradle.component.installation.utils.DescriptorManager
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.model.Defaults
import org.gradle.model.ModelMap
import org.gradle.model.RuleSource
import org.gradle.model.internal.core.ModelPath
import org.gradle.model.internal.core.ModelReference
import org.gradle.model.internal.core.ModelRegistrations
import org.gradle.model.internal.registry.ModelRegistry
import org.slf4j.LoggerFactory
import java.io.File
import javax.inject.Inject

@Suppress("unused")
class ComponentInstallPlugin @Inject constructor(private val modelRegistry: ModelRegistry) : Plugin<Project> {

    override fun apply(project: Project) {
        with(project) {
            logger.info("Install plugin adds extension {} to {}",
                    InstallationExtension.INSTALLATION_EXTENSION_NAME, name)

            val extension = extensions.findByType(InstallationExtension::class.java)
                    ?: extensions.create(InstallationExtension.INSTALLATION_EXTENSION_NAME,
                            InstallationExtension::class.java, project)

            tasks.maybeCreate("install").group = InstallationExtension.INSTALLATION_GROUP_NAME

            if(modelRegistry.state(ModelPath.nonNullValidatedPath("installExtension")) == null) {
                modelRegistry.register(ModelRegistrations.bridgedInstance(
                        ModelReference.of("installExtension", InstallationExtension::class.java), extension)
                        .descriptor("component install configuration").build())
            }
        }
    }

    @Suppress("unused")
    class InstallRule : RuleSource() {

        companion object {
            val LOGGER = LoggerFactory.getLogger(InstallRule::class.java)
        }

        @Defaults
        fun configureDeploymentTasks(tasks: ModelMap<Task>,
                                     installExtension: InstallationExtension) {

            with(installExtension) {
                if(this.detectedOS == OSType.OTHER) {
                    throw GradleException("The operating system is not suppported by the component install plugin!")
                }

                components.forEach {
                    val descriptorMgr =
                            DescriptorManager(project.repositories, it.dependency, installConfig.ivyPatterns)

                    val descriptorRepo = descriptorMgr.getDescriptorRepository()
                    if(descriptorRepo != null) {
                        val targetFile = File(installConfig.installAdminDir,
                                "descriptors/${it.commonName}/component.component")

                        descriptorMgr.loadDescriptorFile(descriptorRepo, targetFile)
                        descriptorMgr.validateDescriptor(targetFile)

                        val component = ComponentUtil.componentFromFile(targetFile)

                        println(component.fileContainers.size)

                        println(component.fileItems.size)

                        println(component.modules.size)

                        println(component.libs.size)

                        println(component.properties.size)
                    } else {
                        throw GradleException("Component '${it.dependency.getDependencyString()}' is " +
                                "not found in configured repositories. See log files for more information.")
                    }
                }
            }
        }
    }
}
