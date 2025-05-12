// Top-level Gradle build file where you can add global configuration settings.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false


    // Google Services Gradle Plugin
    id("com.google.gms.google-services") version "4.4.0" apply false
}

buildscript {
    dependencies {
        classpath ("com.android.tools.build:gradle:8.2.1")
        classpath("com.google.gms:google-services:4.4.2") // Use the latest stable version
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.24") // Ensure the correct Kotlin version
    }
}

tasks.register("clean", Delete::class) {
    delete(rootProject.buildDir)
}
