plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("8.4.0.0.1")

android.defaultConfig.minSdk = 19

repositories {
    maven { url = uri("https://android-sdk.is.com/") }
}

