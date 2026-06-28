import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  id("com.google.devtools.ksp")
}

android {
    namespace = "com.example.beatpulse"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.polonio.duwave"
        minSdk = 24
        targetSdk = 35
        versionCode = 7
        versionName = "1.4.3"
    }

    val keystoreFile = file("../release.keystore")
    if (keystoreFile.exists()) {
        val properties = Properties()
        val localPropertiesFile = rootProject.file("local.properties")
        if (localPropertiesFile.exists()) {
            properties.load(localPropertiesFile.inputStream())
        }
        signingConfigs {
            create("release") {
                storeFile = keystoreFile
                storePassword = properties.getProperty("STORE_PASSWORD") ?: System.getenv("STORE_PASSWORD") ?: ""
                keyAlias = properties.getProperty("KEY_ALIAS") ?: "duwave"
                keyPassword = properties.getProperty("KEY_PASSWORD") ?: System.getenv("KEY_PASSWORD") ?: ""
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystoreFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
      compose = true
      aidl = false
      buildConfig = false
      shaders = false
    }

    packaging {
      resources {
        excludes += "/META-INF/{AL2.0,LGPL2.1}"
      }
    }

    dependenciesInfo {
        includeInApk = false
        includeInBundle = false
    }
}



dependencies {
  val composeBom = platform(libs.androidx.compose.bom)
  implementation(composeBom)
  androidTestImplementation(composeBom)

  // Core Android dependencies
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)

  // Arch Components
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.viewmodel.compose)

  // Compose
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  // Tooling
  debugImplementation(libs.androidx.compose.ui.tooling)
  // Instrumented tests
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  // Local tests: jUnit, coroutines, Android runner
  testImplementation(libs.junit)
  testImplementation(libs.kotlinx.coroutines.test)

  // Instrumented tests: jUnit rules and runners
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.androidx.test.ext.junit)
  androidTestImplementation(libs.androidx.test.runner)
  androidTestImplementation(libs.androidx.test.espresso.core)

  // Navigation
  implementation(libs.androidx.navigation3.ui)
  implementation(libs.androidx.navigation3.runtime)
  implementation(libs.androidx.lifecycle.viewmodel.navigation3)
  implementation("androidx.navigation:navigation-compose:2.8.0")

  // Media3 (ExoPlayer)
  implementation("androidx.media3:media3-exoplayer:1.6.0")
  implementation("androidx.media3:media3-ui:1.6.0")
  implementation("androidx.media3:media3-session:1.6.0")

  // Icons Extended
  implementation("androidx.compose.material:material-icons-extended:1.6.0")

  // Room
  implementation("androidx.room:room-runtime:2.7.0-alpha13")
  implementation("androidx.room:room-ktx:2.7.0-alpha13")
  ksp("androidx.room:room-compiler:2.7.0-alpha13")

  // Palette API
  implementation("androidx.palette:palette-ktx:1.0.0")

  // Coil for image loading
  implementation("io.coil-kt:coil-compose:2.6.0")
}
