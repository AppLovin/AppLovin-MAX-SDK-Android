plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("6.4.1.0")

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:+")
}
