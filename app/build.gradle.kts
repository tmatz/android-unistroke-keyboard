
plugins {
    id("com.android.application")
}

android {
    namespace = "io.github.tmatz.hackers_unistroke_keyboard"
    compileSdk = 33
    
    defaultConfig {
        applicationId = "io.github.tmatz.hackers_unistroke_keyboard"
        minSdk = 16
        targetSdk = 33
        versionCode = 1
        versionName = "1.0"
        
        vectorDrawables { 
            useSupportLibrary = true
        }
    }
    
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    signingConfigs {
        create("release") {
            storeFile = File(rootProject.projectDir, "release.keystore")
            keyAlias = System.getenv("KEY_ALIAS")
            keyPassword = System.getenv("KEY_PASSWORD")
            storePassword = System.getenv("STORE_PASSWORD")
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }

    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    // implementation("com.google.android.material:material:1.9.0")
    // implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    // implementation("androidx.appcompat:appcompat:1.6.1")
}
