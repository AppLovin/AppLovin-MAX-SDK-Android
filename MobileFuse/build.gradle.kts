plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("1.8.2.1")

android.defaultConfig.minSdk = 19

