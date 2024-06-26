import com.applovin.build.extensions.appendDependency

plugins {
    id("adapter-config")
}

afterEvaluate {
    apply(plugin = "adapter-publish")
}

val libraryVersionName by extra("9.10.1.0")
val libraryArtifactId by extra("amazon-tam-adapter")

android.defaultConfig.minSdk = 19

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.iabtcf)
}

publishing {
    publications {
        create<MavenPublication>("Adapter") {
            //The publication doesn't know about our dependencies, so we have to manually add them to the pom
            pom.withXml {
                asNode().apply {
                    appendNode("dependencies")
                        .appendDependency(libs.iabtcf)
                }
            }
        }
    }
}
