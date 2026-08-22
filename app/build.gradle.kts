import java.util.Properties
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.application)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
    alias(libs.plugins.kotlin.serialization)

    id("com.google.devtools.ksp")
}

kotlin {
    androidTarget {
        @OptIn(ExperimentalKotlinGradlePluginApi::class)
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }
    
    jvm("desktop")
    
    sourceSets {
        val desktopMain by getting
        
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            
            // Media3 (ExoPlayer)
            implementation("androidx.media3:media3-exoplayer:1.6.0")
            implementation("androidx.media3:media3-ui:1.6.0")
            implementation("androidx.media3:media3-session:1.6.0")
            
            // RTSP Streaming via Hardware (MediaProjection)
            implementation("com.github.pedroSG94.RootEncoder:library:2.7.2")
            implementation("com.github.pedroSG94:RTSP-Server:1.4.1") {
                exclude(group = "com.github.pedroSG94.RootEncoder")
            }
            
            // Palette API (Removed, using KMPalette in commonMain)
            
            // DI
            implementation(libs.koin.android)
            implementation(libs.koin.androidx.compose)
        }
        
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(compose.components.uiToolingPreview)
            
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            
            implementation("androidx.navigation:navigation-compose:2.8.0")
            implementation("androidx.compose.material:material-icons-extended:1.6.0")
            implementation("io.coil-kt:coil-compose:2.6.0")
            
            implementation("androidx.room:room-runtime:2.7.0-alpha13")
            
            implementation("com.squareup.retrofit2:retrofit:2.11.0")
            implementation("com.squareup.retrofit2:converter-gson:2.11.0")
            implementation("com.github.TeamNewPipe:NewPipeExtractor:v0.26.4")
            
            implementation(libs.koin.core)
            implementation(libs.kmpalette.core)
        }
        
        desktopMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-swing:1.10.1")
        }
    }
}

android {
    namespace = "com.example.beatpulse"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.polonio.duwave"
        minSdk = 24
        targetSdk = 35
        versionCode = 9
        versionName = "2.0.1"
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
            isShrinkResources = false
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
    sourceSets["main"].manifest.srcFile("src/androidMain/AndroidManifest.xml")
    sourceSets["main"].res.srcDirs("src/androidMain/res")
    sourceSets["main"].resources.srcDirs("src/commonMain/resources")
}

dependencies {
    ksp("androidx.room:room-compiler:2.7.0-alpha13")

}
