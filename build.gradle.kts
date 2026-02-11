plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.hebarcodescanner"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId = "com.example.hebarcodescanner"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
        viewBinding = true
        compose = false
    }
}

dependencies {
    implementation("org.jetbrains.kotlin:kotlin-stdlib:2.3.10")
// Quickie QR scanner
    implementation("io.github.g00fy2.quickie:quickie-bundled:1.11.0")
// OkHttp for HTTP POST
    implementation("com.squareup.okhttp3:okhttp:5.3.2")
    implementation("androidx.appcompat:appcompat:1.7.1")
}