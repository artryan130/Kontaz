import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
}

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}
val apiBaseUrl = providers.gradleProperty("API_BASE_URL")
    .orElse(localProperties.getProperty("API_BASE_URL") ?: "https://kontaz-backend.onrender.com/")
    .get()
val supabaseUrl = providers.gradleProperty("SUPABASE_URL")
    .orElse(localProperties.getProperty("SUPABASE_URL") ?: "https://ijgkgydwgdnoxtesxosy.lovable.cloud")
    .get()
    .trimEnd('/')
val supabaseAnonKey = providers.gradleProperty("SUPABASE_ANON_KEY")
    .orElse(localProperties.getProperty("SUPABASE_ANON_KEY") ?: "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImlqZ2tneWR3Z2Rub3h0ZXN4b3N5Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3NjU1NDE1NzEsImV4cCI6MjA4MTExNzU3MX0.c9G6hh1KsoioM-7JTcDxNLFeoXNfv0UOvatbBqFJNZ4")
    .get()
val uploadStoreFile = providers.environmentVariable("KONTAZ_UPLOAD_STORE_FILE").orNull
val uploadStorePassword = providers.environmentVariable("KONTAZ_UPLOAD_STORE_PASSWORD").orNull
val uploadKeyAlias = providers.environmentVariable("KONTAZ_UPLOAD_KEY_ALIAS").orNull
val uploadKeyPassword = providers.environmentVariable("KONTAZ_UPLOAD_KEY_PASSWORD").orNull
val hasReleaseSigning = listOf(uploadStoreFile, uploadStorePassword, uploadKeyAlias, uploadKeyPassword).all { !it.isNullOrBlank() }

if (listOf(uploadStoreFile, uploadStorePassword, uploadKeyAlias, uploadKeyPassword).any { !it.isNullOrBlank() } && !hasReleaseSigning) {
    throw GradleException("Set all four KONTAZ_UPLOAD_* environment variables to configure release signing.")
}

android {
    namespace = "br.com.kontaz"
    compileSdk = 36

    defaultConfig {
        applicationId = "br.com.kontaz"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"
        buildConfigField("String", "API_BASE_URL", "\"$apiBaseUrl\"")
        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
    }

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(requireNotNull(uploadStoreFile))
                storePassword = requireNotNull(uploadStorePassword)
                keyAlias = requireNotNull(uploadKeyAlias)
                keyPassword = requireNotNull(uploadKeyPassword)
            }
        }
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildTypes {
        getByName("release") {
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

tasks.configureEach {
    if (name == "bundleRelease" || name == "assembleRelease") {
        doFirst {
            check(hasReleaseSigning) {
                "Release artifacts require KONTAZ_UPLOAD_STORE_FILE, KONTAZ_UPLOAD_STORE_PASSWORD, KONTAZ_UPLOAD_KEY_ALIAS, and KONTAZ_UPLOAD_KEY_PASSWORD."
            }
            check(apiBaseUrl.startsWith("https://")) {
                "Release artifacts must use an HTTPS API_BASE_URL."
            }
        }
    }
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.security.crypto)
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(libs.okhttp)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
