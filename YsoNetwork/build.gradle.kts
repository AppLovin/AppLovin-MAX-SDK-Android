plugins {
    id("adapter-config")
    id("com.applovin.mobile.publish")
}

val libraryVersionName by extra("1.3.3.0")

applovinMobilePublish {
    libraryArtifactId.set("yso-network-adapter")
}

android.defaultConfig.minSdk = 21

repositories {
    maven { url = uri("https://ysonetwork.s3.eu-west-3.amazonaws.com/sdk/android") }
}
