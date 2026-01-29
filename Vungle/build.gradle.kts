plugins {
    id("adapter-config")
    id("com.applovin.mobile.publish")
}

val libraryVersionName by extra("7.7.0.0")
val minAppLovinSdkVersion by extra("13.0.0")

android.defaultConfig.minSdk = 16
