pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
    }

    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "org.jetbrains.kotlin.plugin.serialization" -> useVersion("2.0.0")
                "org.jetbrains.kotlin.multiplatform" -> useVersion("2.0.0")
                "org.jetbrains.compose" -> useVersion("1.7.0")
            }
        }
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

rootProject.name = "2_Android_bluetooth_master_stradivary"

include(":app")
include(":bluetooth")
include(":delegateadapter")
include(":shared")
