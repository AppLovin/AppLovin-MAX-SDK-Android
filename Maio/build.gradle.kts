import com.applovin.build.extensions.appendDependencyBundle

plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("1.1.16.3")

repositories {
    maven { url = uri("https://imobile-maio.github.io/maven") }
}
