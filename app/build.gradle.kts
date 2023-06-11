plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    id("convention.android.base")
}

android {
    signingConfigs {
        register("release") {
            storeFile = file("signing/release.jks")
            storePassword = "epicdebug"
            keyAlias = "epicdebug"
            keyPassword = "epicdebug"
        }
    }

    buildTypes {
        release {
            signingConfig = signingConfigs.getByName("release")
            isMinifyEnabled = false
            isShrinkResources = false
        }

        debug {
            applicationIdSuffix = ".debug"
        }
    }
}

dependencies {
    implementation(projects.core)
    implementation(libs.google.gson)
    implementation(libs.epicarchitect.epicAdapter)
    implementation(libs.koin.android)
    implementation(libs.bumptech.glide)
    implementation(libs.davemorrissey.scaleImageView)
    implementation(libs.saket.betterLinkMovement)
    implementation(libs.android.datastorePreferences)
    implementation(libs.android.playCore)
    implementation(libs.android.playCoreKtx)
    implementation(libs.android.playServicesAds)
    implementation(libs.android.billingKtx)
    implementation(libs.android.exoplayer)
    implementation(libs.android.roomRuntime)
    implementation(libs.android.roomKtx)
    ksp(libs.android.roomCompiler)
}