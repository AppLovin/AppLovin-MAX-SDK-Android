import com.applovin.build.extensions.appendDependencyBundle

plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("2.0.0.0")

repositories {
    maven { url = uri("https://imobile-maio.github.io/maven") }
}
