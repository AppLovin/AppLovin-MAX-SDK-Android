plugins {
    id("signing")
    id("maven-publish")
}

private val versionMajor = 2
private val versionMinor = 1
private val versionPatch = 1
private val versionAdapterPatch = 1

val libraryVersionName by extra("${versionMajor}.${versionMinor}.${versionPatch}.${versionAdapterPatch}")
val libraryVersionCode by extra((versionMajor * 1000000) + (versionMinor * 10000) + (versionPatch * 100) + versionAdapterPatch)

val libraryArtifactId by extra("moloco-adapter")
val libraryGroupId by extra("com.applovin.mediation")

var libraryVersions = rootProject.extra["versions"] as Map<*, *>

android.namespace = "com.applovin.mediation.adapters.moloco"
android.defaultConfig.versionCode = libraryVersionCode
android.defaultConfig.versionName = libraryVersionName
android.defaultConfig.minSdk = 21

dependencies {
    implementation("com.moloco.sdk:moloco-sdk:${libraryVersions["moloco"]}")
    compileOnly("androidx.annotation:annotation:${libraryVersions["annotation"]}")
}

publishing {
    publications {
        create<MavenPublication>(extra["publicationName"] as String) {
            // The publication doesn't know about our dependencies, so we have to manually add them to the pom
            pom.withXml {
                asNode().apply {
                    appendNode("name", libraryArtifactId)
                    appendNode("description", "Moloco adapter for AppLovin MAX mediation")
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
                    // Add Moloco network to list of dependencies.
                    appendNode("dependencies")
                            .appendNode("dependency").apply {
                                appendNode("groupId", "com.moloco.sdk")
                                appendNode("artifactId", "moloco-sdk")
                                appendNode("version", libraryVersions["moloco"])
                                appendNode("scope", "compile")
                            }
                }
            }
        }
    }
}
