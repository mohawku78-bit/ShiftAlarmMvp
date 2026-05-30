plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.shiftalarmmvp.wear"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.shiftalarmmvp"
        minSdk = 26
        targetSdk = 35
        versionCode = 81
        versionName = "v081-watch"
    }

    buildTypes {
        create("sideBySide") {
            initWith(getByName("debug"))
            applicationIdSuffix = ".next"
            versionNameSuffix = "-next"
            matchingFallbacks += listOf("debug")
        }

        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("androidx.core:core-ktx:1.13.1")
    implementation("com.google.android.gms:play-services-wearable:18.2.0")
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.json:json:20240303")
}
