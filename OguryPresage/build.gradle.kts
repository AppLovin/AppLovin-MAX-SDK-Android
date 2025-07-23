plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("6.1.0.1")
val libraryArtifactId by extra("ogury-presage-adapter")

android.defaultConfig.minSdk = 21

repositories {
    maven { url = uri("https://maven.ogury.co") }
}
