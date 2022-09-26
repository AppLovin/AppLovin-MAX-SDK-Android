plugins {
    id("signing")
    id("maven-publish")
}

private val versionMajor = 21
private val versionMinor = 8
private val versionPatch = 5
private val versionAdapterPatch = 0

val libraryVersionName by extra("${versionMajor}.${versionMinor}.${versionPatch}.${versionAdapterPatch}")
val libraryVersionCode by extra((versionMajor * 1000000) + (versionMinor * 10000) + (versionPatch * 100) + versionAdapterPatch)

val libraryArtifactId by extra("smaato-adapter")
val libraryGroupId by extra("com.applovin.mediation")

var libraryVersions = rootProject.extra["versions"] as Map<*, *>

android.defaultConfig.versionCode = libraryVersionCode
android.defaultConfig.versionName = libraryVersionName

repositories {
    maven { url = uri("https://s3.amazonaws.com/smaato-sdk-releases/") }
}

dependencies {
    implementation("com.smaato.android.sdk:module-core-light:${libraryVersions["smaato"]}")
    implementation("com.smaato.android.sdk:smaato-sdk:${libraryVersions["smaato"]}")
    implementation("com.smaato.android.sdk:smaato-sdk-in-app-bidding:${libraryVersions["smaato"]}")
    implementation("com.smaato.android.sdk:smaato-sdk-native:${libraryVersions["smaato"]}")
    implementation("androidx.lifecycle:lifecycle-extensions:${libraryVersions["lifecycle"]}")
}

publishing {
    publications {
        create<MavenPublication>(extra["publicationName"] as String) {
            //The publication doesn't know about our dependencies, so we have to manually add them to the pom
            pom.withXml {
                asNode().apply {
                    appendNode("name", libraryArtifactId)
                    appendNode("description", "Smaato adapter for AppLovin MAX mediation")
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
                    // Add Smaato network to list of dependencies.
                    appendNode("dependencies").apply {
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.smaato.android.sdk")
                            appendNode("artifactId", "module-core-light")
                            appendNode("version", libraryVersions["smaato"])
                            appendNode("scope", "compile")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.smaato.android.sdk")
                            appendNode("artifactId", "smaato-sdk")
                            appendNode("version", libraryVersions["smaato"])
                            appendNode("scope", "compile")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.smaato.android.sdk")
                            appendNode("artifactId", "smaato-sdk-in-app-bidding")
                            appendNode("version", libraryVersions["smaato"])
                            appendNode("scope", "compile")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.smaato.android.sdk")
                            appendNode("artifactId", "smaato-sdk-native")
                            appendNode("version", libraryVersions["smaato"])
                            appendNode("scope", "compile")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "androidx.lifecycle")
                            appendNode("artifactId", "lifecycle-extensions")
                            appendNode("version", libraryVersions["lifecycle"])
                            appendNode("scope", "compile")
                        }
                    }
                }
            }
        }
    }
}
