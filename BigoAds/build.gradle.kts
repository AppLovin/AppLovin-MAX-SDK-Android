plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("5.0.2.0")
