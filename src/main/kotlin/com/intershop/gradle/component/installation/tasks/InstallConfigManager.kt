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

import com.intershop.gradle.component.descriptor.Component
import com.intershop.gradle.component.descriptor.Library
import com.intershop.gradle.component.descriptor.Module
import com.intershop.gradle.component.descriptor.items.DeploymentItem
import com.intershop.gradle.component.descriptor.items.OSSpecificItem
import com.intershop.gradle.component.installation.ComponentInstallPlugin
import com.intershop.gradle.component.installation.extension.InstallationExtension
import com.intershop.gradle.component.installation.extension.OSType.Companion.checkClassifierForOS
import com.intershop.gradle.component.installation.utils.data.FileItem
import org.gradle.api.GradleException
import org.gradle.api.Task
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.file.CopySpec
import org.gradle.ivy.IvyDescriptorArtifact
import org.gradle.ivy.IvyModule
import org.gradle.maven.MavenModule
import org.gradle.maven.MavenPomArtifact
import org.gradle.model.ModelMap
import org.slf4j.LoggerFactory
import java.io.File

class InstallConfigManager(val prjext: InstallationExtension,
                           val tasks: ModelMap<Task>,
                           val commonName: String,
                           val descriptor: Component,
                           val compInstallDir: File,
                           val update: Boolean = false) {

    companion object {
        private val logger = LoggerFactory.getLogger(InstallConfigManager::class.java)

        const val PREFIX = "config"

        private fun initIvyArtifact(dependency: ModuleDependency) {
            dependency.artifact {
                it.name = "ivy"
                it.type = "ivy"
                it.extension = "xml"
            }
        }

        @JvmStatic
        fun getTargetDir(dir: File, vararg path: String): File {
            val extPath = path.filter { ! it.isNullOrBlank()}.map { it.replace(" ", "_")}.
                    filter { it.matches("[a-z_\\-\\/0-9\\.]+".toRegex()) }.
                    joinToString( "/" )

            return when {
                extPath.isNotBlank() -> File(dir, extPath)
                else -> dir
            }
        }

        fun checkForOS(item: OSSpecificItem): Boolean {
            return checkClassifierForOS(item.classifier)
        }
    }

    private val dependencyHandler = prjext.project.dependencies
    private val configurations = prjext.project.configurations
    private val commonPrefix = commonName.capitalize()
    private val commonConf = configurations.create(commonPrefix)

    private var resolved = false

    var compInstallTask: Task? = null

    fun getSuffixStr(vararg suffix: String): String {
        return commonPrefix.plus(suffix.asList().map { it.capitalize() }.joinToString(""))
    }

    fun checkForType(item: DeploymentItem): Boolean {
        return if (!item.types.isEmpty() && ! prjext.environment.isEmpty()) {
            item.types.intersect(prjext.environment).size > 0
        } else {
            true
        }
    }

    init {
        descriptor.modules.forEach { module ->

            if (((module.value.updatable && update) || !update) && checkForType(module.value)) {
                val configSuffix = getSuffixStr("module", module.value.name)

                val depString = module.value.dependency.toString()

                val ivyDep = dependencyHandler.create(module.value.dependency.toString())
                if(ivyDep is ModuleDependency) {

                    initIvyArtifact(ivyDep)

                    val conf = configurations.create("config".plus(configSuffix))
                    conf.description = "Configuration for ${module.value.dependency.module} of '${commonName}'"
                    conf.isTransitive = false
                    conf.defaultDependencies {
                        it.add(dependencyHandler.create(depString))
                        it.add(dependencyHandler.create(ivyDep))
                    }

                    commonConf.extendsFrom(conf)
                } else {
                    throw GradleException("Dependency '${module.value.dependency}' is " +
                            "not supported by the installation plugin.")
                }
            }
        }

        if (descriptor.libs.isNotEmpty()) {
            val configSuffix = getSuffixStr("libs")
            val conf = configurations.create("config$configSuffix")
            conf.description = "Configuration for libraries of '${commonName}'"
            conf.isTransitive = false

            conf.defaultDependencies {
                descriptor.libs.values.forEach { lib ->
                    if(checkForType(lib)) {
                        it.add(dependencyHandler.create(lib.dependency.toString()))
                    }
                }
            }

            commonConf.extendsFrom(conf)
        }

        val taskPrefix = "install".plus(commonName.capitalize())

        if (tasks.get(taskPrefix) == null) {
            tasks.create(taskPrefix) {
                it.group = ComponentInstallPlugin.INSTALLGROUPNAME
                it.description = "Run installation of '${commonName}'"
            }
            val installTask = tasks.get(ComponentInstallPlugin.INSTALLTASKNAME)
            installTask?.dependsOn(taskPrefix)
        }
        compInstallTask = tasks.get(taskPrefix)
    }

    val fileItemSet: MutableSet<FileItem> = mutableSetOf()

    fun getTargetDir(vararg path: String): File {
        return getTargetDir(compInstallDir, *path)
    }

    @Throws(GradleException::class)
    fun getInstallTask(taskName: String): InstallTask {
        var task = tasks.get(taskName)

        if(task == null) {
            tasks.create(taskName, InstallTask::class.java)
            task = tasks.get(taskName)
        }
        if(task != null && task is InstallTask) {
            return task
        } else {
            throw GradleException("Task '${taskName} exists, but it has the wrong type!")
        }
    }

    @Throws(GradleException::class)
    fun initCleanupTask(backupDir: File) {

        val taskName = "cleanup".plus(getSuffixStr("CleanUp"))
        var task = tasks.get(taskName)

        if(task == null) {
            tasks.create(taskName, CleanUpTask::class.java)
            task = tasks.get(taskName)
        }
        if(task != null && task is CleanUpTask) {
            task.installDir = compInstallDir
            task.backupDir = backupDir
            task.descriptorPath = descriptor.descriptorPath
            task.containersPath = descriptor.containerTarget
            task.modulesPath = descriptor.modulesTarget
            task.modulePaths = descriptor.modules.keys
            task.containerPaths = descriptor.fileContainers.map { it.targetPath }.toSet()
            task.libsPath = descriptor.libsTarget

            compInstallTask?.dependsOn(taskName)
        } else {
            throw GradleException("Task '${taskName} exists, but it has the wrong type!")
        }
    }

    @Throws(GradleException::class)
    fun configureModuleSpec(spec: CopySpec, module: Module) {
        if(! resolved) {
            commonConf.resolve()
            resolved = true
        }

        val confName = "config".plus(getSuffixStr("module", module.name))
        val conf = commonConf.extendsFrom.find { it.name ==  confName }

        if(conf != null) {
            val resolvedArtifacts = conf.resolvedConfiguration.firstLevelModuleDependencies.first().allModuleArtifacts

            resolvedArtifacts.forEach { resolvedArtifact ->
                with(resolvedArtifact) {
                    when {
                        (type == "jar" && extension == "jar" && module.jars.contains(name)) -> {
                            spec.from(prjext.project.file(file)) {
                                it.into(module.jarPath)
                            }
                        }
                        (type == "ivy" && extension == "xml" && name == "ivy") -> {
                            spec.from(prjext.project.file(file)) {
                                it.into(module.descriptorPath)
                            }
                        }
                        (extension == "zip" &&
                                (classifier == null || checkClassifierForOS(classifier ?: ""))) -> {
                            spec.from(prjext.project.zipTree(file))
                        }
                        else -> {
                            logger.debug("Artifact '{}' is not used for '{}'!", this, module.name)
                        }
                    }
                }
            }
        } else {
            throw GradleException("Configuration '$confName' does not exists!")
        }
    }

    private fun renameLibSpec(spec: CopySpec, file: File, filename: String) {
        val pFile = prjext.project.file(file)
        spec.from(pFile) {
            it.rename { name ->
                name.replace(file.name, filename)
            }
        }
    }

    fun configureLibsSpec(spec: CopySpec, libs: MutableMap<String, Library>) {
        if(! resolved) {
            commonConf.resolve()
            resolved = true
        }

        val depTargets: MutableMap<String, String> = mutableMapOf()

        val confName = "config".plus(getSuffixStr("libs"))
        val conf = commonConf.extendsFrom.find { it.name ==  confName }

        if(conf != null) {
            val artifacts = conf.resolvedConfiguration.firstLevelModuleDependencies
            artifacts.forEach { rd ->
                rd.allModuleArtifacts.forEach { ra ->
                    val lib = libs.values.find { it.dependency.toString() == ra.id.componentIdentifier.displayName }
                    if(lib != null) {
                        renameLibSpec(spec, ra.file, "${lib.targetName}.${ra.file.extension}")
                        depTargets[ra.id.componentIdentifier.displayName] = lib.targetName
                    } else {
                        spec.from(prjext.project.file(ra.file))
                        depTargets[ra.id.componentIdentifier.displayName] = ra.file.nameWithoutExtension
                    }
                }
            }

            val componentIds = conf.incoming.resolutionResult.allDependencies.mapNotNull {
                (it as? ResolvedDependencyResult)?.selected?.id
            }

            val ivyResult = dependencyHandler.createArtifactResolutionQuery().forComponents(componentIds).
                    withArtifacts(IvyModule::class.java, IvyDescriptorArtifact::class.java).execute()

            ivyResult.resolvedComponents.forEach {
                it.getArtifacts(org.gradle.api.component.Artifact::class.java).forEach { ra ->
                    if(ra is ResolvedArtifactResult) {
                        val data = depTargets[ra.id.componentIdentifier.displayName]
                        if(data != null) {
                            renameLibSpec(spec, ra.file, "$data.${ra.file.extension}")
                        }
                    }
                }
            }

            val pomResult = dependencyHandler.createArtifactResolutionQuery().forComponents(componentIds).
                    withArtifacts(MavenModule::class.java,  MavenPomArtifact::class.java).execute()

            pomResult.resolvedComponents.forEach {
                it.getArtifacts(org.gradle.api.component.Artifact::class.java).forEach { ra ->
                    if(ra is ResolvedArtifactResult) {
                        val data = depTargets[ra.id.componentIdentifier.displayName]
                        if(data != null) {
                            renameLibSpec(spec, ra.file, "$data.${ra.file.extension}")
                        }
                    }
                }
            }
        } else {
            throw GradleException("Configuration '$confName' does not exists!")
        }
    }
}
