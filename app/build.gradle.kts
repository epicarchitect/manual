plugins {
    id("com.android.application")
    id("kotlin-android")
    id("com.google.devtools.ksp") version "1.8.10-1.0.9"
    id("kotlin-parcelize")
}

android {
    bundle {
        storeArchive {
            enable = true
        }
    }

    namespace = "manual.app"
    compileSdk = 33

    defaultConfig {
        applicationId = "manual.app"
        minSdk = 21
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = JavaVersion.VERSION_11.toString()
    }

    buildFeatures {
        viewBinding = true
    }
}

kotlin {
    jvmToolchain(11)
}

dependencies {
    /** Project */
    implementation(project(":core"))

    /** Android Framework */
    implementation("androidx.core:core-ktx:1.9.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.work:work-runtime-ktx:2.8.0")
    implementation("androidx.fragment:fragment-ktx:1.5.5")
    implementation("androidx.lifecycle:lifecycle-common-java8:2.5.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.5.1")
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("com.google.android.material:material:1.8.0")
    implementation("com.google.android.play:core:1.10.3")
    implementation("com.google.android.play:core-ktx:1.8.1")

    implementation("com.github.epicarchitect:epic-adapter:1.0.7")

    /** Kotlin */
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.6.4")

    /** DI */
    implementation("io.insert-koin:koin-core:3.3.3")
    implementation("io.insert-koin:koin-android:3.3.3")

    /** Json */
    implementation("com.google.code.gson:gson:2.10")

    /** Images */
    implementation("com.github.bumptech.glide:glide:4.15.1")
    ksp("com.github.bumptech.glide:compiler:4.15.1")
    implementation("com.davemorrissey.labs:subsampling-scale-image-view-androidx:3.10.0")

    /** Video */
    implementation("com.google.android.exoplayer:exoplayer:2.18.3")

    /** Links */
    implementation("me.saket:better-link-movement-method:2.2.0")

    /** Database */
    implementation("androidx.room:room-runtime:2.5.0")
    implementation("androidx.room:room-ktx:2.5.0")
    ksp("androidx.room:room-compiler:2.5.0")

    /** Ads */
    implementation("com.google.android.gms:play-services-ads:21.5.0")
    implementation("com.google.android.ads.consent:consent-library:1.0.8")

    /** Billing */
    implementation("com.android.billingclient:billing-ktx:5.1.0")
}