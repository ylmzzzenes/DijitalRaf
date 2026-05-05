import java.io.FileInputStream
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    id("com.google.gms.google-services")
}

val localProperties = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) {
        load(FileInputStream(f))
    }
}
/** Properties dosyasında OPENROUTER_API_KEY="..." yazılmışsa tırnaklar değere dahil olur; sunucuya yanlış gider. */
fun normalizeApiKeyProperty(raw: String?): String =
    (raw ?: "")
        .trim()
        .removeSurrounding("\"")
        .removeSurrounding("'")
        .trim()

val openRouterApiKey = normalizeApiKeyProperty(localProperties.getProperty("OPENROUTER_API_KEY"))

fun escapeForBuildConfig(value: String): String =
    value.replace("\\", "\\\\").replace("\"", "\\\"")

android {
    namespace = "com.example.dijitalraf"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.dijitalraf"
        minSdk = 33
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField(
            "String",
            "OPENROUTER_API_KEY",
            "\"${escapeForBuildConfig(openRouterApiKey)}\"",
        )
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.coordinatorlayout)
    implementation(libs.recyclerview)
    implementation(libs.viewpager2)
    implementation(libs.fragment)
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation(libs.firebase.database)

    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    implementation(platform("com.google.firebase:firebase-bom:34.12.0"))
    implementation("com.google.firebase:firebase-auth")
    implementation("com.google.firebase:firebase-storage")
    implementation("androidx.credentials:credentials:1.3.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.3.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")
}