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

import com.intershop.gradle.component.installation.utils.data.Artifact
import com.intershop.gradle.component.installation.utils.data.Credentials
import com.intershop.gradle.component.installation.utils.data.Dependency
import com.intershop.gradle.component.installation.utils.data.Repository
import org.apache.ivy.core.IvyPatternHelper
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

@Suppress("UnsafeCast")
class RepositoryUtil {

    companion object {

        internal val logger = LoggerFactory.getLogger(RepositoryUtil::class.java.simpleName)

        const val DESCRIPTOR_EXTENSION = "component"

        const val BASE_PATTERN = "[organisation]/[module]/[revision]"
        const val MAVEN_PATTERN = "$BASE_PATTERN/[module]-[revision].[ext]"
        const val INTERSHOP_PATTERN = "$BASE_PATTERN/[ext]s/[artifact]-[type](-[classifier])-[revision].[ext]"
        const val INTERSHOP_IVY_PATTERN = "$BASE_PATTERN/[type]s/ivy-[revision].xml"

        const val DEFAULT_PROXY_PORT = -1

        @JvmStatic
        protected fun getUrlconnection(urlString: String, credentials: Credentials) : URLConnection {
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

        @JvmStatic
        protected fun getPathFromIvy(dependency: Dependency, repo: Repository,
                                     version: String, artifact: Artifact) : String {

            return if(version.isNotBlank()) {
                 IvyPatternHelper.substitute(repo.pattern, dependency.group, dependency.module, version,
                        artifact.artifcat, artifact.type, artifact.ext)
            } else {
                ""
            }
        }

        @JvmStatic
        protected fun getIvyVersion(dependency: Dependency, repo: Repository) : String {
            var version = dependency.version

            if(version.endsWith("+") || version == "latest") {

                val revPos = repo.pattern.indexOf("[revision]")
                val verPattern = repo.pattern.substring(0, revPos - 1)

                val urlPath = StringBuilder(repo.url)
                if(! urlPath.endsWith("/")) {
                    urlPath.append("/")
                }
                urlPath.append(IvyPatternHelper.substitute(verPattern,
                        dependency.group, dependency.module, dependency.version,
                        dependency.module, "", ""))

                var connection = getUrlconnection(urlPath.toString(), repo.credentials)

                when(connection) {
                    is HttpURLConnection -> {
                        if (version == "+" || version == "latest") {
                            version = getLatestVersionFromIvyIndex(connection.inputStream, "", repo.url)
                        } else if (version.endsWith("+")) {
                            version = getLatestVersionFromIvyIndex(connection.inputStream,
                                    version.replace(".", "\\.").replace("+", ".*"), repo.url)
                        }
                    }
                    is FileURLConnection -> {
                        val dir = File(connection.url.toURI())
                        if (version == "+" || version == "latest") {
                            version = getLatestVersionFromIvyDir(dir, "")
                        } else if (version.endsWith("+")) {
                            version = getLatestVersionFromIvyDir(dir,
                                    version.replace(".", "\\.").replace("+", ".*"))
                        }
                    }
                    else -> {

                    }
                }
            }

            return version
        }

        @JvmStatic
        protected fun addMavenArtifactPath(dependency: Dependency, repo: Repository): String {

            var version = dependency.version
            var path = getMavenModulePath(dependency, repo.url)

            if(version == "+" || version == "latest") {
                var connection = getUrlconnection("$path/maven-metadata.xml", repo.credentials)

                version = getLatestVersionFromMaven(connection.inputStream)
            } else if(version.endsWith("+")) {
                var connection = getUrlconnection("$path/maven-metadata.xml", repo.credentials)

                version = getLatestVersionFromMaven(connection.inputStream,
                            version.replace(".", "\\.").replace("+", ".*"))
            }

            var updateStr = ""

            if(version.endsWith("-SNAPSHOT")) {
                val connection = getUrlconnection("$path/${version}/maven-metadata.xml", repo.credentials)
                try {
                    updateStr = getSnapshotVersionFromMaven(connection.inputStream, dependency.version)
                }catch (ioException: IOException) {
                    logger.debug("No metadata for SNAPSHOTS available")
                }
            }

            val resultBuilder = StringBuilder(path)
            if(version.isNotBlank()) {
                resultBuilder.append(version).append("/").append(dependency.module).append("-")
                if(updateStr.isNotBlank()) {
                    resultBuilder.append(updateStr)
                } else {
                    resultBuilder.append(version)
                }
            }

            repo.artifactPath = resultBuilder.toString()

            return version
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
            path.append(dependency.module).append("/")
            return path.toString()
        }

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

        @JvmStatic
        @JvmOverloads
        protected fun getLatestVersionFromMaven(metadata: Any, regex:String = "") : String{
            var version = ""

            val versioningElement = getVersioningNodeFromMaven(metadata)
            if(versioningElement != null) {

                val versionsElement = versioningElement.getElementsByTagName("versions")
                if (versionsElement.length > 0) {
                    val versionElement = versionsElement.item(0) as Element
                    val versionElements = versionElement.getElementsByTagName("version")
                    val versionList = mutableListOf<String>()
                    val regexObj = regex.toRegex()

                    for (i in 0..versionElements.length - 1) {
                        if (regex.isBlank() ||
                                (regex.isNotBlank() && versionElements.item(i).textContent.matches(regexObj))) {
                            versionList.add(versionElements.item(i).textContent)
                        }
                    }

                    version = versionList.sortedWith(VersionComparator()).last()
                }
            }
            return version
        }

        @JvmStatic
        protected fun getVersioningNodeFromMaven(metadata: Any) : Element? {
            var returnElement: Element? = null

            val dbFactory = DocumentBuilderFactory.newInstance()
            val builder = dbFactory.newDocumentBuilder()

            val meta = if( metadata is File ) builder.parse(metadata) else builder.parse(metadata as InputStream)
            meta.getDocumentElement().normalize()

            val versioning = meta.getElementsByTagName("versioning")
            if(versioning.length > 0) {
                returnElement = versioning.item(0) as Element
            }
            return returnElement
        }

        @JvmStatic
        @JvmOverloads
        protected fun getLatestVersionFromIvyIndex(index: Any, pattern: String = "", baseURL: String = "") : String {
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
                if(pattern.isNotBlank() && it.text().matches(patterRegex)) {
                    versionList.add(it.text().replace("/",""))
                } else if(pattern.isBlank()) {
                    versionList.add(it.text().replace("/",""))
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
        protected fun getLatestVersionFromIvyDir(dir: File, pattern: String = "") : String {
            val versionList = mutableListOf<String>()

            if (dir.isDirectory && dir.canRead()) {
                if (pattern.isNotBlank()) {
                    val regex = pattern.toRegex()
                    dir.list { dir, name -> name.matches(regex) }.forEach {
                        versionList.add(it)
                    }
                } else {
                    dir.list().forEach {
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
}
