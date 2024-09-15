plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("9.7.0.3")

repositories {
    maven { url = uri("https://cboost.jfrog.io/artifactory/chartboost-ads/") }
}

