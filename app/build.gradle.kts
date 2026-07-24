@file:Suppress("UNUSED_EXPRESSION")

import groovy.json.JsonSlurper
import java.util.Properties

plugins {
    kotlin("multiplatform")
    kotlin("plugin.compose")
    kotlin("plugin.serialization")
    kotlin("kapt")
    id("com.android.application")
    id("org.jetbrains.compose")
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) {
        file.inputStream().use(::load)
    }
}

fun signingProperty(name: String): String? =
    (findProperty(name) as? String)
        ?: localProperties.getProperty(name)
        ?: System.getenv(name)

val motoricaReleaseStoreFile = signingProperty("motoricaReleaseStoreFile")
    ?: rootProject.file("keystore.jks").takeIf { it.isFile }?.absolutePath
val motoricaReleaseStorePassword = signingProperty("motoricaReleaseStorePassword")
val motoricaReleaseKeyAlias = signingProperty("motoricaReleaseKeyAlias")
val motoricaReleaseKeyPassword = signingProperty("motoricaReleaseKeyPassword")
val hasMotoricaReleaseSigning = listOf(
    motoricaReleaseStoreFile,
    motoricaReleaseStorePassword,
    motoricaReleaseKeyAlias,
    motoricaReleaseKeyPassword
).all { !it.isNullOrBlank() }
val missingMotoricaReleaseSigning = buildList {
    if (motoricaReleaseStoreFile.isNullOrBlank()) add("motoricaReleaseStoreFile")
    if (motoricaReleaseStorePassword.isNullOrBlank()) add("motoricaReleaseStorePassword")
    if (motoricaReleaseKeyAlias.isNullOrBlank()) add("motoricaReleaseKeyAlias")
    if (motoricaReleaseKeyPassword.isNullOrBlank()) add("motoricaReleaseKeyPassword")
}

val v3ManifestFile = file("src/main/assets/STR2_V3/festh3_test3_manifest.json")
val packagedV3AssetNames = mutableSetOf(v3ManifestFile.name)

fun collectPackagedV3AssetNames(value: Any?) {
    when (value) {
        is Map<*, *> -> value.values.forEach(::collectPackagedV3AssetNames)
        is Iterable<*> -> value.forEach(::collectPackagedV3AssetNames)
        is String -> if (value.endsWith(".v3bin") || value.endsWith(".v3def")) {
            packagedV3AssetNames += value.substringAfterLast('/')
        }
    }
}

collectPackagedV3AssetNames(JsonSlurper().parse(v3ManifestFile))

val unusedV3AssetNames = listOf("STR2_V3", "STR2_V3_BIN")
    .flatMap { directory ->
        file("src/main/assets/$directory").listFiles()?.filter { it.isFile }.orEmpty()
    }
    .map { it.name }
    .filterNot(packagedV3AssetNames::contains)
    .distinct()

gradle.taskGraph.whenReady {
    val needsReleaseApk = allTasks.any { task ->
        task.path == ":app:assembleRelease" ||
            task.path == ":app:installRelease" ||
            task.path == ":app:packageRelease"
    }
    if (needsReleaseApk && !hasMotoricaReleaseSigning) {
        throw GradleException(
            "Motorica release signing is not configured. Missing: " +
                missingMotoricaReleaseSigning.joinToString() +
                ". Add uncommented values to ${rootProject.file("local.properties").absolutePath}."
        )
    }
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
    sourceSets.maybeCreate("metrics").java.srcDir("src/debug/java")
    defaultConfig {
        applicationId = "com.bailout.stickk"
        minSdk = 28
        targetSdk = 33
        versionCode = 15
        versionName = "3.3.1785"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        multiDexEnabled = true
        manifestPlaceholders["gameControlPermission"] = "com.motorica.gamecontrol.permission.CONTROL_GAME"

        val motoricaGamesManifestUrl = providers.gradleProperty("motoricaGamesManifestUrl").orElse("").get()
        buildConfigField("String", "MOTORICA_GAMES_MANIFEST_URL", "\"${motoricaGamesManifestUrl.replace("\\", "\\\\").replace("\"", "\\\"")}\"")
        buildConfigField("String", "MOTORICA_STK_PACKAGE", "\"com.motorica.games.stk\"")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlin {
        jvmToolchain(17)
    }

    signingConfigs {
        if (hasMotoricaReleaseSigning) {
            create("motoricaRelease") {
                storeFile = file(motoricaReleaseStoreFile!!)
                storePassword = motoricaReleaseStorePassword
                keyAlias = motoricaReleaseKeyAlias
                keyPassword = motoricaReleaseKeyPassword
            }
        }
    }


    buildTypes {
        create("metrics") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".metrics"
            versionNameSuffix = "-metrics"
            matchingFallbacks += listOf("debug")
            manifestPlaceholders["gameControlPermission"] = "com.bailout.stickk.metrics.permission.CONTROL_GAME"
        }
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
            if (hasMotoricaReleaseSigning) {
                signingConfig = signingConfigs.getByName("motoricaRelease")
            }
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
        resources {
            excludes.add("META-INF/{AL2.0,LGPL2.1}")
            excludes.add("META-INF/INDEX.LIST")
            merges.add("META-INF/LICENSE.md")
            merges.add("classpath.index")
            merges.add("META-INF/LICENSE-notice.md")
        }
    }
    androidResources {
        noCompress += "v3bin"
        noCompress += "v3def"
        noCompress += "astc"
        ignoreAssetsPattern = (
            listOf("!.svn:!.git:!.ds_store:!*.scc:.*:<dir>_*:!CVS:!thumbs.db:!picasa.ini:!*~") +
                unusedV3AssetNames
            ).joinToString(":")
    }
    sourceSets {
        getByName("main") {
            jniLibs.srcDirs(listOf("libs"))
        }
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            all {
                it.useJUnitPlatform()
            }
        }
    }
    buildFeatures {
        aidl = true
        buildConfig = true
        compose = true
        viewBinding = true
    }

}

dependencies {
    val kotlinVersion: String = providers.gradleProperty("kotlin.version").get()

    // local libs
    implementation(project(":bluetooth"))
    implementation(project(":delegateadapter"))

    implementation("androidx.localbroadcastmanager:localbroadcastmanager:1.1.0")
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.12.0")
    //noinspection GradleDependency
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")

    // retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:0.8.0")

    // gson
    implementation("com.google.code.gson:gson:2.11.0")

    // dagger 2
    implementation("com.google.dagger:dagger:2.56")
    "kapt"("com.google.guava:guava:33.3.1-jre")
    "kapt"("com.google.dagger:dagger-compiler:2.56")
    "kapt"("com.google.dagger:dagger-android-processor:2.56")
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
    implementation("com.github.skydoves:elasticviews:2.0.7")
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
    implementation(compose.runtime)
    implementation(compose.ui)
    implementation(compose.foundation)
    implementation(compose.material)
    implementation(compose.components.uiToolingPreview)
    debugImplementation(compose.uiTooling)
    releaseImplementation(compose.uiTooling)

    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0-alpha")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")

    // dexter (permissions)
    implementation("com.karumi:dexter:6.2.3")

    //animation
    implementation("com.airbnb.android:lottie:6.4.0")

    //noinspection GradleDependency
    implementation("androidx.fragment:fragment-ktx:1.3.3")

    // coroutine
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.1")
    implementation("androidx.work:work-runtime-ktx:2.9.1")

    //TESTS
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.2")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.2")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.mockito:mockito-core:5.3.1")
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.1.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.1")
    testImplementation("io.mockk:mockk:1.13.7")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test:runner:1.6.2")
    androidTestImplementation("androidx.test:rules:1.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.test.espresso:espresso-contrib:3.6.1")
    androidTestImplementation("androidx.test.uiautomator:uiautomator:2.3.0")

    // Ktor
    implementation("io.ktor:ktor-client-core:2.3.12")
    implementation("io.ktor:ktor-client-okhttp:2.3.12")
    implementation("io.ktor:ktor-client-content-negotiation:2.3.12")
    implementation("io.ktor:ktor-serialization-kotlinx-json:2.3.12")
    implementation("io.ktor:ktor-client-logging:2.3.12")


    implementation("androidx.room:room-runtime:2.7.2")
    implementation("androidx.room:room-ktx:2.7.2")
}
