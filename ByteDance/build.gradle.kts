plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("6.5.0.8.1")
val minAppLovinSdkVersion by extra("13.2.0")

android.defaultConfig.minSdk = 19

repositories {
    maven { url = uri("https://artifact.bytedance.com/repository/pangle") }
}
