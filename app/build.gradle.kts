plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    //id("dagger.hilt.android.plugin")
    //id("kotlin-android-extensions")
    //id("androidx.navigation.safeargs.kotlin")
}

android {
    namespace = "com.example.googleassistant"
    compileSdk = 33

    defaultConfig {
        applicationId = "com.example.googleassistant"
        minSdk = 24
        //noinspection OldTargetApi
        targetSdk = 33
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    kotlinOptions {
        jvmTarget = "1.8"
    }
    dataBinding{
        enable = true
    }
}

dependencies {
    //starting implementations
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.10.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("com.google.firebase:firebase-crashlytics-buildtools:2.9.9")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

    //recyclerView
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    // For control over item selection of both touch and mouse driven selection
    implementation("androidx.recyclerview:recyclerview-selection:1.1.0")

    //lifecycle
    implementation("androidx.lifecycle:lifecycle-extensions:2.2.0")
    //viewmodel
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.6.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.6.2")

    //room db
    implementation("androidx.room:room-runtime:2.6.0")
    //noinspection KaptUsageInsteadOfKsp
    kapt("androidx.room:room-compiler:2.6.0")

    implementation("androidx.room:room-ktx:2.6.0")
    testImplementation("androidx.room:room-testing:2.6.0")

    implementation("org.jetbrains.kotlin:kotlinx-coroutines-core:1.3.7")
    implementation("org.jetbrains.kotlinx-coroutines-android:1.3.7")
    //ML Kit Vision
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")

    implementation("com.github.shubham0204:Text2Summary-Android:alpha-05")

    implementation("com.theartofdev.edmodo:android-image-cropper:2.8.0")
    //OpenWeatherMap
    implementation("com.github.KwabenBerko:OpenWeatherMap-Android-Library:2.1.0")
    implementation("the.bot.box:horoscope-api:{latest-version}")
    implementation("com.github.shubham0204:Text2Summary-Android:alpha-05")

    //weather
    implementation("org.jetbrains.anko:anko")
}