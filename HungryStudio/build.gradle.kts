plugins {
    id("com.android.library")
}

android {
    namespace = "com.applovin.mediation.adapters"
    compileSdk = 35

    defaultConfig {
        minSdk = 23

        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
}

dependencies {
    implementation ("com.applovin:applovin-sdk:13.3.1")
    implementation  ("com.hs.adx:core:1.1.67")
}


repositories {
    maven {
        url = uri("https://repo.pubmatic.com/artifactory/public-repos")
        credentials {
            username = "675fd60425be8f2424e20934"
            password = "y4)2tKp60VCW"
        }
    }
}
