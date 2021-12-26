plugins {
    id("signing")
    id("maven-publish")
}

private val versionMajor = 20
private val versionMinor = 5
private val versionPatch = 0
private val versionAdapterPatch = 3

val libraryVersionName by extra("${versionMajor}.${versionMinor}.${versionPatch}.${versionAdapterPatch}")
val libraryVersionCode by extra((versionMajor * 1000000) + (versionMinor * 10000) + (versionPatch * 100) + versionAdapterPatch)

val libraryArtifactId by extra("google-adapter")
val libraryGroupId by extra("com.applovin.mediation")

var libraryVersions = rootProject.extra["versions"] as Map<*, *>

android.defaultConfig.versionCode = libraryVersionCode
android.defaultConfig.versionName = libraryVersionName

dependencies {
    implementation("com.google.android.gms:play-services-ads:${libraryVersions["playServicesAds"]}")

    // Also required by MoPub and Inneractive to check for availability of Google Play Services APIs and retrieve the advertising ID
    implementation("com.google.android.gms:play-services-base:${libraryVersions["playServicesBase"]}")
}

publishing {
    publications {
        create<MavenPublication>(extra["publicationName"] as String) {
            // The publication doesn't know about our dependencies, so we have to manually add them to the pom
            pom.withXml {
                asNode().apply {
                    appendNode("name", libraryArtifactId)
                    appendNode("description", "Google AdMob adapter for AppLovin MAX mediation")
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
                    // Add Google AdMob to list of dependencies.
                    appendNode("dependencies")
                            .appendNode("dependency").apply {

                                appendNode("groupId", "com.google.android.gms")
                                appendNode("artifactId", "play-services-ads")
                                appendNode("version", libraryVersions["playServicesAds"])
                                appendNode("scope", "compile")
                            }
                }
            }
        }
    }
}
