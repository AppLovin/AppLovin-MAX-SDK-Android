plugins {
    id("signing")
    id("maven-publish")
}

private val versionMajor = 13
private val versionMinor = 4
private val versionPatch = 61
private val versionBuild = 302
private val versionAdapterPatch = 0

val libraryVersionName by extra("${versionMajor}.${versionMinor}.${versionPatch}.${versionBuild}.${versionAdapterPatch}")
val libraryVersionCode by extra((versionMajor * 1000000000) + (versionMinor * 10000000) + (versionPatch * 100000) + (versionBuild * 100) + versionAdapterPatch)

val libraryArtifactId by extra("huawei-adapter")
val libraryGroupId by extra("com.applovin.mediation")

var libraryVersions = rootProject.extra["versions"] as Map<*, *>

android.defaultConfig.versionCode = libraryVersionCode
android.defaultConfig.versionName = libraryVersionName

repositories {
    maven { url = uri("https://developer.huawei.com/repo/") }
}

dependencies {
    implementation("com.huawei.hms:ads-prime:${libraryVersions["huawei"]}")
    implementation("com.huawei.hms:ads-consent:${libraryVersions["huawei"]}")
}

publishing {
    publications {
        create<MavenPublication>(extra["publicationName"] as String) {
            //The publication doesn't know about our dependencies, so we have to manually add them to the pom
            pom.withXml {
                asNode().apply {
                    appendNode("name", libraryArtifactId)
                    appendNode("description", "Huawei adapter for AppLovin MAX mediation")
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
                    // Add Huawei to list of dependencies.
                    appendNode("dependencies").apply {
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.huawei.hms")
                            appendNode("artifactId", "ads-prime")
                            appendNode("version", libraryVersions["huawei"])
                            appendNode("type", "aar")
                            appendNode("scope", "compile")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.huawei.hms")
                            appendNode("artifactId", "ads-consent")
                            appendNode("version", libraryVersions["huawei"])
                            appendNode("type", "aar")
                            appendNode("scope", "compile")
                        }
                    }
                }
            }
        }
    }
}
