plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("8.3.6.1")
val minAppLovinSdkVersion by extra("13.2.0")

android.defaultConfig.minSdk = 19
