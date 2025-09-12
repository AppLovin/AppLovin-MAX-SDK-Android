plugins {
    id("adapter-config")
    id("com.applovin.mobile.publish")
}

val libraryVersionName by extra("24.6.0.0")
val libraryArtifactId by extra("google-ad-manager-adapter")
val minAppLovinSdkVersion by extra("13.0.0")

android.defaultConfig.minSdk = 23
