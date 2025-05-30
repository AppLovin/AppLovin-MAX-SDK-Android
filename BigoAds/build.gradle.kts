plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("5.3.0.2")

android.defaultConfig.minSdk = 16
