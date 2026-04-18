plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.newshub"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.example.newshub"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        val supabaseUrl = (project.findProperty("SUPABASE_URL") as String?) ?: ""
        val supabaseAnonKey = (project.findProperty("SUPABASE_ANON_KEY") as String?) ?: ""
        val supabaseBucket = (project.findProperty("SUPABASE_PROFILE_BUCKET") as String?) ?: "profile-pictures"
        val supabaseProfileTable = (project.findProperty("SUPABASE_PROFILE_TABLE") as String?) ?: "users"
        val supabaseUserIdColumn = (project.findProperty("SUPABASE_PROFILE_USER_ID_COLUMN") as String?) ?: "auth_user_id"
        val supabasePhotoColumn = (project.findProperty("SUPABASE_PROFILE_PHOTO_COLUMN") as String?) ?: "profile_photo_url"
        val backendBaseUrl = (project.findProperty("BACKEND_BASE_URL") as String?) ?: "http://10.0.2.2:8080"

        buildConfigField("String", "SUPABASE_URL", "\"$supabaseUrl\"")
        buildConfigField("String", "SUPABASE_ANON_KEY", "\"$supabaseAnonKey\"")
        buildConfigField("String", "SUPABASE_PROFILE_BUCKET", "\"$supabaseBucket\"")
        buildConfigField("String", "SUPABASE_PROFILE_TABLE", "\"$supabaseProfileTable\"")
        buildConfigField("String", "SUPABASE_PROFILE_USER_ID_COLUMN", "\"$supabaseUserIdColumn\"")
        buildConfigField("String", "SUPABASE_PROFILE_PHOTO_COLUMN", "\"$supabasePhotoColumn\"")
        buildConfigField("String", "BACKEND_BASE_URL", "\"$backendBaseUrl\"")

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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        buildConfig = true
        viewBinding = true
    }
}

dependencies {
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("io.coil-kt:coil:2.7.0")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.8.1")
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.8.7")
    implementation("androidx.fragment:fragment-ktx:1.8.5")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.navigation.fragment.ktx)
    implementation(libs.androidx.navigation.ui.ktx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}