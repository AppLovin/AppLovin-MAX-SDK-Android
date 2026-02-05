plugins {
    id("adapter-config")
    id("com.applovin.mobile.publish")
}

val libraryVersionName by extra("7.8.6.0.0")
val minAppLovinSdkVersion by extra("13.2.0")

android.defaultConfig.minSdk = 19

repositories {
    maven { url = uri("https://artifact.bytedance.com/repository/pangle") }
}
