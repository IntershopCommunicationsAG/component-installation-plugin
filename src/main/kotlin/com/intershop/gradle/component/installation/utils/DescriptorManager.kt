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
package com.intershop.gradle.component.installation.utils

import com.intershop.gradle.component.descriptor.MetaData
import com.intershop.gradle.component.descriptor.util.ComponentUtil
import com.intershop.gradle.component.installation.utils.data.Artifact
import com.intershop.gradle.component.installation.utils.data.Credentials
import com.intershop.gradle.component.installation.utils.data.Dependency
import com.intershop.gradle.component.installation.utils.data.Repository
import com.intershop.gradle.component.installation.utils.data.RepositoryType
import org.apache.ivy.core.IvyPatternHelper
import org.gradle.api.GradleException
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.artifacts.repositories.IvyArtifactRepository
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.jsoup.Jsoup
import org.slf4j.LoggerFactory
import org.w3c.dom.Element
import sun.net.www.protocol.file.FileURLConnection
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.InetSocketAddress
import java.net.Proxy
import java.net.URL
import java.net.URLConnection
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import javax.xml.parsers.DocumentBuilderFactory

/**
 * This class contains all methods and data to handle the
 * download of the descriptor and all items of the component.
 * The standard Gradle resolution is not used and downloaded
 * items will be not cached.
 *
 * @property repositories   initialized project repository handler
 * @property descriptor     dependency configuration of the component
 * @property ivyPattern     all used ivy patterns (it is not possible to get this
 * information from the repository handler)
 *
 * @constructor initialize this class
 */
class DescriptorManager(private val repositories: RepositoryHandler,
                        private val descriptor: Dependency,
                        private val ivyPattern: Set<String>) {

    companion object {
        private val logger = LoggerFactory.getLogger(DescriptorManager::class.java)
        private const val BASE_PATTERN = "[organisation]/[module]/[revision]"

        /**
         * Default INTERSHOP IVY pattern for artifacts.
         */
        const val INTERSHOP_PATTERN = "$BASE_PATTERN/[ext]s/[artifact]-[type](-[classifier])-[revision].[ext]"
        /**
         * Default INTERSHOP IVY pattern for ivy files.
         */
        const val INTERSHOP_IVY_PATTERN = "$BASE_PATTERN/[type]s/ivy-[revision].xml"

        /**
         * Descriptor name.
         */
        const val DESCRIPTOR_NAME = "component"

        @JvmStatic
        private fun getPathFromIvy(dependency: Dependency, repo: Repository, artifact: Artifact) : String {
            val pathBuilder = StringBuilder(repo.urlStr)
            if(!pathBuilder.endsWith("/")) {
                pathBuilder.append("/")
            }
            val extraAttributes = mutableMapOf<String, String>()
            if(artifact.classifier.isNotBlank()) {
                extraAttributes["classifier"] = artifact.classifier
            }
            pathBuilder.append(IvyPatternHelper.substitute(repo.pattern, dependency.group, dependency.module,
                    repo.version, artifact.artifact, artifact.type, artifact.ext, "", extraAttributes,
                    mutableMapOf<String,String>()))

            return pathBuilder.toString()
        }

        @JvmStatic
        private fun getUrlConnection(urlString: String, credentials: Credentials) : URLConnection {
            val urlInternal = URL(urlString)

            val scheme = urlInternal.protocol
            val proxyHost = System.getProperty("$scheme.proxyHost", "")
            val proxyPort = System.getProperty("$scheme.proxyPort", "")

            if(scheme == "https") {
                val sslContext = SSLContext.getInstance("SSL")

                val trustAllCerts = arrayOf<TrustManager>(object : X509TrustManager {
                    override fun getAcceptedIssuers(): Array<X509Certificate>? = null
                    override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                    override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
                })

                // Create all-trusting host name verifier
                val allHostsValid = HostnameVerifier { _, _ -> true }

                sslContext.init(null, trustAllCerts, SecureRandom())
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
                HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid)
            }

            val connection = if(proxyPort.isNotBlank() && proxyHost.isNotBlank() && scheme.startsWith("http")) {
                logger.info("Proxy host is used: {}://{}:{}", scheme, proxyHost, proxyPort)
                val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, Integer.parseInt(proxyPort)))
                urlInternal.openConnection(proxy)
            } else {
                urlInternal.openConnection()
            }

            if(credentials.username.isNotBlank() && credentials.password.isNotBlank() && scheme.startsWith("http") ) {
                logger.info("User {} is used for access.", credentials.username)
                connection.setRequestProperty("Authorization", "Basic ${credentials.authString}")
            }

            return connection
        }

        private fun getMavenModulePath(dependency: Dependency, hostURL: String) : String {
            val path = StringBuilder(hostURL)
            if(! path.endsWith("/")) {
                path.append("/")
            }
            path.append(dependency.group.replace(".", "/"))
            if(! path.endsWith("/")) {
                path.append("/")
            }
            path.append(dependency.module)
            return path.toString()
        }

        @Suppress("UnsafeCast")
        @JvmStatic
        protected fun getSnapshotVersionFromMaven(metadata: Any, version: String) : String {
            var updateStr = ""

            val versioningElement = getVersioningNodeFromMaven(metadata)
            if(versioningElement != null) {
                val snapshotElements = versioningElement.getElementsByTagName("snapshot")
                if(snapshotElements.length > 0) {
                    val snapshotElement = snapshotElements.item(0) as Element
                    val timestamp = snapshotElement.getElementsByTagName("timestamp").item(0).textContent
                    val buildNumber = snapshotElement.getElementsByTagName("buildNumber").item(0).textContent

                    val baseVersion = version.replace("-SNAPSHOT", "")

                    updateStr = "$baseVersion-$timestamp-$buildNumber"
                }
            }

            return updateStr
        }

        @Suppress("UnsafeCast")
        @JvmStatic
        @JvmOverloads
        protected fun getVersionFromMaven(metadata: Any, regex:String = "") : String{
            var version = ""

            val versioningElement = getVersioningNodeFromMaven(metadata)
            if(versioningElement != null) {

                val versionsElement = versioningElement.getElementsByTagName("versions")
                if (versionsElement.length > 0) {
                    val versionElement = versionsElement.item(0) as Element
                    val versionElements = versionElement.getElementsByTagName("version")
                    val versionList = mutableListOf<String>()
                    val regexObj = regex.toRegex()

                    for (i in 0 until versionElements.length) {
                        if (regex.isBlank() ||
                                (regex.isNotBlank() && versionElements.item(i).textContent.matches(regexObj))) {
                            versionList.add(versionElements.item(i).textContent)
                        }
                    }

                    if (! versionList.isEmpty()) {
                        version = versionList.sortedWith(VersionComparator()).last()
                    }
                }
            }
            return version
        }

        @Suppress("UnsafeCast")
        @JvmStatic
        protected fun getVersioningNodeFromMaven(metadata: Any) : Element? {
            var returnElement: Element? = null

            val dbFactory = DocumentBuilderFactory.newInstance()
            val builder = dbFactory.newDocumentBuilder()

            val meta = if( metadata is File ) builder.parse(metadata) else builder.parse(metadata as InputStream)
            meta.documentElement.normalize()

            val versioning = meta.getElementsByTagName("versioning")
            if(versioning.length > 0) {
                returnElement = versioning.item(0) as Element
            }
            return returnElement
        }

        @Suppress("UnsafeCast")
        @JvmStatic
        @JvmOverloads
        protected fun getVersionFromIvyIndex(index: Any, pattern: String = "", baseURL: String = "") : String {
            val versionList = mutableListOf<String>()

            val doc = if( index is File ) {
                Jsoup.parse(index, "UTF-8")
            } else {
                Jsoup.parse(index as InputStream, "UTF-8", baseURL)
            }

            val links = doc.getElementsByTag("a")
            val patterRegex = pattern.toRegex()
            links.filter { ! it.attr("href").startsWith("..") &&
                    it.attr("href").endsWith("/")}.forEach {

                val versionTxt = it.text().replace("/","")
                if((pattern.isNotBlank() && versionTxt.matches(patterRegex)) || pattern.isBlank()) {
                    versionList.add(versionTxt)
                }
            }

            return if(versionList.size > 0) {
                versionList.sortedWith(VersionComparator()).last()
            } else {
                ""
            }
        }

        @JvmStatic
        @JvmOverloads
        protected fun getVersionFromIvyDir(moduleDir: File, pattern: String = "") : String {
            val versionList = mutableListOf<String>()

            if (moduleDir.isDirectory && moduleDir.canRead()) {
                if (pattern.isNotBlank()) {
                    val regex = pattern.toRegex()
                    moduleDir.list { _, name -> name.matches(regex) }.forEach {
                        versionList.add(it)
                    }
                } else {
                    moduleDir.list().forEach {
                        versionList.add(it)
                    }
                }
            }

            return if(versionList.size > 0) {
                versionList.sortedWith(VersionComparator()).last()
            } else {
                ""
            }
        }
    }

    /**
     * Repository that includes the component with a descriptor.
     *
     * @property descriptorRepo repository object with component descriptor.
     */
    lateinit var descriptorRepo: Repository

    init {
        calcDescriptorRepository()
    }

    /**
     * Load descriptor file from the repository. The repository is the first
     * repository with the configured component.
     *
     * @param target    target file for download
     */
    @Throws(IOException::class, GradleException::class)
    fun loadDescriptorFile(target: File) {
        val artifact = Artifact(descriptor.module, DESCRIPTOR_NAME, DESCRIPTOR_NAME)
        loadArtifactFile(artifact, target)
    }

    /**
     * Load component artifact from the repository. The repository is the first
     * repository with the configured component.
     *
     * @param artifact  artifact description.
     * @param target    target file for download.
     */
    @Throws(IOException::class, GradleException::class)
    fun loadArtifactFile(artifact: Artifact, target: File) {
        val path = when(descriptorRepo.type) {
            RepositoryType.IVY -> {
                getPathFromIvy(descriptor, descriptorRepo, artifact)
            }
            RepositoryType.MAVEN -> {
                val pathBuilder = StringBuilder(descriptorRepo.artifactPath)
                pathBuilder.append("-")
                pathBuilder.append(artifact.type).append(".")
                pathBuilder.append(artifact.ext)
                pathBuilder.toString()
            }
        }

        if(path.isNotBlank()) {
            target.parentFile.mkdirs()
            if(! target.parentFile.exists()) {
                throw GradleException("It was not possible to create directory '${target.parent}'." )
            }
            if(target.exists() && ! (target.isFile && target.canWrite())) {
                throw GradleException("The file '${target.absolutePath} can not be created.")
            }
            if((target.exists() && ! target.delete()) && target.createNewFile()) {
                throw GradleException("The file '${target.absolutePath} can not be recreated.")
            }

            val conn = getUrlConnection(path, descriptorRepo.credentials)
            target.copyInputStreamToFile(conn.getInputStream())
        }
    }

    private fun calcDescriptorRepository() {
        val versionRepoMap = mutableMapOf<String, Repository>()

        repositories.forEach repo@{ repo ->
            if((descriptor.hasLatestPattern) || versionRepoMap.size != 1) {
                when (repo) {
                    is IvyArtifactRepository -> {
                        ivyPattern.forEach { pattern ->
                            checkIvyAvailability(Repository.initIvyRepoFrom(repo, pattern), versionRepoMap)
                            if (!(descriptor.hasLatestPattern) && versionRepoMap.size == 1) return@repo
                        }
                    }
                    is MavenArtifactRepository -> {
                        checkMavenAvailability(Repository.initMavenRepoFrom(repo), versionRepoMap)
                    }
                    else -> {
                        throw GradleException("This kind of repository configuration for '${repo.name}'" +
                                " is not supported by the 'component-install-plugin' - wrong Gradle configuration.")
                    }
                }
            }
        }

        if(versionRepoMap.isNotEmpty()) {
            val repo = versionRepoMap[versionRepoMap.keys.sortedWith(VersionComparator()).last()]
            if(repo != null) {
                descriptorRepo = repo
            }
        }

        try {
            val url = descriptorRepo.url
            logger.info("Descriptor '{}' found in '{}'.", descriptor, url)
        } catch(ex: UninitializedPropertyAccessException) {
            throw GradleException("Descriptor '$descriptor' not found in the configured repositories! " +
                    "Please check your log files!")
        }
    }

    private fun checkIvyAvailability(repoData: Repository, versionRepoMap: MutableMap<String, Repository>) {
        try {
            val version = addIvyVersion(repoData)
            if(version.isNotBlank() && ! versionRepoMap.containsKey(version)) {
                versionRepoMap[version] = repoData
            }
        } catch (ex: IOException) {
            logger.info("Dependency {} is not available in {}.", descriptor.getDependencyString(), repoData.url)
        }
    }

    private fun checkMavenAvailability(repoData: Repository, versionRepoMap: MutableMap<String, Repository>) {
        try {
            val version = addMavenArtifactPath(repoData)
            if(version.isNotBlank() && ! versionRepoMap.containsKey(version)) {
                versionRepoMap[version] = repoData
            }
        } catch (ex: IOException) {
            logger.info("Dependency {} is not available in {}.", descriptor.getDependencyString(), repoData.url)
        }
    }

    /**
     * Adds a final version string - without any placeholders - to the
     * internal repository object.
     *
     * @param repo internal repository object.
     *
     * @return version string
     */
    @Throws(IOException::class)
    fun addIvyVersion(repo: Repository) : String {
        val version: String

        val revPos = repo.pattern.indexOf("[revision]")
        val verPattern = repo.pattern.substring(0, revPos - 1)

        val urlPath = StringBuilder(repo.urlStr)
        if(! urlPath.endsWith("/")) {
            urlPath.append("/")
        }
        urlPath.append(IvyPatternHelper.substitute(verPattern,
                descriptor.group, descriptor.module, descriptor.version,
                descriptor.module, "", ""))

        val connection = getUrlConnection(urlPath.toString(), repo.credentials)

        when(connection) {
            is HttpURLConnection -> {
                version = when {
                    descriptor.hasLatestVersion ->
                        getVersionFromIvyIndex(connection.inputStream, "", repo.urlStr)
                    descriptor.hasVersionPattern ->
                        getVersionFromIvyIndex(connection.inputStream, descriptor.versionPattern, repo.urlStr)
                    else ->
                        getVersionFromIvyIndex(connection.inputStream, descriptor.versionPattern)
                }
            }
            is FileURLConnection -> {
                val dir = File(connection.url.toURI())
                version = when {
                    descriptor.hasLatestVersion -> getVersionFromIvyDir(dir, "")
                    descriptor.hasVersionPattern -> getVersionFromIvyDir(dir, descriptor.versionPattern)
                    else -> getVersionFromIvyDir(dir, descriptor.versionPattern)
                }
            }
            else -> {
                throw GradleException("This kind of repository configuration for '${repo.url}' ($connection)" +
                        " is not supported by the 'component-install-plugin' - wrong connection.")
            }
        }

        repo.version = version
        return version
    }

    /**
     * Adds a artifact path to an internal repository object
     * of an IVY repository.
     *
     * @param repo internal repository object.
     *
     * @return artifact path.
     */
    @Throws(IOException::class)
    fun addMavenArtifactPath(repo: Repository): String {
        val version: String

        val path = getMavenModulePath(descriptor, repo.urlStr)

        version = when {
            descriptor.hasLatestVersion -> {
                val connection = getUrlConnection("$path/maven-metadata.xml", repo.credentials)
                getVersionFromMaven(connection.inputStream)
            }
            descriptor.hasVersionPattern -> {
                val connection = getUrlConnection("$path/maven-metadata.xml", repo.credentials)
                getVersionFromMaven(connection.inputStream, descriptor.versionPattern)
            }
            else -> {
                val connection = getUrlConnection("$path/maven-metadata.xml", repo.credentials)
                getVersionFromMaven(connection.inputStream, descriptor.versionPattern)
            }
        }

        var updateStr = ""

        if(version.endsWith("-SNAPSHOT")) {
            val connection = getUrlConnection("$path/$version/maven-metadata.xml", repo.credentials)
            updateStr = getSnapshotVersionFromMaven(connection.inputStream, descriptor.version)
        }

        val resultBuilder = StringBuilder(path)
        if(version.isNotBlank()) {
            resultBuilder.append("/").append(version).append("/").append(descriptor.module).append("-")
            if(updateStr.isNotBlank()) {
                resultBuilder.append(updateStr)
            } else {
                resultBuilder.append(version)
            }
        }

        repo.artifactPath = resultBuilder.toString()
        repo.version = version

        return version
    }

    /**
     * Determines the meta data of the descriptor file.
     *
     * @param targetFile the descriptor file.
     *
     * @return meta data information of the descriptor.
     */
    @Throws(GradleException::class)
    fun getDescriptorMetadata(targetFile: File): MetaData {
        val metadata = ComponentUtil.metadataFromFile(targetFile)

        if(metadata.version != ComponentUtil.version) {
            throw GradleException("The component descriptor '${descriptor.getDependencyString()}'" +
                    "was created by an other version (Descriptor version is '${metadata.version}', " +
                    "but the used framework has '${ComponentUtil.version}').")
        }

        return metadata
    }
}
