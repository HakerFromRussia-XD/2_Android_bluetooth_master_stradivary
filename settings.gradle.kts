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
                "org.jetbrains.compose" -> useVersion("1.8.2")
                "org.jetbrains.kotlin.plugin.compose" -> useVersion("2.0.0")
            }
        }
    }

//    plugins {
//        val kotlinVersion = extra["kotlin.version"] as String
//        val agpVersion = extra["agp.version"] as String
//        val composeVersion = extra["compose.version"] as String
//
//        kotlin("jvm").version(kotlinVersion)
//        kotlin("multiplatform").version(kotlinVersion)
//        kotlin("plugin.compose").version(kotlinVersion)
//        kotlin("plugin.serialization").version(kotlinVersion)
//        kotlin("android").version(kotlinVersion)
//        id("com.android.base").version(agpVersion)
//        id("com.android.application").version(agpVersion)
//        id("com.android.library").version(agpVersion)
//        id("org.jetbrains.compose").version(composeVersion)
//    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven(url = "https://jitpack.io")
    }
}

//plugins {
//    id("org.gradle.toolchains.foojay-resolver-convention") version("0.4.0")
//}

rootProject.name = "2_Android_bluetooth_master_stradivary"

include(":app")
include(":bluetooth")
include(":delegateadapter")
include(":kmm_ubi4")
