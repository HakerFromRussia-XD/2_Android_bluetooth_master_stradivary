plugins {
    kotlin("multiplatform") //apply false
//    id("org.jetbrains.kotlin.plugin.compose")
//    kotlin("plugin.compose")
    id("org.jetbrains.compose") //apply false
    kotlin("plugin.serialization") //apply false
    id("com.android.library") //apply false
    kotlin("plugin.parcelize") //apply false
    id("dev.icerock.mobile.multiplatform-resources") //apply false
    kotlin("jvm") apply false
}

android {
    namespace = "com.bailout.stickk.ubi4"
    compileSdk = 34

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    android.sourceSets.named("main") {
        java.srcDirs("src/androidMain/java")
        res.srcDirs("src/androidMain/res")
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }
}

kotlin {
    val kotlinVersion = project.property("kotlin.version") as String
    androidTarget()
//    compilerOptions {
//        jvmTarget.set(JvmTarget.fromTarget("17"))
//    }

    listOf(
        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            isStatic = true
        }
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        all {
            languageSettings {
                optIn("org.jetbrains.compose.ExperimentalComposeLibrary")
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
            }
        }

        val commonMain by getting {
            dependencies {
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.components.resources)
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
                implementation("io.ktor:ktor-client-core:2.3.2")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.2")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.2")
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion")
                implementation("dev.icerock.moko:resources:0.24.5")
            }
            kotlin.srcDir("$buildDir/generated/moko/resources/commonMain/kotlin")
        }

        val androidMain by getting {
            dependencies {
                api("androidx.activity:activity-compose:1.7.0")
                api("androidx.appcompat:appcompat:1.7.0")
                api("androidx.core:core-ktx:1.15.0")
                implementation("com.google.android.material:material:1.12.0")
                implementation("io.ktor:ktor-client-okhttp:2.3.2")
                implementation("dev.icerock.moko:resources:0.24.5")

                implementation("io.reactivex.rxjava2:rxjava:2.2.17")
                implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
                implementation("io.reactivex.rxjava2:rxkotlin:2.4.0")
                implementation("com.jakewharton.rxbinding2:rxbinding:2.2.0")
                implementation("com.trello.rxlifecycle2:rxlifecycle:2.2.2")
                implementation("com.trello.rxlifecycle2:rxlifecycle-android:2.2.2")
                implementation("com.trello.rxlifecycle2:rxlifecycle-components:2.2.2")

                implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.1")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
            }
            kotlin.srcDir("$buildDir/generated/moko/resources/androidMain/kotlin")
        }
    }
}

//tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
//    kotlinOptions.jvmTarget = "17"
//}

multiplatformResources {
    resourcesPackage.set("com.bailout.stickk.ubi4")
    resourcesClassName.set("MR")
}