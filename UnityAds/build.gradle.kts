plugins {
    id("adapter-config")
    id("com.applovin.mobile.publish")
}

val libraryVersionName by extra("4.16.6.0")
val minAppLovinSdkVersion by extra("13.0.0")

android.defaultConfig.minSdk = 19
