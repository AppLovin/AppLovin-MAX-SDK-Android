plugins {
    id("adapter-config")
    id("com.applovin.mobile.publish")
}

val libraryVersionName by extra("2025.7.18.1")
val minAppLovinSdkVersion by extra("13.0.0")

android.defaultConfig.minSdk = 19
