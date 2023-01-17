plugins {
    id("signing")
    id("maven-publish")
}

private val versionMajor = 10
private val versionMinor = 1
private val versionPatch = 2
private val versionAdapterPatch = 4

val libraryVersionName by extra("${versionMajor}.${versionMinor}.${versionPatch}.${versionAdapterPatch}")
val libraryVersionCode by extra((versionMajor * 1000000) + (versionMinor * 10000) + (versionPatch * 100) + versionAdapterPatch)

val libraryArtifactId by extra("inmobi-adapter")
val libraryGroupId by extra("com.applovin.mediation")

var libraryVersions = rootProject.extra["versions"] as Map<*, *>

android.defaultConfig.versionCode = libraryVersionCode
android.defaultConfig.versionName = libraryVersionName

dependencies {
    implementation("com.inmobi.monetization:inmobi-ads:${libraryVersions["inmobi"]}")

    // The InMobi SDK for Android uses the popular Picasso library for loading the ad assets for interstitials
    implementation("com.squareup.picasso:picasso:${libraryVersions["picasso"]}")

    // Failure to include RecyclerView dependency in your application gradle scripts will cause interstitial ad requests to fail, thus affecting monetization of your app with the InMobi SDK
    implementation("com.android.support:recyclerview-v7:28.0.0")

    // https://support.inmobi.com/monetize/android-guidelines#h3-null-adding-and-verifying-the-dependenci
    // Failure to include Chrome Custom Tab dependency in your application gradle scripts will cause ad requests to fail, thus affecting monetization of your app with the InMobi SDK.
    implementation("com.android.support:customtabs:28.0.0")
}

publishing {
    publications {
        create<MavenPublication>(extra["publicationName"] as String) {
            // The publication doesn't know about our dependencies, so we have to manually add them to the pom
            pom.withXml {
                asNode().apply {

                    appendNode("name", libraryArtifactId)
                    appendNode("description", "InMobi adapter for AppLovin MAX mediation")
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
                    // Add InMobi to list of dependencies.
                    appendNode("dependencies")
                            .appendNode("dependency").apply {
                                appendNode("groupId", "com.inmobi.monetization")
                                appendNode("artifactId", "inmobi-ads")
                                appendNode("version", libraryVersions["inmobi"])
                                appendNode("scope", "compile")
                            }
                }
            }
        }
    }
}
