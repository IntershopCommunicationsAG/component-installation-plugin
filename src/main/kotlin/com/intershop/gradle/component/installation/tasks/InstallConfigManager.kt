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
import com.intershop.gradle.component.installation.ComponentInstallPlugin.Companion.INSTALLTASKNAME
import com.intershop.gradle.component.installation.ComponentInstallPlugin.Companion.PREINSTALLTASKNAME
import com.intershop.gradle.component.installation.extension.InstallationExtension
import com.intershop.gradle.component.installation.utils.ContentType
import com.intershop.gradle.component.installation.utils.OSType
import com.intershop.gradle.component.installation.utils.OSType.Companion.checkClassifierForOS
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

/**
 * The object manages all installation information of a component. It
 * is used to create all necessary tasks.
 *
 * @property prjext the extension of the plugin.
 * @property tasks the tasks object of the model space of the plugin.
 * @property commonName the common name of the component within the installation project.
 * @property descriptor the component descriptor object
 * @property compInstallDir the installation directory of the component
 * @property update this is true if the installation of the component exists
 *
 * @constructor initializes a manager instance for a component
 */
class InstallConfigManager(private val prjext: InstallationExtension,
                           private val tasks: ModelMap<Task>,
                           private val commonName: String,
                           val descriptor: Component,
                           private val compInstallDir: File,
                           private val update: Boolean = false) {

    companion object {
        private val logger = LoggerFactory.getLogger(InstallConfigManager::class.java)

        private fun initIvyArtifact(dependency: ModuleDependency) {
            dependency.artifact {
                it.name = "ivy"
                it.type = "ivy"
                it.extension = "xml"
            }
        }

        /**
         * Get the target dir from a base dir and additional path elements.
         * The path elements will be filtered and adapted. Empty elements or elements with
         * forbidden characters will be removed, spaces replaced with an underscore.
         *
         * @param dir the base dir
         * @param path path elements
         *
         * @return a file object from the input values
         */
        @JvmStatic
        fun getTargetDir(dir: File, vararg path: String): File {
            val extPath = path.filter { ! it.isBlank()}.map { it.replace(" ", "_")}.
                    filter { it.matches("[a-z_\\-\\/0-9\\.]+".toRegex()) }.
                    joinToString( "/" )

            return when {
                extPath.isNotBlank() -> File(dir, extPath)
                else -> dir
            }
        }

        /**
         * The function returns true, if no OS configuration is specified for
         * the item or the OS configuration matches to the environment OS.
         *
         * @param item Installation item with OS specific configuration
         *
         * @return true if the OS configuration matches
         */
        @JvmStatic
        fun checkForOS(item: OSSpecificItem): Boolean {
            return checkClassifierForOS(item.classifier)
        }

        private fun pathToName(path: String): String {
            val pathSB = StringBuilder()
            if(path.isNotBlank()) {
                pathSB.append(path.split("/").joinToString("") { it.capitalize() })
            }
            return pathSB.toString()
        }
    }

    private val dependencyHandler = prjext.project.dependencies
    private val configurations = prjext.project.configurations
    private val commonPrefix = commonName.capitalize()
    private val commonConf = configurations.create(commonPrefix)

    private var resolved = false

    /**
     * The main installation task of the component.
     * It will be initialized in the init method of the class.
     *
     * @property compInstallTask the task object or null
     */
    var compInstallTask: Task? = null

    /**
     * The name of the component pre installation task. This
     * task will be executed before a sub installation task
     * of the component will be started.
     */
    var preCompInstallTaskName: String = ""

    /**
     * Get a suffix for tasks and configurations. This is
     * specific for the component and depends from the common name.
     *
     * @param suffix additional elements for the name
     *
     * @return a specify suffix for the names
     */
    fun getSuffixStr(vararg suffix: String): String {
        return commonPrefix.plus(suffix.asList().joinToString("") { it.capitalize() })
    }

    /**
     * Compares the type information of an item with
     * configured environment information.
     *
     * @param item an item with a specific environment configuration.
     *
     * @return true if the configured environment matches to the configuration of the item.
     */
    fun checkForType(item: DeploymentItem): Boolean {
        return checkForType(item.types)
    }

    /**
     * Checks the if the types of items are included in the
     * types (environments) of the installation configuration.
     *
     * If one of the item list is empty the method returns always
     * true.
     *
     * @param itemTypes set of types
     *
     * @return If some itemTypes are included in the environment configuration
     * the return value is true.
     */
    fun checkForType(itemTypes: Set<String>): Boolean {
        return if (!itemTypes.isEmpty() && ! prjext.environment.isEmpty()) {
            itemTypes.intersect(prjext.environment).isNotEmpty()
        } else {
            true
        }
    }

    // initialization of the class
    init {
        descriptor.modules.forEach { module ->

            if ((module.value.updatable || !update) &&
                    checkForType(module.value) &&
                    OSType.checkClassifierForOS(module.value.classifiers)) {

                val configSuffix = getSuffixStr("module", module.value.name)

                val depString = module.value.dependency.toString()

                val ivyDep = dependencyHandler.create(module.value.dependency.toString())
                if(ivyDep is ModuleDependency) {

                    initIvyArtifact(ivyDep)

                    val conf = configurations.create("config".plus(configSuffix))
                    conf.description = "Configuration for ${module.value.dependency.module} of '$commonName'"
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
            conf.description = "Configuration for libraries of '$commonName'"
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

        val taskPrefix = INSTALLTASKNAME.plus(commonName.capitalize())

        if (tasks.get(taskPrefix) == null) {
            tasks.create(taskPrefix) {
                it.group = ComponentInstallPlugin.INSTALLGROUPNAME
                it.description = "Run installation of '$commonName'"
            }
            val installTask = tasks.get(ComponentInstallPlugin.INSTALLTASKNAME)
            installTask?.dependsOn(taskPrefix)
        }
        compInstallTask = tasks.get(taskPrefix)

        preCompInstallTaskName = PREINSTALLTASKNAME.plus(commonName.capitalize())

        if(tasks.get(preCompInstallTaskName) == null) {
            tasks.create(preCompInstallTaskName) {
                it.group = ComponentInstallPlugin.INSTALLGROUPNAME
                it.description = "Run all tasks before the installation of '$commonName' starts"
            }
        }
        val preCompInstallTask = tasks.get(preCompInstallTaskName)

        preCompInstallTask?.dependsOn(PREINSTALLTASKNAME)
    }

    /**
     * This is a set of file items for the installation.
     * File items will replace existing files depending
     * on the OS or the environment type.
     * File items can be added from outside.
     *
     * @property fileItemSet set of file items
     */
    val fileItemSet: MutableSet<FileItem> = mutableSetOf()

    /**
     * Get a target dir from path elements for this
     * specific component.
     *
     * See also the static method getTargetDir.
     *
     * @param path path elements
     *
     * @return a file object from the input values
     */
    fun getTargetDir(vararg path: String): File {
        return getTargetDir(compInstallDir, *path)
    }

    /**
     * Get an install task for this component installation.
     * If the tasks exist it will be configured.
     *
     * @param taskName a task name
     *
     * @return a preconfigured installation task
     */
    @Throws(GradleException::class)
    fun getInstallTask(taskName: String, contentType: ContentType = ContentType.UNSPECIFIED): InstallTask {
        var task = tasks.get(taskName)

        if(task == null) {
            if(contentType == ContentType.IMMUTABLE) {
                tasks.create(taskName, InstallTask::class.java)
            } else {
                tasks.create(taskName, InstallMutableTask::class.java)
            }
            task = tasks.get(taskName)
        }
        if(task != null && task is InstallTask) {
            task.contentType = contentType
            return task
        } else {
            throw GradleException("Task '$taskName exists, but it has the wrong type!")
        }
    }

    /**
     * Adds a directory task to the task list.
     *
     * @param path the directory path
     *
     * @return configured directory task
     */
    @Throws(GradleException::class)
    fun getDirectoryTask(path: String): DirectoryTask {
        val taskName = INSTALLTASKNAME.plus(getSuffixStr("dir", pathToName(path)))
        var task = tasks.get(taskName)

        if(task == null) {
            tasks.create(taskName, DirectoryTask::class.java)
            task = tasks.get(taskName)
        }
        if(task != null && task is DirectoryTask) {
            return task
        } else {
            throw GradleException("Task '$taskName exists, but it has the wrong type!")
        }
    }

    /**
     * Add a link task for the component
     * to the task list.
     *
     * @return link task of the component.
     */
    @Throws(GradleException::class)
    fun getLinkTask(): LinkTask {
        val taskName = INSTALLTASKNAME.plus(getSuffixStr("links"))
        var task = tasks.get(taskName)

        if(task == null) {
            tasks.create(taskName, LinkTask::class.java)
            task = tasks.get(taskName)
        }
        if(task != null && task is LinkTask) {
            return task
        } else {
            throw GradleException("Task '$taskName exists, but it has the wrong type!")
        }
    }

    /**
     * It initializes the clean up task for the
     * component installation. Unused items will be removed
     * or moved to a backup directory.
     *
     * @param backupDir directory for backup files
     */
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
            task.containersPath = descriptor.containerPath
            task.modulesPath = descriptor.modulesPath
            task.modulePaths = descriptor.modules.keys
            task.containerPaths = descriptor.fileContainers.map { it.targetPath }.toSet()
            task.libsPath = descriptor.libsPath
            task.linkNames = descriptor.linkItems.map { it.name }.toSet()
            task.directoryPaths = descriptor.directoryItems.map { it.targetPath }.toSet()

            compInstallTask?.dependsOn(taskName)
        } else {
            throw GradleException("Task '$taskName exists, but it has the wrong type!")
        }
    }

    /**
     * This method configures an existing copy spec form
     * the configuration of a module item. The artifacts
     * must be resolved by the special configuration.
     *
     * @param spec the copy spec.
     * @param module the module descriptor.
     */
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
            logger.debug("Configuration '$confName' does not exists!")
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

    /**
     * This method configures an existing copy spec for the installation
     * of the libraries item of the component installation. The artifacts
     * must be resolved by the special configuration.
     *
     * @param spec the copy spec.
     * @param libs the library configuration of the component descriptor.
     */
    fun configureLibsSpec(spec: CopySpec, libs: MutableMap<String, Library>) {
        if(! resolved) {
            commonConf.resolve()
            resolved = true
        }

        val depTargets: MutableMap<String, String> = mutableMapOf()
        val fileTargets: MutableMap<String, File> = mutableMapOf()

        val confName = "config".plus(getSuffixStr("libs"))
        val conf = commonConf.extendsFrom.find { it.name ==  confName }


        if(conf != null) {
            val artifacts = conf.resolvedConfiguration.firstLevelModuleDependencies
            artifacts.forEach { rd ->
                rd.allModuleArtifacts.forEach { ra ->
                    val lib = libs.values.find { it.dependency.toString() == ra.id.componentIdentifier.displayName }
                    if(lib != null) {
                        fileTargets["${lib.targetName}.${ra.file.extension}"] = ra.file
                        depTargets[ra.id.componentIdentifier.displayName] = lib.targetName
                    } else {
                        fileTargets[ra.file.name] = ra.file
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
                            fileTargets["$data.${ra.file.extension}"] = ra.file
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
                            fileTargets["$data.${ra.file.extension}"] = ra.file
                        }
                    }
                }
            }

            fileTargets.toSortedMap().forEach {
                renameLibSpec(spec, it.value, it.key)
            }

        } else {
            throw GradleException("Configuration '$confName' does not exists!")
        }
    }
}
