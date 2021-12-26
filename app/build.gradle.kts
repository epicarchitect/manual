plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
}

android {
    compileSdk = 31

    defaultConfig {
        applicationId = "manual.app"
        minSdk = 21
        targetSdk = 31
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_1_8.toString()
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    /** Project */
    implementation(project(":core"))

    /** Android Framework */
    implementation("androidx.core:core-ktx:1.7.0")
    implementation("androidx.appcompat:appcompat:1.4.0")
    implementation("androidx.work:work-runtime-ktx:2.7.1")
    implementation("androidx.fragment:fragment-ktx:1.4.0")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.4.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.4.0")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("com.google.android.material:material:1.4.0")
    implementation("com.google.android.play:core:1.10.2")
    implementation("com.google.android.play:core-ktx:1.8.1")

    /** Kotlin */
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.5.2")

    /** DI */
    implementation("io.insert-koin:koin-core:3.1.4")
    implementation("io.insert-koin:koin-android:3.1.4")

    /** Json */
    implementation("com.google.code.gson:gson:2.8.9")

    /** Images */
    implementation("com.github.stfalcon-studio:StfalconImageViewer:v1.0.1")
    implementation("com.github.bumptech.glide:glide:4.12.0")
    kapt("com.github.bumptech.glide:compiler:4.12.0")

    /** Keyboard */
    implementation("com.mctech.library.keyboard:visibilitymonitor:1.0.6")

    /** Links */
    implementation("me.saket:better-link-movement-method:2.2.0")

    /** Database */
    implementation("androidx.room:room-runtime:2.4.0")
    implementation("androidx.room:room-ktx:2.4.0")
    kapt("androidx.room:room-compiler:2.4.0")

    /** Ads */
    implementation("com.google.android.gms:play-services-ads:20.5.0")
    implementation("com.google.android.ads.consent:consent-library:1.0.8")

    /** Billing */
    implementation("com.android.billingclient:billing-ktx:4.0.0")
}