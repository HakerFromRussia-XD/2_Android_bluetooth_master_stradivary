plugins {
    kotlin("jvm") apply false
    kotlin("multiplatform") apply false
    kotlin("android") apply false
    id("com.android.application") apply false
    id("com.android.library") apply false
    id("org.jetbrains.compose") apply false
}

allprojects {
    configurations.all {
        exclude(group = "com.android.support", module = "support-compat")
    }
    repositories {
        mavenCentral()
        mavenLocal()
        maven("https://jitpack.io")
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
        maven("https://plugins.gradle.org/m2")
        google()
        gradlePluginPortal()
    }
}

tasks.register<Delete>("clean") { delete(rootProject.buildDir) }