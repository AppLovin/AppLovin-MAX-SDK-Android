plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("24.1.0.0")

android.defaultConfig.minSdk = 23
