

//val composeVersion: String = settings.findProperty("compose.version") as String
//val composeVersion: String  = providers.gradleProperty("compose.version").get()

pluginManagement {
    val kotlinVersion: String = providers.gradleProperty("kotlin.version").get()
    val composeVersion: String = providers.gradleProperty("compose.version").get()
    val agpVersion: String = providers.gradleProperty("agp.version").get()

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

    plugins {
        kotlin("jvm").version(kotlinVersion)
        kotlin("multiplatform").version(kotlinVersion)
        kotlin("plugin.compose").version(kotlinVersion)
        kotlin("plugin.serialization").version(kotlinVersion)
        kotlin("android").version(kotlinVersion)
        id("com.android.base").version(agpVersion)
        id("com.android.application").version(agpVersion)
        id("com.android.library").version(agpVersion)
        id("org.jetbrains.compose").version(composeVersion)
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
