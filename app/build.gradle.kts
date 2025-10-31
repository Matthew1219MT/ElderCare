import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
}

val properties = Properties()
val localPropertiesFile = project.rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    properties.load(localPropertiesFile.inputStream())
}

android {
    namespace = "com.eldercare.eldercare"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.eldercare.eldercare"
        minSdk = 27
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // BuildConfig fields - Get API key from local.properties
        buildConfigField(
            type = "String",
            name = "OPENAI_API_KEY",
            value = "\"${properties.getProperty("OPENAI_API_KEY", "")}\""
        )

        buildConfigField(
            type = "String",
            name = "FACETRACKER_API_BASE_URL",
            value = "\"${properties.getProperty("FACETRACKER_API_BASE_URL", "")}\""
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
        dataBinding = true
        viewBinding = true
        buildConfig = true
    }
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.core.ktx)

    // ARCore and Camera
    implementation(libs.arcore)
    implementation(libs.camera2)
    implementation(libs.camerax.core)
    implementation(libs.camerax.camera2)
    implementation(libs.camerax.lifecycle)
    implementation(libs.camerax.view)

    // ML Kit Face Detection
    implementation(libs.mlkit.face.detection)
    implementation(libs.mlkit.face.mesh)  // ADDED - Critical for 3D face mesh

    // Networking
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)
    implementation(libs.moshi)
    implementation(libs.retrofit.moshi)

    // ViewModel and LiveData
    implementation(libs.lifecycle.viewmodel)
    implementation(libs.lifecycle.livedata)
    implementation(libs.lifecycle.runtime.compose)

    // Coroutines
    implementation(libs.coroutines.android)

    // Compose runtime-livedata
    implementation(libs.compose.runtime.livedata)

    // OpenGL and 3D rendering
    implementation(libs.filament.android)
    implementation(libs.gltfio.android)

    // Permissions
    implementation(libs.dexter)

    // Volley
    implementation(libs.volley)

    // Fragment
    implementation(libs.fragment)
    implementation(libs.fragment.ktx)

    // Google Play Services
    implementation(libs.play.services.location)
    implementation("com.google.android.gms:play-services-maps:18.1.0")
    implementation("androidx.fragment:fragment:1.6.1")
    implementation("com.google.android.libraries.places:places:3.3.0")

    // MPAndroidChart - For pie chart visualization in scan results
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}