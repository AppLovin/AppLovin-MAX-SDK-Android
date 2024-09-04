plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

// NOTE: Mintegral has 2 separate SDK versions, e.g. x.x.51 for Google Play & x.x.52 for Android Market (in China)
val libraryVersionName by extra("16.8.41.0")

repositories {
    maven { url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea") }
}
