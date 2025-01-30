plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("23.6.0.3")

val libraryArtifactId by extra("google-ad-manager-adapter")

android.defaultConfig.minSdk = 21

