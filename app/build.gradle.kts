import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

@Suppress("DSL_SCOPE_VIOLATION") // TODO: Remove once KTIJ-19369 is fixed
plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.jetbrains.kotlin.serialization)
    alias(libs.plugins.screenshot)
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

object Version {
    const val versionCode = 3
    const val versionName = "1.0.$versionCode"
}

val releaseSigningPropertiesFile = rootProject.file("app/signing.properties")
val releaseSigningProperties = Properties().apply {
    if (releaseSigningPropertiesFile.exists()) {
        releaseSigningPropertiesFile.inputStream().use(::load)
    }
}

android {
    namespace = "no.timnor.app"
    compileSdk = 37
    experimentalProperties["android.experimental.enableScreenshotTest"] = true

    defaultConfig {
        applicationId = "no.timnor.app"
        minSdk = 26
        targetSdk = 34
        versionCode = Version.versionCode
        versionName = Version.versionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    if (releaseSigningPropertiesFile.exists()) {
        signingConfigs {
            create("release") {
                storeFile = file(requireNotNull(releaseSigningProperties.getProperty("storeFile")))
                storePassword = requireNotNull(releaseSigningProperties.getProperty("storePassword"))
                keyAlias = requireNotNull(releaseSigningProperties.getProperty("keyAlias"))
                keyPassword = requireNotNull(releaseSigningProperties.getProperty("keyPassword"))
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            versionNameSuffix = "-debug"
            signingConfig = signingConfigs.getByName("debug")
        }

        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (releaseSigningPropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(project(":shared"))
    implementation(libs.core.ktx)
    implementation(libs.lifecycle.runtime.ktx)
    implementation(libs.lifecycle.runtime.compose)
    implementation(libs.activity.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.ui)
    implementation(libs.ui.graphics)
    implementation(libs.ui.tooling.preview)
    implementation(libs.material3)
    implementation(libs.androidx.navigation3.runtime)
    implementation(libs.androidx.navigation3.ui)
    implementation(libs.lifecycle.viewmodel.navigation3)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.ktor.client.mock)
    testImplementation(libs.ktor.client.content.negotiation)
    testImplementation(libs.ktor.serialization.kotlinx.json)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.room.testing)
    androidTestImplementation(libs.ktor.client.mock)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(platform(libs.compose.bom))
    androidTestImplementation(libs.ui.test.junit4)
    debugImplementation(libs.ui.tooling)
    debugImplementation(libs.ui.test.manifest)
    screenshotTestImplementation(libs.screenshot.validation.api)
    screenshotTestImplementation(libs.ui.tooling)


    implementation(libs.lottie.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)
    implementation(libs.ktor.client.okhttp)

    implementation(libs.room.runtime)
    implementation(libs.sqlite.bundled)


    implementation(libs.poi)
    implementation(libs.poi.ooxml)

    ksp(libs.room.compiler)
}

room {
    schemaDirectory("$projectDir/schemas")
}

tasks.register<Exec>("exportUiCatalog") {
    dependsOn("updateDebugScreenshotTest")
    commandLine("python", rootProject.file("tools/export_ui_catalog.py").absolutePath)
}
