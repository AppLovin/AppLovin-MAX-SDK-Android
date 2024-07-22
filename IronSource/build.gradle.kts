plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("8.2.1.0.0")

android.defaultConfig.minSdk = 19

repositories {
    maven { url = uri("https://android-sdk.is.com/") }
}

