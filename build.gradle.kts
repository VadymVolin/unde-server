plugins {
    kotlin("jvm") version "2.1.0" apply false
    kotlin("multiplatform") version "2.1.0" apply false
    id("io.ktor.plugin") version "3.1.0" apply false
    id("org.jetbrains.kotlin.plugin.serialization") version "2.1.0" apply false
}

subprojects {
    repositories {
        mavenCentral()
    }

    group = "com.unde.server"
    version = "0.0.1"
}
