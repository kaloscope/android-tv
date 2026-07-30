pluginManagement {
    buildscript {
        repositories {
            mavenCentral()
            maven {
                url = uri("https://storage.googleapis.com/r8-releases/raw")
            }
        }
        dependencies {
            // Kotlin 2.4 metadata requires R8 9.1.29 or newer.
            classpath("com.android.tools:r8:9.1.29")
        }
    }
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    // Keep repository resolution deterministic without failing on user-level init scripts.
    repositoriesMode.set(RepositoriesMode.PREFER_SETTINGS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "KaloscopeTV"
include(":app")
