plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("6.1.0.9.0")

repositories {
    maven { url = uri("https://artifact.bytedance.com/repository/pangle") }
}

