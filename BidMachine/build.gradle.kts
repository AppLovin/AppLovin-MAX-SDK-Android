plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("3.0.1.2")

android.defaultConfig.minSdk = 21

repositories {
    maven { url = uri("https://artifactory.bidmachine.io/bidmachine") }
}
