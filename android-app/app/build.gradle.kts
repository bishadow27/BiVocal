plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "com.example.bivoca"

    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.bivoca"

        minSdk = 24
        targetSdk = 37

        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"


    }

    buildFeatures {
        buildConfig = true
    }

    buildTypes {
        getByName("debug") {
            buildConfigField("String", "API_BASE_URL_OVERRIDE", "\"\"")
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"900150975285-tf2ogeummkum8fdopho2gbgd9476gfhv.apps.googleusercontent.com\"")
        }

        create("e2e") {
            initWith(getByName("debug"))
            matchingFallbacks += listOf("debug")
            applicationIdSuffix = ".e2e"
            versionNameSuffix = "-e2e"
            buildConfigField(
                "String",
                "API_BASE_URL_OVERRIDE",
                "\"http://10.0.2.2:3001/api/v1/\""
            )
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"900150975285-tf2ogeummkum8fdopho2gbgd9476gfhv.apps.googleusercontent.com\"")
        }

        release {
            buildConfigField("String", "API_BASE_URL_OVERRIDE", "\"\"")
            buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"900150975285-tf2ogeummkum8fdopho2gbgd9476gfhv.apps.googleusercontent.com\"")
            optimization {
                enable = false
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

}

dependencies {

    // =========================
    // Android mặc định
    // =========================
    implementation(libs.activity.ktx)
    implementation(libs.appcompat)
    implementation(libs.constraintlayout)
    implementation(libs.material)
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")

    // =========================
    // Retrofit
    // Android -> Backend REST API
    // =========================
    implementation("com.squareup.retrofit2:retrofit:2.11.0")

    // JSON <-> Java Object
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")

    // =========================
    // OkHttp
    // Xem request / response trong Logcat
    // =========================
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // =========================
    // MVVM
    // ViewModel + LiveData
    // =========================
    implementation("androidx.lifecycle:lifecycle-viewmodel:2.8.7")
    implementation("androidx.lifecycle:lifecycle-livedata:2.8.7")
    implementation("androidx.credentials:credentials:1.6.0")
    implementation("androidx.credentials:credentials-play-services-auth:1.6.0")
    implementation("com.google.android.libraries.identity.googleid:googleid:1.2.1")

    // =========================
    // Test
    // =========================
    testImplementation(libs.junit)
    androidTestImplementation(libs.espresso.core)
    androidTestImplementation(libs.ext.junit)
}
