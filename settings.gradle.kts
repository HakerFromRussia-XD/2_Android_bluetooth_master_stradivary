pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "org.jetbrains.kotlin.plugin.serialization") {
                useVersion "1.9.22" // укажите нужную версию плагина
            }
        }
    }
}

include ':app', ':bluetooth', ':delegateadapter', ':kmm_ubi4'