plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.myhifiplayer"
    compileSdk = 34
    ndkVersion = "27.0.12077973"

    defaultConfig {
        applicationId = "com.example.myhifiplayer"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        // Явно указываем архитектуры, под которые собираем нативную библиотеку
        ndk {
            abiFilters.add("arm64-v8a") // Твой Redmi Note 14 Pro использует эту архитектуру
        }

        externalNativeBuild {
            cmake {
                cppFlags("-std=c++17", "-O3", "-ffast-math")
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "3.22.1"
        }
    }

    buildFeatures {
        prefab = true
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.oboe:oboe:1.9.0")
}
