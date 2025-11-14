plugins {
    id("adapter-config")
    id("com.applovin.mobile.publish")
}

val libraryVersionName by extra("5.6.2.0")

android.defaultConfig.minSdk = 16
