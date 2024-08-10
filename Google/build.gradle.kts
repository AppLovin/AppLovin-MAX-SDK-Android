plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("23.2.0.1")

android.defaultConfig.minSdk = 21
