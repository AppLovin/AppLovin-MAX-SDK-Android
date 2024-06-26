plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("2024.5.15.0")

android.defaultConfig.minSdk = 19
