plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("7.15.0.0")
val minAppLovinSdkVersion by extra("13.0.0")

android.defaultConfig.minSdk = 21
