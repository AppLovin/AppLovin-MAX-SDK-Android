plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("7.5.0.1")

android.defaultConfig.minSdk = 21
