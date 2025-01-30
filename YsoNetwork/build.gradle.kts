plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("1.2.7.1")
val libraryArtifactId by extra("yso-network-adapter")

android.defaultConfig.minSdk = 21

repositories {
    maven { url = uri("https://ysonetwork.s3.eu-west-3.amazonaws.com/sdk/android") }
}
