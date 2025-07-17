@file:Suppress("UNUSED_EXPRESSION")

import com.android.build.api.dsl.Packaging

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
    kotlin("kapt")
    id("com.android.application")
    id("org.jetbrains.compose")
}

kotlin {
    androidTarget()
    sourceSets {
        val androidMain by getting {
            dependencies {
                implementation(project(":shared"))
            }
        }
    }
}

android {
    namespace = "com.bailout.stickk"
    compileSdk = 35
    sourceSets["main"].manifest.srcFile("src/main/AndroidManifest.xml")
    defaultConfig {
        applicationId = "com.bailout.stickk"
        minSdk = 28
        targetSdk = 33
        versionCode = 11
        versionName = "3.2.1023"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
//        signingConfig = signingConfigs.getByName("release")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }

    buildTypes {
        getByName("release") {
            // отключаем профилирование
            isProfileable = false
            // отключаем сжатие кода
            isMinifyEnabled = false
            // отключаем отладку
            isDebuggable = false
            // файлы ProGuard
            proguardFiles(
                getDefaultProguardFile("proguard-android.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("debug")
        }
    }
    lint {
        checkReleaseBuilds = false
        warningsAsErrors = false
        abortOnError = false
        baseline = file("lint-baseline.xml")
    }
    packaging {
        dex {
            useLegacyPackaging = true
        }
    }
    fun Packaging.() {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            excludes += "META-INF/INDEX.LIST"
            merges += "META-INF/LICENSE.md"
            merges += "classpath.index"
            merges += "META-INF/LICENSE-notice.md"
        }
    }
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs(listOf("libs"))
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

}

dependencies {

    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("androidx.core:core-ktx:1.16.0")

    // retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp:okhttp:2.7.5")
    implementation("com.squareup.okhttp3:logging-interceptor:4.5.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.8.0")

    // gson
    implementation("com.google.code.gson:gson:2.10.1")

    // dagger 2
    implementation("com.google.dagger:dagger:2.56")
    "kapt"("com.google.dagger:dagger-compiler:2.56"){
        exclude(group = "com.google.guava", module = "guava")
    }
    "kapt"("com.google.dagger:dagger-android-processor:2.56"){
        exclude(group = "com.google.guava", module = "guava")
    }
//    runtimeOnly("org.jetbrains.kotlin:kotlin-metadata-jvm:2.2.0")
//    implementation("com.google.guava:guava:33.1.0-android")

    // rxJava
    implementation("io.reactivex.rxjava2:rxjava:2.2.17")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    implementation("io.reactivex.rxjava2:rxkotlin:2.4.0")
    implementation("com.jakewharton.rxbinding2:rxbinding:2.2.0")
    implementation("com.trello.rxlifecycle2:rxlifecycle:2.2.2")
    implementation("com.trello.rxlifecycle2:rxlifecycle-android:2.2.2")
    implementation("com.trello.rxlifecycle2:rxlifecycle-components:2.2.2")

    // custom views
    implementation(files("libs/navigationtabbar-1.2.5.aar"))
    implementation(files("libs/switchbutton.aar"))
    implementation("com.github.jorgecastilloprz:fillableloaders:1.03@aar")
    implementation("com.github.paolorotolo:appintro:4.1.0")
    implementation("com.skyfishjy.ripplebackground:library:1.0.1")
    implementation("com.github.skydoves:elasticviews:2.0.3")
    implementation("com.github.skydoves:colorpickerview:2.1.3")
    implementation("com.github.shchurov:horizontalwheelview:0.9.5")
    implementation("com.github.skydoves:powerspinner:1.2.7")
    implementation("com.github.SimformSolutionsPvtLtd:SSPullToRefresh:1.5.2")
    implementation("com.github.woxthebox:draglistview:1.7.3")
    // pin
    implementation("online.devliving:passcodeview:1.0.3") {
        exclude(group = "com.android.support", module = "appcompat-v7")
        exclude(group = "com.android.support", module = "support-compat")
    }

    // butter knife
    implementation("com.jakewharton:butterknife:10.2.3")
    "kapt"("com.jakewharton:butterknife-compiler:10.2.3")

    // BLE connection
    implementation("com.android.support:cardview-v7:34.0.0")
    implementation("com.polidea.rxandroidble2:rxandroidble:1.11.1")

    // multi dex
    implementation("androidx.multidex:multidex:2.0.1")

    // debug
    implementation("com.jakewharton.timber:timber:4.7.1")

    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0-alpha")
    implementation("com.google.code.gson:gson:2.11.0")

    // dexter (permissions)
    implementation("com.karumi:dexter:6.2.3")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")

    //animation
    implementation("com.airbnb.android:lottie:6.4.0")

    //noinspection GradleDependency
    implementation("androidx.fragment:fragment-ktx:1.3.3")

    // coroutine
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")

    //TESTS
    implementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("io.mockk:mockk:1.13.7")
}
//dependencies {
//    val kotlinVersion = project.property("kotlin.version") as String
//    implementation(project(":bluetooth"))
//    implementation(project(":delegateadapter"))
//    implementation(project(":shared"))
//
//    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
//    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
//    implementation("androidx.core:core-ktx:1.16.0")
//
//
//    implementation("androidx.appcompat:appcompat:1.7.0")
//    implementation("com.google.android.material:material:1.12.0")
//    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
//    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
//
//    // retrofit
//    implementation("com.squareup.retrofit2:retrofit:2.9.0")
//    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
//    implementation("com.squareup.okhttp:okhttp:2.7.5")
//    implementation("com.squareup.okhttp3:logging-interceptor:4.5.0")
//    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0")
//    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.8.0")
//
//    // gson
//    implementation("com.google.code.gson:gson:2.10.1")
//
//    // dagger
//    // Dagger 2
//    implementation("com.google.dagger:dagger:2.47")
//    kapt("com.google.dagger:dagger-compiler:2.47")
//    kapt("com.google.dagger:dagger-android-processor:2.47")
//
//    // rxJava
//    implementation("io.reactivex.rxjava2:rxjava:2.2.17")
//    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
//    implementation("io.reactivex.rxjava2:rxkotlin:2.4.0")
//    implementation("com.jakewharton.rxbinding2:rxbinding:2.2.0")
//    implementation("com.trello.rxlifecycle2:rxlifecycle:2.2.2")
//    implementation("com.trello.rxlifecycle2:rxlifecycle-android:2.2.2")
//    implementation("com.trello.rxlifecycle2:rxlifecycle-components:2.2.2")
//
//    // custom views
//    implementation(files("libs/navigationtabbar-1.2.5.aar"))
//    implementation(files("libs/switchbutton.aar"))
//    implementation("com.github.jorgecastilloprz:fillableloaders:1.03@aar")
//    implementation("com.github.paolorotolo:appintro:4.1.0")
//    implementation("com.skyfishjy.ripplebackground:library:1.0.1")
//    implementation("com.github.skydoves:elasticviews:2.0.3")
//    implementation("com.github.skydoves:colorpickerview:2.1.3")
//    implementation("com.github.shchurov:horizontalwheelview:0.9.5")
//    implementation("com.github.skydoves:powerspinner:1.2.7")
//    implementation("com.github.SimformSolutionsPvtLtd:SSPullToRefresh:1.5.2")
//    implementation("com.github.woxthebox:draglistview:1.7.3")
//    // pin
//    implementation("online.devliving:passcodeview:1.0.3")
//
//    // butter knife
//    implementation("com.jakewharton:butterknife:10.2.3")
//    kapt("com.jakewharton:butterknife-compiler:10.2.3")
//
//    // BLE connection
//    implementation("com.android.support:cardview-v7:34.0.0")
//    implementation("com.polidea.rxandroidble2:rxandroidble:1.11.1")
//
//    // multi dex
//    implementation("androidx.multidex:multidex:2.0.1")
//
//    // debug
//    implementation("com.jakewharton.timber:timber:4.7.1")
//
//    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
//    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0-alpha")
//    //noinspection GradleCompatible
////    implementation 'com.android.support:appcompat-v7:30.0.0-alpha'
////    implementation 'com.android.support:design:34.0.0'
////    implementation 'com.android.support:mediarouter-v7:34.0.0'
////    implementation 'com.android.support:recyclerview-v7:34.0.0'
////    implementation 'com.android.support:support-v13:34.0.0'
////    implementation 'com.android.support:support-v4:34.0.0'
//    implementation("com.google.code.gson:gson:2.10.1")
//
//    // dexter (permissions)
//    implementation("com.karumi:dexter:6.2.3")
//    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
//
//    //animation
//    implementation("com.airbnb.android:lottie:6.4.0")
//
//    //noinspection GradleDependency
//    implementation("androidx.fragment:fragment-ktx:1.3.3")
//
//    // coroutine
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
//    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
//
//    //TESTS
//    implementation("org.junit.jupiter:junit-jupiter:5.10.2")
//    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.2")
//    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
//    testImplementation("junit:junit:4.13.2")
//    testImplementation("org.mockito:mockito-core:5.3.1")
//    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
//    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
//    testImplementation("io.mockk:mockk:1.13.7")
//}
