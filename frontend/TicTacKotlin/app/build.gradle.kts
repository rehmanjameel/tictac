plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
}

android {
    namespace = "io.xconn.tictackotlin"
    compileSdk = 35

    defaultConfig {
        applicationId = "io.xconn.tictackotlin"
        minSdk = 24
        targetSdk = 35
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

    buildFeatures {
        viewBinding = true
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
// https://mvnrepository.com/artifact/io.xconn/wampproto
    implementation("io.xconn:wampproto:0.1.1")


// https://mvnrepository.com/artifact/io.xconn/xconn
    implementation("io.xconn:xconn:0.1.0-alpha.4")

    implementation ("androidx.core:core-splashscreen:1.0.1")
    implementation ("com.airbnb.android:lottie:6.4.1") // Lottie for animation
    implementation ("nl.dionsegijn:konfetti-xml:2.0.2") // Konfetti for confetti effect

//    implementation("com.google.android.gms:play-services-ads:23.6.0")

    // Import the BoM for the Firebase platform
    implementation(platform("com.google.firebase:firebase-bom:33.8.0"))

    // Add the dependency for the Analytics library
    // When using the BoM, you don't specify versions in Firebase library dependencies
    implementation("com.google.firebase:firebase-analytics")
    implementation("de.hdodenhof:circleimageview:3.1.0")

}