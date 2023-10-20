plugins {
    id("signing")
    id("maven-publish")
}

private val versionMajor = 6
private val versionMinor = 1
private val versionPatch = 0
private val versionAdapterPatch = 0

val libraryVersionName by extra("${versionMajor}.${versionMinor}.${versionPatch}.${versionAdapterPatch}")
val libraryVersionCode by extra((versionMajor * 1000000) + (versionMinor * 10000) + (versionPatch * 100) + versionAdapterPatch)

val libraryArtifactId by extra("yandex-adapter")
val libraryGroupId by extra("com.applovin.mediation")

var libraryVersions = rootProject.extra["versions"] as Map<*, *>

android.defaultConfig.versionCode = libraryVersionCode
android.defaultConfig.versionName = libraryVersionName
// Ignore lint error that Yandex throws when incorrectly parsing their SDK version below.
android.lintOptions.isAbortOnError = false

// Suppress lint because Gradle does not detect the correct `yandex` version
@Suppress("MobileAdsSdkOutdatedVersion") dependencies {
    implementation("com.yandex.android:mobileads:${libraryVersions["yandex"]}")
    implementation("com.yandex.android:mobmetricalib:${libraryVersions["yandexMobmetrica"]}")
}

publishing {
    publications {
        create<MavenPublication>(extra["publicationName"] as String) {
            //The publication doesn't know about our dependencies, so we have to manually add them to the pom
            pom.withXml {
                asNode().apply {
                    appendNode("name", libraryArtifactId)
                    appendNode("description", "Yandex adapter for AppLovin MAX mediation")
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
                    // Add Yandex to list of dependencies.
                    appendNode("dependencies").apply {
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.yandex.android")
                            appendNode("artifactId", "mobileads")
                            appendNode("version", libraryVersions["yandex"])
                            appendNode("scope", "compile")
                        }
                        appendNode("dependency").apply {
                            appendNode("groupId", "com.yandex.android")
                            appendNode("artifactId", "mobmetricalib")
                            appendNode("version", libraryVersions["yandexMobmetrica"])
                            appendNode("scope", "compile")
                        }
                    }
                }
            }
        }
    }
}
