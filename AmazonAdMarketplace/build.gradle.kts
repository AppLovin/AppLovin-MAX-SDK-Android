plugins {
    id("adapter-config")
    id("com.applovin.mobile.publish")
}

val libraryVersionName by extra("11.1.1.0")

applovinMobilePublish {
    libraryArtifactId.set("amazon-tam-adapter")
    pomDependencies.set(com.applovin.gradle.PomDependency.fromList(listOf(
        libs.iabtcf
    )))
}

android.defaultConfig.minSdk = 19

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.iabtcf)
}
