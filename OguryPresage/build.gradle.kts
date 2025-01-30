plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("6.0.1.1")
val libraryArtifactId by extra("ogury-presage-adapter")

repositories {
    maven { url = uri("https://maven.ogury.co") }
}

