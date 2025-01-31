plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("6.4.0.6.1")

repositories {
    maven { url = uri("https://artifact.bytedance.com/repository/pangle") }
}

