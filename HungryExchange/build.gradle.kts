plugins {
//    id("com.android.library")
    id("adapter-config")
    id("com.applovin.mobile.publish")
}


val libraryVersionName by extra("1.1.73.0")
val minAppLovinSdkVersion by extra("13.3.1")

android.defaultConfig.minSdk = 23

//android {
//    namespace = "com.applovin.mediation.adapters"
//    compileSdk = 35
//
//    defaultConfig {
//        minSdk = 23
//
//        consumerProguardFiles("consumer-rules.pro")
//    }
//
//    buildTypes {
//        release {
//            isMinifyEnabled = false
//            proguardFiles(
//                getDefaultProguardFile("proguard-android-optimize.txt"),
//                "proguard-rules.pro"
//            )
//        }
//    }
//    compileOptions {
//        sourceCompatibility = JavaVersion.VERSION_11
//        targetCompatibility = JavaVersion.VERSION_11
//    }
//}

dependencies {
    implementation ("com.applovin:applovin-sdk:13.3.1")
    implementation  ("com.hs.adx:core:1.1.73")
}


repositories {
    maven {
        url = uri("https://packages.aliyun.com/6639f75d4977ded33af8fb60/maven/repo-hmcxt")
        credentials {
            username = "675fd60425be8f2424e20934"
            password = "y4)2tKp60VCW"
        }
    }
}
