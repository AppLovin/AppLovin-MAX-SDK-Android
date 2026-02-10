plugins {
    id("adapter-config")
    id("com.applovin.mobile.publish")
}

// NOTE: Mintegral has 2 separate SDK versions, e.g. x.x.51 for Google Play & x.x.52 for Android Market (in China)
val libraryVersionName by extra("17.0.91.0")
val minAppLovinSdkVersion by extra("13.0.0")

android.defaultConfig.minSdk = 16

repositories {
    maven { url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea") }
}
