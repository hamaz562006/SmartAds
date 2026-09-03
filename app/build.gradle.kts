plugins {
    id("com.android.application")
    id("com.google.gms.google-services") apply false
}

if (file("google-services.json").exists() || file("src/debug/google-services.json").exists() || file("src/release/google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

android {
    namespace = "com.partharoypc.smartads.demo"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.partharoypc.smartads.demo"
        minSdk = 23
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (file("${rootDir}/debug.keystore").exists()) {
            create("debugConfig") {
                storeFile = file("${rootDir}/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            signingConfigs.findByName("debugConfig")?.let {
                signingConfig = it
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {

    implementation("androidx.appcompat:appcompat:1.7.1")
    implementation("com.google.android.material:material:1.13.0")
    implementation("androidx.activity:activity:1.12.3")
    implementation("androidx.constraintlayout:constraintlayout:2.2.1")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.3.0")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.7.0")
    implementation(project(":smartads"))
    implementation("com.google.android.gms:play-services-ads:25.0.0")

    // Firebase Remote Config
    implementation(platform("com.google.firebase:firebase-bom:33.10.0"))
    implementation("com.google.firebase:firebase-config")
}