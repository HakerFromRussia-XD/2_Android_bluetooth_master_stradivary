plugins {
    base
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
    configurations.all {
        exclude(group = "com.android.support", module = "support-compat")
        resolutionStrategy.force("com.google.guava:guava:33.1.0-android")
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

fun String.toBooleanFlag(): Boolean =
    equals("true", ignoreCase = true) || equals("1", ignoreCase = true) || equals("yes", ignoreCase = true)

val runTestsOnBuild = providers
    .gradleProperty("runTestsOnBuild")
    .orElse("false")
    .map { it.toBooleanFlag() }
    .get()

val runAllUnitTests = tasks.register("runAllUnitTests") {
    group = "verification"
    description = "Runs all JVM/unit Test tasks from all subprojects."
}

gradle.projectsEvaluated {
    subprojects.forEach { subproject ->
        subproject.tasks.names
            .filter { taskName ->
                taskName == "test" || (taskName.startsWith("test") && taskName.endsWith("UnitTest"))
            }
            .forEach { taskName ->
                runAllUnitTests.configure {
                    dependsOn("${subproject.path}:$taskName")
                }
            }
    }
}

tasks.named("build").configure {
    if (runTestsOnBuild) {
        dependsOn(runAllUnitTests)
    }
}

if (runTestsOnBuild) {
    subprojects {
        tasks.matching {
            it.name == "build" ||
                it.name.startsWith("assemble")
        }.configureEach {
            dependsOn(rootProject.tasks.named("runAllUnitTests"))
        }
    }
}

val buildAllModules = tasks.register("buildAllModules") {
    group = "build"
    description = "Builds all subprojects."
    subprojects.forEach { sub ->
        dependsOn("${sub.path}:build")
    }
}

tasks.register("buildWithAllTests") {
    group = "build"
    description = "Builds all modules and always runs all JVM/unit tests."
    dependsOn(buildAllModules)
    dependsOn(runAllUnitTests)
}

tasks.named<Delete>("clean") {
    @Suppress("DEPRECATION")
    delete(rootProject.buildDir)
}
