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
package com.intershop.gradle.component.installation

import com.intershop.gradle.component.descriptor.util.ComponentUtil
import com.intershop.gradle.component.installation.extension.DeploymentExtension
import com.intershop.gradle.component.installation.tasks.DeployComponent
import com.intershop.gradle.component.installation.utils.DeploymentConfiguration
import org.apache.commons.io.FileUtils
import org.apache.ivy.core.IvyPatternHelper
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.model.Defaults
import org.gradle.model.ModelMap
import org.gradle.model.Path
import org.gradle.model.RuleSource
import org.gradle.model.internal.core.ModelPath
import org.gradle.model.internal.core.ModelReference
import org.gradle.model.internal.core.ModelRegistrations
import org.gradle.model.internal.registry.ModelRegistry
import org.slf4j.LoggerFactory
import java.io.File
import java.net.URL
import javax.inject.Inject

@Suppress("unused")
class ComponentInstallPlugin @Inject constructor(private val modelRegistry: ModelRegistry,
                                                 private val dependencyHandler: DependencyHandler) : Plugin<Project> {

    companion object {
        const val COMPONENT_DESCRIPTOR_EXTENSION = "component"
        const val MAVEN_IVY_PATTERN = "[organisation]/[module]/[revision]/[module]-[revision].[ext]"
    }

    override fun apply(project: Project) {
        with(project) {
            logger.info("Deploy plugin adds extension {} to {}", DeploymentExtension.DEPLOYMENT_EXTENSION_NAME, name)

            val extension = extensions.findByType(DeploymentExtension::class.java)
                    ?: extensions.create(DeploymentExtension.DEPLOYMENT_EXTENSION_NAME,
                            DeploymentExtension::class.java, this)

            tasks.maybeCreate("deploy").group = DeploymentExtension.DEPLOYMENT_GROUP_NAME

            if(modelRegistry.state(ModelPath.nonNullValidatedPath("componentDeploymentConf")) == null) {
                modelRegistry.register(ModelRegistrations.bridgedInstance(
                        ModelReference.of("componentDeploymentConf", DeploymentConfiguration::class.java),
                        DeploymentConfiguration(
                                extension.repositoryURLProvider,
                                extension.repositoryPatternProvider,
                                extension.deploymentTargetProvider,
                                extension.deploymentComponents,
                                dependencyHandler))
                        .descriptor("Deployment configuration").build())
            }
        }
    }

    @Suppress("unused")
    class DeployRule : RuleSource() {
        companion object {
            val LOGGER = LoggerFactory.getLogger(RuleSource::class.java.simpleName)!!

            fun calculateURLForConfFile(defaultHostURL: String,
                                        defaultPattern: String,
                                        hostURL: String,
                                        pattern: String,
                                        dependency: Dependency,
                                        confDir: File) : File? {
                val urlSB = StringBuilder()

                if(! hostURL.isBlank()) {
                    urlSB.append(hostURL)
                } else {
                    urlSB.append(defaultHostURL)
                }

                if(! urlSB.endsWith('/')) {
                    urlSB.append('/')
                }

                if((pattern.isBlank() || pattern == "maven") && defaultPattern.isBlank()) {
                    // use maven pattern
                    // TODO: Snapshots will be not handeld!
                    urlSB.append(getPathForConfFromMaven(dependency))
                } else {
                    // use ivy pattern
                    if(!pattern.isBlank() && pattern != "maven") {
                        urlSB.append(getPathForConfFromIvy(pattern, dependency))
                    } else {
                        urlSB.append(getPathForConfFromIvy(defaultPattern, dependency))
                    }
                }

                val file = File(confDir,
                        "${dependency.name}-${dependency.version}.$COMPONENT_DESCRIPTOR_EXTENSION")
                FileUtils.copyURLToFile(URL(urlSB.toString()), file)

                return file
            }

            private fun getPathForConfFromIvy(pattern: String, dependency: Dependency): String {
                return IvyPatternHelper.substitute(pattern, dependency.group, dependency.name, dependency.version,
                        dependency.name, COMPONENT_DESCRIPTOR_EXTENSION, COMPONENT_DESCRIPTOR_EXTENSION)
            }

            private fun getPathForConfFromMaven(dependency: Dependency): String {
                //TODO: this will not handle snaphots!
                return getPathForConfFromIvy(MAVEN_IVY_PATTERN, dependency)
            }
        }

        @Defaults
        fun configureDeploymentTasks(tasks: ModelMap<Task>,
                                     deploymentConf: DeploymentConfiguration,
                                     @Path("buildDir") buildDir: File) {

            val defaultRepoPattern = deploymentConf.repositoryPatternProvider.getOrElse("")
            val defaultRepositoryURL = deploymentConf.repositoryURLProvider.getOrElse("")

            deploymentConf.componentContainer.forEach { deployComponent ->
                val confFile = calculateURLForConfFile(
                        defaultRepositoryURL,
                        defaultRepoPattern,
                        deployComponent.repositoryURL,
                        deployComponent.repositoryPattern,
                        deploymentConf.dependencyHandler.create(deployComponent.dependencyObject!!),
                        File(buildDir, "deploymentConf/${deployComponent.commonName}"))


                val componentDescr = ComponentUtil.componentFromFile(confFile!!)

                componentDescr.modules.forEach { moduleEntry ->
                    val taskName = "deploy${moduleEntry.value.name.capitalize()}${deployComponent.commonName}"

                    if(! tasks.containsKey(taskName)) {
                        LOGGER.info("Create Deployment Task {} from {}", taskName, moduleEntry.value.name)

                        tasks.create(taskName, DeployComponent::class.java, {
                            it.dependencyProperty = moduleEntry.value.dependency.toString()

                            it.installTarget = deploymentConf.deploymentTargetProvider.get().dir(deployComponent.path).asFile

                            it.targetPath = moduleEntry.key
                            it.targetPathInternal = moduleEntry.value.targetPath

                            it.jars = moduleEntry.value.jars

                        })
                    }

                    tasks.get("deploy")?.dependsOn(taskName)
                }
            }
        }
    }
}
