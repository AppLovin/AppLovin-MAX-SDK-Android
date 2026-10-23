plugins {
    id("adapter-config")
    id("com.applovin.mobile.publish")
}

val libraryVersionName by extra("1.2.1.0")
val minAppLovinSdkVersion by extra("13.0.0")

applovinMobilePublish {
    libraryArtifactId.set("google-next-gen-ad-manager-adapter")
}

android.defaultConfig.minSdk = 24
