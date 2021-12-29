plugins {
    id("signing")
    id("maven-publish")
}

private val versionMajor = 8
private val versionMinor = 0
private val versionPatch = 1
private val versionAdapterPatch = 0

val libraryVersionName by extra("${versionMajor}.${versionMinor}.${versionPatch}.${versionAdapterPatch}")
val libraryVersionCode by extra((versionMajor * 1000000) + (versionMinor * 10000) + (versionPatch * 100) + versionAdapterPatch)

val libraryArtifactId by extra("nend-adapter")
val libraryGroupId by extra("com.applovin.mediation")

var libraryVersions = rootProject.extra["versions"] as Map<*, *>

android.defaultConfig.versionCode = libraryVersionCode
android.defaultConfig.versionName = libraryVersionName

repositories {
    maven { url = uri("http://fan-adn.github.io/nendSDK-Android-lib/library") }
}

dependencies {
    implementation("net.nend.android:nend-sdk:${libraryVersions["nend"]}")
}

publishing {
    publications {
        create<MavenPublication>(extra["publicationName"] as String) {
            //The publication doesn't know about our dependencies, so we have to manually add them to the pom
            pom.withXml {
                asNode().apply {
                    appendNode("name", libraryArtifactId)
                    appendNode("description", "Nend adapter for AppLovin MAX mediation")
                    appendNode("url", "https://www.applovin.com/")
                    appendNode("licenses")
                            .appendNode("license").apply {
                                appendNode("name", "AppLovin Corporation Mediation Adapter EULA")
                                appendNode("url", "https://www.applovin.com/eula")
                            }
                    appendNode("scm").apply {
                        appendNode("connection", "scm:git:github.com/AppLovin/AppLovin-MAX-SDK-Android.git")
                        appendNode("developerConnection", "scm:git:ssh://github.com/AppLovin/AppLovin-MAX-SDK-Android.git")
                        appendNode("url", "https://github.com/AppLovin/AppLovin-MAX-SDK-Android")
                    }
                    appendNode("developers")
                            .appendNode("developer").apply {
                                appendNode("name", "AppLovin")
                                appendNode("url", "https://www.applovin.com")
                            }
                    appendNode("dependencies").apply {
                        appendNode("dependency").apply {
                            appendNode("groupId", "net.nend.android")
                            appendNode("artifactId", "nend-sdk")
                            appendNode("version", libraryVersions["nend"])
                            appendNode("scope", "compile")
                        }
                        // Add constraint-layout dependency needed by Nend SDK: https://github.com/fan-ADN/nendSDK-Android-lib/blob/gh-pages/library/net/nend/android/nend-sdk/5.1.1/nend-sdk-5.1.1.pom
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.android.support.constraint")
                            appendNode("artifactId", "constraint-layout")
                            appendNode("version", "1.1.3")
                            appendNode("scope", "runtime")
                        }
                    }
                }
            }
        }
    }
}
