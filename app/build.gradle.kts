@file:Suppress("UnstableApiUsage")

plugins {
    id("com.android.application")
    id("kotlin-android")
}

android {
    namespace = "io.github.cloudburst.messengerex"
    compileSdk = 34
    ndkVersion = "25.1.8937393"

    defaultConfig {
        applicationId = "io.github.cloudburst.messengerex"
        minSdk = 27
        targetSdk = 34
        versionCode = 1
        versionName = "1.0.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        compose = true
        resValues = false
    }
    composeOptions.kotlinCompilerExtensionVersion = "1.5.3"
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
        }
    }
}

dependencies {
    compileOnly("de.robv.android.xposed:api:82")
    implementation("org.luckypray:dexkit:2.0.0-rc7")

    // // AndroidX
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.browser:browser:1.6.0")
}
