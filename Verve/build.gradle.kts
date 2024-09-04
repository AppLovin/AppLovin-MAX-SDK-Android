plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("3.0.4.0")

android.defaultConfig.minSdk = 21

repositories {
    maven { url = uri("https://verve.jfrog.io/artifactory/verve-gradle-release") }
}

