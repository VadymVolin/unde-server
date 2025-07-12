plugins {
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ktor.plugin) apply false
}

subprojects {
    repositories {
        mavenCentral()
    }

    group = "com.unde.server"
    version = "0.0.1"
}
