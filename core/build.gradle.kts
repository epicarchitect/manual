plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    id("convention.android.base")
}

dependencies {
    api(libs.android.coreKtx)
    api(libs.android.appcompat)
    api(libs.android.fragmentKtx)
    api(libs.android.material)
    api(libs.kotlin.coroutinesCore)
}