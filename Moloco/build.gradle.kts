plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("2.3.0.0")

android.defaultConfig.minSdk = 21
