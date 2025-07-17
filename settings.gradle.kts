pluginManagement {
    val kotlinVersion: String = providers.gradleProperty("kotlin.version").get()
    val composeVersion: String = providers.gradleProperty("compose.version").get()
    val agpVersion: String = providers.gradleProperty("agp.version").get()
    val mppResourcesVersion: String = providers.gradleProperty("mpp.resources.version").get()

    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
        google()
    }

//    resolutionStrategy {
//        eachPlugin {
//            when (requested.id.id) {
//                "org.jetbrains.kotlin.multiplatform" -> useVersion(kotlinVersion)
//                "org.jetbrains.kotlin.plugin.compose" -> useVersion(kotlinVersion)
//                "org.jetbrains.compose" -> useVersion(composeVersion)
//                "org.jetbrains.kotlin.plugin.serialization" -> useVersion(kotlinVersion)
//                "org.jetbrains.kotlin.kapt" -> useVersion(kotlinVersion)
//                "kotlin-parcelize" -> useVersion(kotlinVersion)
//                "org.jetbrains.kotlin.plugin.parcelize" -> useVersion(kotlinVersion)
//                "dev.icerock.mobile.multiplatform-resources" -> useVersion(mppResourcesVersion)
//            }
//        }
//    }

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
        id("dev.icerock.mobile.multiplatform-resources").version(mppResourcesVersion)


        id("com.autonomousapps.dependency-analysis") version "1.27.0"
    }
}

//dependencyResolutionManagement {
//    repositories {
//        google()
//        mavenCentral()
//        maven(url = "https://jitpack.io")
//    }
//}

rootProject.name = "2_Android_bluetooth_master_stradivary"

include(":app")
include(":bluetooth")
include(":delegateadapter")
include(":shared")
