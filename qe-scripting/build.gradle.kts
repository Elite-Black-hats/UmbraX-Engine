plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.quantum.engine.scripting"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
    }

    buildTypes {
        release {
            isMinifyEnabled = false
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation(project(":qe-core"))
    implementation(project(":qe-math"))
    
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.0.0")
    implementation("org.jetbrains.kotlin:kotlin-scripting-jvm:2.0.0")
    implementation("org.jetbrains.kotlin:kotlin-compiler-embeddable:2.0.0")
    
    // JavaScript - Rhino
    implementation("org.mozilla:rhino:1.7.14")
    
    // Lua - LuaJ
    implementation("org.luaj:luaj-jse:3.0.1")
    
    // Python - Chaquopy (Android Python)
    // Se configura por separado en el proyecto
    
    implementation("com.jakewharton.timber:timber:5.0.1")
}
