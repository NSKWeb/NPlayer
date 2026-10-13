# Retrofit / Gson model classes are reflected over.
-keepattributes Signature
-keepattributes *Annotation*
-keep class com.example.nplayer.data.remote.model.** { *; }

-dontwarn okhttp3.**
-dontwarn okio.**
-dontwarn retrofit2.**
