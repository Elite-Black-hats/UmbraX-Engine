pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "QuantumEngine"

// Core modules
include(":qe-core")
include(":qe-math")
include(":qe-platform")

// Rendering modules
include(":qe-renderer-common")
include(":qe-renderer-vulkan")
include(":qe-renderer-gles")

// Engine systems
include(":qe-physics")
include(":qe-audio")
include(":qe-animation")
include(":qe-ai")
include(":qe-networking")
include(":qe-scripting")

// Asset and content
include(":qe-assets")
include(":qe-terrain")
include(":qe-particles")
include(":qe-serialization")

// Input and UI
include(":qe-input")
include(":qe-ui-runtime")

// Editor
include(":qe-editor")

// Sample application
include(":app")
