plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("4.4.0.0")

android.defaultConfig.minSdk = 19

repositories {
    maven { url = uri("https://repo.pubmatic.com/artifactory/public-repos") }
}
