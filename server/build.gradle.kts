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

// Creates platform-specific installer (msi/dmg/deb) using jpackage
tasks.register<Exec>("createPackage") {
    dependsOn("installDist")
    
    val appName = "Unde Server " + project.version.toString()
    val appVersion = project.version.toString()
    val distDir = layout.buildDirectory.dir("install/server").get().asFile
    val outputDir = layout.buildDirectory.dir("jpackage").get().asFile
    
    doFirst {
        outputDir.deleteRecursively()
        outputDir.mkdirs()
    }
    
    // Detect OS and choose installer type
    val os = System.getProperty("os.name").lowercase()
    val installerType = when {
        os.contains("win") -> "exe"  // exe doesn't require WiX, msi does
        os.contains("mac") -> "dmg"
        else -> "deb"
    }
    
    val iconPath = when {
        os.contains("win") -> "scripts/resources/icon.ico"
        os.contains("mac") -> "scripts/resources/icon.icns"
        else -> "scripts/resources/icon.png"
    }
    
    val iconFile = file("${rootProject.projectDir}/$iconPath")
    val baseCommand = mutableListOf(
        "jpackage",
        "--type", installerType,
        "--icon", if (iconFile.exists()) iconFile.absolutePath else "",
        "--name", appName,
        "--app-version", appVersion,
        "--vendor", "Unde",
        "--description", "Unde Server Application",
        "--input", "${distDir}/lib",
        "--main-jar", "server-${appVersion}.jar",
        "--main-class", "io.ktor.server.netty.EngineMain",
        "--dest", outputDir.absolutePath,
        "--java-options", "-Dio.ktor.development=false"
    )
    
    commandLine(baseCommand)
}

// Creates portable application folder without installer
tasks.register<Exec>("createPortable") {
    dependsOn("installDist")
    
    val appName = "Unde Server " + project.version.toString()
    val appVersion = project.version.toString()
    val distDir = layout.buildDirectory.dir("install/server").get().asFile
    val outputDir = layout.buildDirectory.dir("jpackage").get().asFile
    
    doFirst {
        outputDir.deleteRecursively()
        outputDir.mkdirs()
    }
    
    val os = System.getProperty("os.name").lowercase()
    val iconPath = when {
        os.contains("win") -> "scripts/resources/icon.ico"
        os.contains("mac") -> "scripts/resources/icon.icns"
        else -> "scripts/resources/icon.png"
    }
    
    val iconFile = file("${rootProject.projectDir}/$iconPath")
    val baseCommand = mutableListOf(
        "jpackage",
        "--type", "app-image",
        "--icon", if (iconFile.exists()) iconFile.absolutePath else "",
        "--name", appName,
        "--app-version", appVersion,
        "--vendor", "Unde",
        "--input", "${distDir}/lib",
        "--main-jar", "server-${appVersion}.jar",
        "--main-class", "io.ktor.server.netty.EngineMain",
        "--dest", outputDir.absolutePath,
        "--java-options", "-Dio.ktor.development=false"
    )
    
    commandLine(baseCommand)
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
