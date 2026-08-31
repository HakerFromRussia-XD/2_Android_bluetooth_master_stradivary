plugins {
    kotlin("jvm") apply false
    kotlin("multiplatform") apply false
    kotlin("android") apply false
    id("com.android.application") apply false
    id("com.android.library") apply false
    id("org.jetbrains.compose") apply false
}
buildscript {
    dependencies {
        classpath("dev.icerock.moko:resources-generator:0.25.1")
    }
}

allprojects {
    configurations.configureEach {
        exclude(group = "com.android.support", module = "support-compat")
        // Dagger kapt needs JRE Guava; Android variant on worker classpath → NoSuchMethodError
        val guavaArtifact = if (
            name.contains("kapt", ignoreCase = true) ||
            name.contains("annotationProcessor", ignoreCase = true)
        ) {
            "com.google.guava:guava:33.1.0-jre"
        } else {
            "com.google.guava:guava:33.1.0-android"
        }
        resolutionStrategy.force(guavaArtifact)
    }
    repositories {
        mavenCentral()
        mavenLocal()
        google()
        gradlePluginPortal()
        maven("https://jitpack.io")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://plugins.gradle.org/m2")
    }
}

tasks.register<Delete>("clean") {
    @Suppress("DEPRECATION")
    delete(rootProject.buildDir)
}