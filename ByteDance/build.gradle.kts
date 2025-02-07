plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("6.5.0.3.0")

repositories {
    maven { url = uri("https://artifact.bytedance.com/repository/pangle") }
}

