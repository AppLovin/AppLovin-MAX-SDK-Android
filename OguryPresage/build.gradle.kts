plugins {
    id("adapter-config")
    id("com.applovin.mobile.publish")
}

val libraryVersionName by extra("6.2.1.0")

applovinMobilePublish {
    libraryArtifactId.set("ogury-presage-adapter")
}

android.defaultConfig.minSdk = 21

repositories {
    maven { url = uri("https://maven.ogury.co") }
}
