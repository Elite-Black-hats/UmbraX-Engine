plugins {
    id("com.android.application")
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

repositories {
    google()
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}