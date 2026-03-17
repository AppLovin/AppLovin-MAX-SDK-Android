plugins {
    id("adapter-config")
    id("com.applovin.mobile.publish")
}

val libraryVersionName by extra("23.0.1.0")
val minAppLovinSdkVersion by extra("13.0.0")

applovinMobilePublish {
    pomDependencies.set(com.applovin.gradle.PomDependency.fromList(listOf(
        libs.androidx.lifecycle.extensions
    )))
}

android.defaultConfig.minSdk = 21

repositories {
    maven { url = uri("https://s3.amazonaws.com/smaato-sdk-releases/") }
}

dependencies {
    implementation(libs.androidx.lifecycle.extensions)
}
