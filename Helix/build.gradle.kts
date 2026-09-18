plugins {
    id("adapter-config")
    id("com.applovin.mobile.publish")
    id("org.jetbrains.kotlin.android")
}

val libraryVersionName by extra("4.28.3.0")
val minAppLovinSdkVersion by extra("13.0.0")

android.defaultConfig.minSdk = 26

repositories {
    maven { url = uri("https://framework.voodoo-adn.com/android/release/apps") }
}

dependencies {
    api("io.adn:adn-sdk:4.28.3")
    implementation("org.jetbrains.kotlin:kotlin-stdlib")
}
