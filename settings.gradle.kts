pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }

    resolutionStrategy {
        eachPlugin {
            when (requested.id.id) {
                "org.jetbrains.kotlin.plugin.serialization" -> useVersion("1.9.22")
                "org.jetbrains.kotlin.multiplatform" -> useVersion("1.9.22")
                "org.jetbrains.kotlin.native.cocoapods" -> useVersion("1.9.22")
                "org.jetbrains.kotlin.plugin.compose" -> useVersion("1.9.22")
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
