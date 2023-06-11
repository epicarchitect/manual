plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ksp)
    id("convention.android.base")
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