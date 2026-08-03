plugins { id("com.android.application") }

android {
    namespace = "com.geostruct.field"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.geostruct.field"
        minSdk = 26
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            // signed with the debug key so the artifact installs directly;
            // swap in your own keystore for distribution
            signingConfig = signingConfigs.getByName("debug")
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("androidx.webkit:webkit:1.11.0")
}
