pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
        maven(url = "https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }

    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "org.jetbrains.kotlin.plugin.serialization" -> useVersion("2.0.0")
                "org.jetbrains.kotlin.multiplatform" -> useVersion("2.0.0")
//                "org.jetbrains.kotlin.native.cocoapods" -> useVersion("2.0.0")
                "org.jetbrains.compose" -> useVersion("1.8.2")
                "org.jetbrains.kotlin.plugin.compose" -> useVersion("2.0.0")
            }
        }
    }
}

dependencyResolutionManagement {
//    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
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
include(":kmm_ubi4")
