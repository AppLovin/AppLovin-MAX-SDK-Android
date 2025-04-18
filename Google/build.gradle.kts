plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("24.2.0.1")
val minAppLovinSdkVersion by extra("13.0.0")

android.defaultConfig.minSdk = 23
