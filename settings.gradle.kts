pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
        // Mudita Mindful Design (MMD) is published here. Adjust if Mudita moves it.
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "MindfulAnki"

include(":core")
include(":app")
