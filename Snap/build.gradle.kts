plugins {
    id("signing")
    id("maven-publish")
}

private val versionMajor = 2
private val versionMinor = 3
private val versionPatch = 2
private val versionBuild = 0
private val versionAdapterPatch = 1

val libraryVersionName by extra("${versionMajor}.${versionMinor}.${versionPatch}.${versionBuild}.${versionAdapterPatch}")
val libraryVersionCode by extra((versionMajor * 100000000) + (versionMinor * 1000000) + (versionPatch * 10000) + (versionBuild * 100) + versionAdapterPatch)

val libraryArtifactId by extra("snap-adapter")
val libraryGroupId by extra("com.applovin.mediation")

var libraryVersions = rootProject.extra["versions"] as Map<*, *>

android.defaultConfig.versionCode = libraryVersionCode
android.defaultConfig.versionName = libraryVersionName

dependencies {
    implementation("com.snap.adkit:adkit:${libraryVersions["snap"]}")
    implementation("androidx.constraintlayout:constraintlayout:1.1.3")
    implementation("com.squareup.picasso:picasso:${libraryVersions["picasso"]}")
}

publishing {
    publications {
        create<MavenPublication>(extra["publicationName"] as String) {
            //The publication doesn't know about our dependencies, so we have to manually add them to the pom
            pom.withXml {
                asNode().apply {
                    appendNode("name", libraryArtifactId)
                    appendNode("description", "Snap adapter for AppLovin MAX mediation")
                    appendNode("url", "http://www.applovin.com/")
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
                    // Add Snap network to list of dependencies.
                    appendNode("dependencies")
                            .appendNode("dependency").apply {
                                appendNode("groupId", "com.snap.adkit")
                                appendNode("artifactId", "adkit")
                                appendNode("version", libraryVersions["snap"])
                                appendNode("type", "aar")
                                appendNode("scope", "compile")
                            }
                }
            }
        }
    }
}
