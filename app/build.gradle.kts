plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    id("com.google.gms.google-services") // Apply Google services plugin
}

android {
    namespace = "com.example.saplingsales"
    compileSdk = 35
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17  // Update to 17 or 21
        targetCompatibility = JavaVersion.VERSION_17  // Update to 17 or 21
    }
    tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
        kotlinOptions {
            jvmTarget = "17" // Make sure this matches Java version
        }
    }
    defaultConfig {
        applicationId = "com.example.saplingsales"
        minSdk = 21
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
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

    lint {
        baseline = file("lint-baseline.xml")
        abortOnError = false
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Firebase Dependencies
    implementation("com.google.firebase:firebase-auth-ktx:22.3.1")
    implementation("com.google.firebase:firebase-firestore-ktx:24.10.1")
    implementation("com.google.firebase:firebase-database-ktx:20.3.1")
    implementation("com.google.firebase:firebase-storage-ktx:20.2.1")
    implementation("androidx.cardview:cardview:1.0.0")

    // Circle Image View
    implementation("de.hdodenhof:circleimageview:3.1.0")

    // Google Sign-In
    implementation("com.google.android.gms:play-services-auth:20.7.0")
    implementation("com.google.android.gms:play-services-location:21.1.0")

    // Cashfree SDK
    implementation("com.cashfree.pg:api:2.2.0")

    implementation("com.razorpay:checkout:1.6.33")
    implementation(libs.androidx.gridlayout)
    // Testing Dependencies
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}

// Apply Google Services
apply(plugin = "com.google.gms.google-services")
