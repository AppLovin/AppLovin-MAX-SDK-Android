plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("24.0.0.0")

val libraryArtifactId by extra("google-ad-manager-adapter")

android.defaultConfig.minSdk = 23

