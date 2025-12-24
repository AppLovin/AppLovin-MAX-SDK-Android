plugins {
    id("adapter-config")
    id("com.applovin.mobile.publish")
}

val libraryVersionName by extra("8.4.2.0")
val minAppLovinSdkVersion by extra("13.2.0")

android.defaultConfig.minSdk = 19
