plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("1.0.0.0")
val minAppLovinSdkVersion by extra("13.0.0")

android.defaultConfig.minSdk = 24

dependencies {
    // BidsCube ARR library
    implementation("com.bidscube:bidscube-sdk:1.0.2@aar")

    // Necessary libraries to test BidsCube SDK in Android
    implementation("com.google.android.ump:user-messaging-platform:2.1.0")
    implementation("com.google.android.gms:play-services-ads-identifier:18.0.1")
    implementation("com.google.ads.interactivemedia.v3:interactivemedia:3.37.0")
    implementation("androidx.media3:media3-ui:1.8.0")
    implementation("androidx.cardview:cardview:1.0.0")
    implementation("com.google.android.material:material:1.11.0")
}
