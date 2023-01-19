plugins {
    id("signing")
    id("maven-publish")
}

private val versionMajor = 7
private val versionMinor = 2
private val versionPatch = 7
private val versionBuild = 0
private val versionAdapterPatch = 0

val libraryVersionName by extra("${versionMajor}.${versionMinor}.${versionPatch}.${versionBuild}.${versionAdapterPatch}")
val libraryVersionCode by extra((versionMajor * 100000000) + (versionMinor * 1000000) + (versionPatch * 10000) + (versionBuild * 100) + versionAdapterPatch)

val libraryArtifactId by extra("ironsource-adapter")
val libraryGroupId by extra("com.applovin.mediation")

var libraryVersions = rootProject.extra["versions"] as Map<*, *>

android.defaultConfig.versionCode = libraryVersionCode
android.defaultConfig.versionName = libraryVersionName

dependencies {
    implementation("com.ironsource.sdk:mediationsdk:${libraryVersions["ironsource"]}")
}

repositories {
    maven { url = uri("https://android-sdk.is.com/") }
}

publishing {
    publications {
        create<MavenPublication>(extra["publicationName"] as String) {
            //The publication doesn't know about our dependencies, so we have to manually add them to the pom
            pom.withXml {
                asNode().apply {

                    appendNode("name", libraryArtifactId)
                    appendNode("description", "IronSource adapter for AppLovin MAX mediation")
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
                    appendNode("dependencies")
                            .appendNode("dependency").apply {

                                appendNode("groupId", "com.ironsource.sdk")
                                appendNode("artifactId", "mediationsdk")
                                appendNode("version", libraryVersions["ironsource"])
                                appendNode("type", "aar")
                                appendNode("scope", "compile")
                            }
                }
            }
        }
    }
}
