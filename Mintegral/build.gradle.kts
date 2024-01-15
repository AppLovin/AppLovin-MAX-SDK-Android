plugins {
    id("signing")
    id("maven-publish")
}

// NOTE: Mintegral has 2 separate SDK versions, e.g. x.x.51 for Google Play & x.x.52 for Android Market (in China)
private val versionMajor = 16
private val versionMinor = 6
private val versionPatch = 11
private val versionAdapterPatch = 0

val libraryVersionName by extra("${versionMajor}.${versionMinor}.${versionPatch}.${versionAdapterPatch}")
val libraryVersionCode by extra((versionMajor * 1000000) + (versionMinor * 10000) + (versionPatch * 100) + versionAdapterPatch)

val libraryArtifactId by extra("mintegral-adapter")
val libraryGroupId by extra("com.applovin.mediation")

var libraryVersions = rootProject.extra["versions"] as Map<*, *>

android.defaultConfig.versionCode = libraryVersionCode
android.defaultConfig.versionName = libraryVersionName

repositories {
    maven { url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea") }
}

dependencies {
    implementation("com.mbridge.msdk.oversea:same:${libraryVersions["mintegral"]}")
    implementation("com.mbridge.msdk.oversea:interstitial:${libraryVersions["mintegral"]}")
    implementation("com.mbridge.msdk.oversea:interstitialvideo:${libraryVersions["mintegral"]}")
    implementation("com.mbridge.msdk.oversea:mbbanner:${libraryVersions["mintegral"]}")
    implementation("com.mbridge.msdk.oversea:mbbid:${libraryVersions["mintegral"]}")
    implementation("com.mbridge.msdk.oversea:mbjscommon:${libraryVersions["mintegral"]}")
    implementation("com.mbridge.msdk.oversea:playercommon:${libraryVersions["mintegral"]}")
    implementation("com.mbridge.msdk.oversea:reward:${libraryVersions["mintegral"]}")
    implementation("com.mbridge.msdk.oversea:videocommon:${libraryVersions["mintegral"]}")
    implementation("com.mbridge.msdk.oversea:videojs:${libraryVersions["mintegral"]}")
    implementation("com.mbridge.msdk.oversea:mbnative:${libraryVersions["mintegral"]}")
    implementation("com.mbridge.msdk.oversea:dycreator:${libraryVersions["mintegral"]}")

    implementation("androidx.recyclerview:recyclerview:${libraryVersions["recyclerView"]}")
}

publishing {
    publications {
        create<MavenPublication>(extra["publicationName"] as String) {
            //The publication doesn"t know about our dependencies, so we have to manually add them to the pom
            pom.withXml {
                asNode().apply {
                    appendNode("name", libraryArtifactId)
                    appendNode("description", "Mintegral adapter for AppLovin MAX mediation")
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
                    // Add Mintegral SDKs to list of dependencies.
                    appendNode("dependencies").apply {
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.mbridge.msdk.oversea")
                            appendNode("artifactId", "same")
                            appendNode("version", libraryVersions["mintegral"])
                            appendNode("scope", "compile")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.mbridge.msdk.oversea")
                            appendNode("artifactId", "interstitial")
                            appendNode("version", libraryVersions["mintegral"])
                            appendNode("scope", "compile")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.mbridge.msdk.oversea")
                            appendNode("artifactId", "interstitialvideo")
                            appendNode("version", libraryVersions["mintegral"])
                            appendNode("scope", "compile")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.mbridge.msdk.oversea")
                            appendNode("artifactId", "mbbanner")
                            appendNode("version", libraryVersions["mintegral"])
                            appendNode("scope", "compile")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.mbridge.msdk.oversea")
                            appendNode("artifactId", "mbbid")
                            appendNode("version", libraryVersions["mintegral"])
                            appendNode("scope", "compile")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.mbridge.msdk.oversea")
                            appendNode("artifactId", "mbjscommon")
                            appendNode("version", libraryVersions["mintegral"])
                            appendNode("scope", "compile")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.mbridge.msdk.oversea")
                            appendNode("artifactId", "mbnative")
                            appendNode("version", libraryVersions["mintegral"])
                            appendNode("scope", "compile")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.mbridge.msdk.oversea")
                            appendNode("artifactId", "playercommon")
                            appendNode("version", libraryVersions["mintegral"])
                            appendNode("scope", "compile")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.mbridge.msdk.oversea")
                            appendNode("artifactId", "reward")
                            appendNode("version", libraryVersions["mintegral"])
                            appendNode("scope", "compile")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.mbridge.msdk.oversea")
                            appendNode("artifactId", "videocommon")
                            appendNode("version", libraryVersions["mintegral"])
                            appendNode("scope", "compile")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.mbridge.msdk.oversea")
                            appendNode("artifactId", "videojs")
                            appendNode("version", libraryVersions["mintegral"])
                            appendNode("scope", "compile")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.mbridge.msdk.oversea")
                            appendNode("artifactId", "dycreator")
                            appendNode("version", libraryVersions["mintegral"])
                            appendNode("scope", "compile")
                        }
                    }
                }
            }
        }
    }
}
