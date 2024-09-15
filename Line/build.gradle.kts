plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("2024.8.27.1")

android.defaultConfig.minSdk = 19
