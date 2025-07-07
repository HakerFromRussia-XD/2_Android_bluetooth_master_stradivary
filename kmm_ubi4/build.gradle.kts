plugins {
    id("com.android.library")
    id("dev.icerock.mobile.multiplatform-resources") version "0.24.5"
    kotlin("multiplatform")
    kotlin("plugin.serialization")
    id("org.jetbrains.kotlin.native.cocoapods")
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
    // Дублирующая строка с res уже не нужна, но оставил на всякий случай:
    // sourceSets["main"].res.srcDirs("src/androidMain/res")

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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }


}


kotlin {
    androidTarget()
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Shared business logic"
        homepage = "https://github.com/your-repo"
        version = "1.0.0"
        ios.deploymentTarget = "14.0"
        framework {
            baseName = "Shared"
            export("org.jetbrains.kotlinx:kotlinx-coroutines-core")
        }
        pod("MotoricaBLEBridge", path = file("/Users/motoricallc/Documents/iOs/iOS-Clean-Architecture-MVVM"))
    }

    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
                implementation("org.jetbrains.kotlinx:kotlinx-datetime:0.4.0")
                implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.5.1")
                implementation("io.ktor:ktor-client-core:2.3.2")
                implementation("io.ktor:ktor-client-content-negotiation:2.3.2")
                implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.2")
                implementation("org.jetbrains.kotlin:kotlin-stdlib-common:1.9.22")
                implementation("dev.icerock.moko:resources:0.24.5")
            }
            kotlin.srcDir("$buildDir/generated/moko/resources/commonMain/kotlin")
        }

        val androidMain by getting {
            dependencies {
                implementation("androidx.core:core-ktx:1.15.0")
                implementation("androidx.appcompat:appcompat:1.7.0")
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

        val iosMain by creating {
            dependsOn(commonMain)
            kotlin.srcDir("$buildDir/generated/moko/resources/iosMain/kotlin")
            dependencies {
                implementation("io.ktor:ktor-client-darwin:2.3.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.1")
            }
        }
        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimulatorArm64Main by getting { dependsOn(iosMain) }

        val commonTest by getting {
            dependencies {
                implementation(kotlin("test"))
                implementation("junit:junit:4.13.2")
                implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.1")
            }
        }
        val androidUnitTest by getting {
            dependencies {
                implementation("androidx.test.ext:junit:1.2.1")
                implementation("androidx.test.espresso:espresso-core:3.6.1")
            }
        }
    }
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile>().configureEach {
    kotlinOptions.jvmTarget = "1.8"
}

multiplatformResources {
    resourcesPackage.set("com.bailout.stickk.ubi4")
    resourcesClassName.set("MR")
}