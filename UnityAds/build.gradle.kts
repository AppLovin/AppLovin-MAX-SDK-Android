plugins {
    id("adapter-config")
    id("com.applovin.mobile.publish")
}

val libraryVersionName by extra("4.22.0.1")
val minAppLovinSdkVersion by extra("13.0.0")

android.defaultConfig.minSdk = 19
