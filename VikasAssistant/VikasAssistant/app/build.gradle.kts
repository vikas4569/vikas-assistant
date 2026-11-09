plugins { id("com.android.application"); id("org.jetbrains.kotlin.android") }
android {
    namespace = "com.vikas.assistant"
    compileSdk = 34
    defaultConfig { applicationId = "com.vikas.assistant"; minSdk = 26; targetSdk = 34; versionCode = 1; versionName = "1.0" }
    buildTypes { release { isMinifyEnabled = true } }
    compileOptions { sourceCompatibility = JavaVersion.VERSION_17; targetCompatibility = JavaVersion.VERSION_17 }
    kotlinOptions { jvmTarget = "17" }
}
dependencies { implementation("androidx.core:core-ktx:1.13.1") }
