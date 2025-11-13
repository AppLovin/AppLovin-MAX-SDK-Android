plugins {
    id("adapter-config")
    id("com.applovin.mobile.publish")
}

val libraryVersionName by extra("9.10.2.0")
val minAppLovinSdkVersion by extra("13.0.0")

android.defaultConfig.minSdk = 19

repositories {
    maven { url = uri("https://cboost.jfrog.io/artifactory/chartboost-ads/") }
}
