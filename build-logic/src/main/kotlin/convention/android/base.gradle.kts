package convention.android

import BuildConstants
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.internal.dsl.BaseAppModuleExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.pm20.util.archivesName
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

configure<BaseExtension> {
    namespace = buildAndroidNamespace()
    compileSdkVersion(BuildConstants.TARGET_ANDROID_SDK)
    archivesName.set("${BuildConstants.ANDROID_APP_ID}-${BuildConstants.APP_VERSION_NAME}")

    defaultConfig {
        if (this@configure is BaseAppModuleExtension) {
            applicationId = BuildConstants.ANDROID_APP_ID
        }
        minSdk = BuildConstants.MIN_ANDROID_SDK
        targetSdk = BuildConstants.TARGET_ANDROID_SDK
        versionCode = BuildConstants.APP_VERSION_CODE
        versionName = BuildConstants.APP_VERSION_NAME
    }

    compileOptions {
        sourceCompatibility = JavaVersion.toVersion(BuildConstants.JVM_TARGET)
        targetCompatibility = JavaVersion.toVersion(BuildConstants.JVM_TARGET)
    }

    buildFeatures.viewBinding = true

    if (this is BaseAppModuleExtension) {
        bundle {
            storeArchive {
                enable = true
            }
        }
    }
}

tasks.withType<KotlinCompile>().configureEach {
    kotlinOptions {
        jvmTarget = BuildConstants.JVM_TARGET
    }
}

fun Project.buildAndroidNamespace(): String {
    val pathToModule = project.path.replace(
        regex = "[:\\-]".toRegex(),
        replacement = "."
    )

    return (rootProject.name + pathToModule).lowercase()
}