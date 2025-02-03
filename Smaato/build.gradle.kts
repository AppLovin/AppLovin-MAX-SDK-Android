import com.applovin.build.extensions.appendDependency

plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("22.7.2.0")

repositories {
    maven { url = uri("https://s3.amazonaws.com/smaato-sdk-releases/") }
}

dependencies {
    implementation(libs.androidx.lifecycle.extensions)
}

publishing {
    publications {
        create<MavenPublication>("Adapter") {
            //The publication doesn't know about our dependencies, so we have to manually add them to the pom
            pom.withXml {
                asNode().apply {
                    // Add Smaato network to list of dependencies.
                    appendNode("dependencies")
                        .appendDependency(libs.androidx.lifecycle.extensions)
                }
            }
        }
    }
}
