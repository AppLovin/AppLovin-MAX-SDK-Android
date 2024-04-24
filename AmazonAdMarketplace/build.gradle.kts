plugins {
    id("signing")
    id("maven-publish")
}

private val versionMajor = 9
private val versionMinor = 9
private val versionPatch = 5
private val versionAdapterPatch = 0

val libraryVersionName by extra("${versionMajor}.${versionMinor}.${versionPatch}.${versionAdapterPatch}")
val libraryVersionCode by extra((versionMajor * 1000000) + (versionMinor * 10000) + (versionPatch * 100) + versionAdapterPatch)

val libraryArtifactId by extra("amazon-tam-adapter")
val libraryGroupId by extra("com.applovin.mediation")

var libraryVersions = rootProject.extra["versions"] as Map<*, *>

android.namespace = "com.applovin.mediation.adapters.amazonadmarketplace"
android.defaultConfig.versionCode = libraryVersionCode
android.defaultConfig.versionName = libraryVersionName
android.defaultConfig.minSdk = 19

dependencies {
    api("com.amazon.android:aps-sdk:${libraryVersions["amazonAdMarketplace"]}")
    compileOnly("com.android.support:support-annotations:+")
    implementation("androidx.appcompat:appcompat:${libraryVersions["appcompat"]}")
    implementation("com.iabtcf:iabtcf-decoder:2.0.10")
}

publishing {
    publications {
        create<MavenPublication>(extra["publicationName"] as String) {
            //The publication doesn't know about our dependencies, so we have to manually add them to the pom
            pom.withXml {
                asNode().apply {
                    appendNode("name", libraryArtifactId)
                    appendNode("description", "Amazon TAM adapter for AppLovin MAX mediation")
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
                    // NOTE: Amazon requested us to not add them to the list of transitive dependencies. External pubs have to manually integrate their SDK.
                    appendNode("dependencies")
                        .appendNode("dependency").apply {
                            appendNode("groupId", "com.iabtcf")
                            appendNode("artifactId", "iabtcf-decoder")
                            appendNode("version", "2.0.10")
                            appendNode("scope", "compile")
                        }
                }
            }
        }
    }
}
