plugins {
    id("adapter-config")
    id("com.applovin.mobile.publish")
}

val libraryVersionName by extra("3.7.1.0")
val minAppLovinSdkVersion by extra("13.0.0")

android.defaultConfig.minSdk = 21

repositories {
    maven { url = uri("https://verve.jfrog.io/artifactory/verve-gradle-release") }
}
