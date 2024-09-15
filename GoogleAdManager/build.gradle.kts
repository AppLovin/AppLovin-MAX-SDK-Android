plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("23.3.0.1")
val libraryArtifactId by extra("google-ad-manager-adapter")

android.defaultConfig.minSdk = 21

