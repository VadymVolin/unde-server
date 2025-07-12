plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.ktor.plugin)
    alias(libs.plugins.kotlin.serialization)
}

application {
    mainClass.set("io.ktor.server.netty.EngineMain")

    val isDevelopment: Boolean = project.ext.has("development")
    applicationDefaultJvmArgs = listOf("-Dio.ktor.development=$isDevelopment")
}

dependencies {
    implementation(libs.bundles.ktor)
    implementation(libs.kotlin.serialization.kotlinx.json)
    implementation(libs.kotlin.html)
    // database
    implementation(libs.exposed.core)
    implementation(libs.exposed.jdbc)
    // h2
    implementation(libs.h2)
    //
    implementation(libs.logback)

    testImplementation(libs.test.ktor.server.host)
    testImplementation(libs.kotlin.test.junit)
}
