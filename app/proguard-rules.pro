# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Quantum Engine classes
-keep class com.quantum.engine.** { *; }

# Keep Kotlin
-keep class kotlin.** { *; }
-keep class kotlinx.** { *; }

# Keep OpenGL
-keep class android.opengl.** { *; }
-keep class javax.microedition.khronos.** { *; }

# Keep Timber
-keep class timber.log.** { *; }
