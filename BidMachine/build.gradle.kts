plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("3.2.0.0")

android.defaultConfig.minSdk = 21

repositories {
    maven { url = uri("https://artifactory.bidmachine.io/bidmachine") }
}
