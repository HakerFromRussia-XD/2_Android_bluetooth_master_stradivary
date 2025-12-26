plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    id("com.android.library")
    id("org.jetbrains.compose")
    kotlin("plugin.serialization")
    id("kotlin-parcelize")
    id("dev.icerock.mobile.multiplatform-resources")
    id("com.google.devtools.ksp") version "2.1.21-2.0.1"
    id("androidx.room") version "2.7.2"
//    kotlin("kapt")
}

ksp {
    arg("ksp.incremental", "false")
    arg("ksp.verbose", "true")
}
room {
//    schemaDirectory("$projectDir/schemas")
    // Android variants
    schemaDirectory("debug",   layout.projectDirectory.dir("schemas/androidDebug"))
    schemaDirectory("release", layout.projectDirectory.dir("schemas/androidRelease"))

    // Common metadata
    schemaDirectory("metadata",                    layout.projectDirectory.dir("schemas/metadata"))
    schemaDirectory("kspKotlinMetadata",           layout.projectDirectory.dir("schemas/metadata"))
    schemaDirectory("kspCommonMainKotlinMetadata", layout.projectDirectory.dir("schemas/metadata"))

    // iOS targets — даём оба написания для каждого
    schemaDirectory("iosArm64",            layout.projectDirectory.dir("schemas/iosArm64"))
    schemaDirectory("kspKotlinIosArm64",   layout.projectDirectory.dir("schemas/iosArm64"))

    schemaDirectory("iosX64",              layout.projectDirectory.dir("schemas/iosX64"))
    schemaDirectory("kspKotlinIosX64",     layout.projectDirectory.dir("schemas/iosX64"))

    schemaDirectory("iosSimulatorArm64",   layout.projectDirectory.dir("schemas/iosSimArm64"))
    schemaDirectory("kspKotlinIosSimulatorArm64", layout.projectDirectory.dir("schemas/iosSimArm64"))
}


android {
    namespace = "com.bailout.stickk.ubi4.shared"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
        consumerProguardFiles("consumer-rules.pro")
    }

    sourceSets.named("main") {
        java.srcDirs("src/androidMain/java")
        res.srcDirs(
            "src/androidMain/res",
            "build/generated/moko/androidMain/res",
            "build/generated/moko-resources/androidMain/res"
        )
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
tasks.named("preBuild").configure {
    dependsOn("generateMRandroidMain")
}


kotlin {
    androidTarget()

    listOf(
//        iosX64(),
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "shared"
            freeCompilerArgs = listOf("-Xbinary=bundleId=com.example.shared")
            export("dev.icerock.moko:resources:0.25.1")
//            export("dev.icerock.moko:graphics:0.9.0")
        }
    }



    applyDefaultHierarchyTemplate()

    sourceSets {
        all {
            languageSettings {
                optIn("org.jetbrains.compose.ExperimentalComposeLibrary")
                optIn("org.jetbrains.compose.resources.ExperimentalResourceApi")
                optIn("kotlin.experimental.ExperimentalObjCName")
            }
        }

        val commonMain by getting {
            dependencies {
                // ---- Твои compose/stdlib и др. ----
                implementation(compose.runtime)
                implementation(compose.foundation)
                implementation(compose.material)
                implementation(compose.components.resources)

                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.5.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.7.0")

                // ---- Ktor (оставляю обе версии, как у тебя; Gradle возьмёт новее) ----
                implementation("io.ktor:ktor-client-core:2.3.2")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.2")

                implementation("io.ktor:ktor-client-core:2.3.12")
                implementation("io.ktor:ktor-client-logging:2.3.12")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")

                // ---- MOKO resources ----
                api("dev.icerock.moko:resources:0.25.1")
                api("dev.icerock.moko:graphics:0.9.0")

                // --- Room Multiplatform ---
                implementation("androidx.room:room-runtime:2.7.2")
                implementation("androidx.sqlite:sqlite-bundled:2.5.0")

                implementation("androidx.room:room-common:2.7.2")
            }
        }

        val androidMain by getting {
            dependencies {
                api("androidx.activity:activity-compose:1.10.1")
                api("androidx.appcompat:appcompat:1.7.1")
                api("androidx.core:core-ktx:1.16.0")
                implementation("com.google.android.material:material:1.12.0")

                // Ktor Android
                implementation("io.ktor:ktor-client-okhttp:2.3.2")
                implementation("io.ktor:ktor-client-okhttp:2.3.12")

                // RX
                implementation("io.reactivex.rxjava2:rxjava:2.2.21")
                implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
                implementation("io.reactivex.rxjava2:rxkotlin:2.4.0")
                implementation("com.jakewharton.rxbinding2:rxbinding:2.2.0")

                // Lifecycle
                implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.2")
                implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")

                // OkHttp utils
                implementation("com.squareup.okhttp3:okhttp-sse:4.10.0")
                implementation("com.squareup.okhttp3:logging-interceptor:4.10.0")

                implementation("androidx.room:room-runtime:2.7.2")
                implementation("androidx.room:room-ktx:2.7.2")

            }

        }

        val iosMain by getting {
            dependencies {
                // SQLDelight iOS
//                api("app.cash.sqldelight:native-driver:2.1.0")
                implementation("androidx.room:room-runtime:2.7.2")
                api("dev.icerock.moko:resources:0.25.1")
                api("dev.icerock.moko:graphics:0.9.0")
//                implementation("app.cash.sqldelight:native-driver:2.1.0")
            }
        }
        androidUnitTest.dependencies {
            implementation(kotlin("test"))
            implementation(kotlin("test-junit"))
            implementation("junit:junit:4.13.2")
        }
    }
}

dependencies {
    add("kspCommonMainMetadata", "androidx.room:room-compiler:2.7.2")
    add("kspAndroid", "androidx.room:room-compiler:2.7.2")
    add("kspIosArm64", "androidx.room:room-compiler:2.7.2")
    add("kspIosSimulatorArm64", "androidx.room:room-compiler:2.7.2")
}




multiplatformResources {
    resourcesPackage.set("com.bailout.stickk.ubi4.shared")
    resourcesClassName.set("SharedRes")
}



