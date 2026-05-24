# Jetpack Compose core keeps
-keep class androidx.compose.compiler.plugins.kotlin.ComposeVersion { *; }

# Media3 / ExoPlayer Keeps
-keep class androidx.media3.exoplayer.** { *; }
-keep class androidx.media3.ui.** { *; }
-dontwarn androidx.media3.**

# Firebase SDK keeps
-keep class com.google.firebase.** { *; }
-dontwarn com.google.firebase.**
-keepattributes *Annotation*,Signature,InnerClasses,EnclosingMethod

# Coil Image Loader
-keep class coil.** { *; }
-dontwarn coil.**

# Retrofit & OkHttp Network keep rules
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keep class retrofit2.** { *; }
-dontwarn retrofit2.**
-keep class okhttp3.** { *; }
-dontwarn okhttp3.**

# Moshi serializable Keep
-keep class com.squareup.moshi.** { *; }
-dontwarn com.squareup.moshi.**

# Prevent shrinking of serialized user data classes
-keepclassmembers class * {
    @com.squareup.moshi.Json <fields>;
}

# Keep project data models safe from obfuscation
-keep class com.example.data.** { *; }
-keep class com.example.service.** { *; }
