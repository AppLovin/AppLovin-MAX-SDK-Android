plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("0.0.6.2")
val libraryGroupId by extra("com.applovin.dsp")

android.defaultConfig.minSdk = 21
