# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.kts.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
#-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# Keep Navigation Routes (Serializable)
-keepattributes *Annotation*, Signature, InnerClasses
-keepclassmembers class * {
    @kotlinx.serialization.Serializable *;
}
-keep class * {
    @kotlinx.serialization.Serializable <fields>;
}

# Keep Data Classes & Models
-keep class com.example.beatpulse.data.** { *; }
-keep class com.example.beatpulse.** { *; }

# Keep ViewModels
-keep class * extends androidx.lifecycle.ViewModel { *; }

# Keep ExoPlayer / Media3
-keep class androidx.media3.** { *; }
-dontwarn androidx.media3.**

# Keep Coil
-keep class io.coil-kt.** { *; }
-dontwarn io.coil-kt.**
