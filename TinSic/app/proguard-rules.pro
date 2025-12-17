# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# Keep Firebase classes
-keep class com.google.firebase.** { *; }
-keep class com.google.android.gms.** { *; }

# Keep ExoPlayer classes
-keep class androidx.media3.** { *; }

# Keep data classes (for Firebase serialization)
-keep class com.tinsic.app.data.model.** { *; }

# Keep DataStore and Protobuf
-keep class androidx.datastore.** { *; }
-keep class androidx.datastore.preferences.protobuf.** { *; }
