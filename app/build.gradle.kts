plugins { id("com.android.application") }

android {
    namespace = "com.geostruct.field"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.geostruct.field"
        minSdk = 26
        targetSdk = 34
        versionCode = 11
        versionName = "3.8"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // Signed with the debug key: installs immediately, not Play-Store ready.
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    lint {
        abortOnError = false
    }
}

dependencies {
    implementation("androidx.webkit:webkit:1.11.0")
}
