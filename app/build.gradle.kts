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
        buildConfigField(
            type = "String",
            name = "OPENAI_API_KEY",
            value = properties.getProperty("OPENAI_API_KEY", "\"YOUR_DEFAULT_OR_MISSING_KEY_PLACEHOLDER\"")
        )
        android.buildFeatures.buildConfig = true
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
}

dependencies {

    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0");
    // LiveData (Optional, can use StateFlow instead)
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0");
    // Coroutines for background tasks
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3");
    // For network requests (Example: Retrofit and Moshi/Gson)
    implementation("com.squareup.retrofit2:retrofit:2.9.0");
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0"); // Or converter-gson
    implementation("com.squareup.moshi:moshi-kotlin:1.15.0"); // If using Moshi
    // OkHttp (Retrofit's underlying HTTP client, often needed for logging interceptors)
    implementation("com.squareup.okhttp3:okhttp:4.11.0");
    implementation("com.squareup.okhttp3:logging-interceptor:4.11.0"); // For logging network requests

    // Jetpack Compose (if you're using it for UI)
    implementation("androidx.compose.runtime:runtime-livedata:1.6.3"); // To observe LiveData in Compose
    // or
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0"); // To collect StateFlow in Compose

    // Material Components for Button (if using XML)
    implementation("com.google.android.material:material:1.11.0");
    implementation("com.android.volley:volley:1.2.1");
    implementation("com.google.code.gson:gson:2.11.0");

}