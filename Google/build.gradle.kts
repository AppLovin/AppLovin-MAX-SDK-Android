plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("23.6.0.3")

android.defaultConfig.minSdk = 21
