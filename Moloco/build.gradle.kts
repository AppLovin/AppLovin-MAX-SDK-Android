plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("3.0.2.0")

android.defaultConfig.minSdk = 21
