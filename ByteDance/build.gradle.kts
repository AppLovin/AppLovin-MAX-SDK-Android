plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("7.3.0.4.0")
val minAppLovinSdkVersion by extra("13.2.0")

android.defaultConfig.minSdk = 19

repositories {
    maven { url = uri("https://artifact.bytedance.com/repository/pangle") }
}
