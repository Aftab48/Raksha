# Keep TFLite classes
-keep class org.tensorflow.** { *; }
-keep class org.tensorflow.lite.** { *; }

# Keep Room entities
-keep class com.raksha.app.data.local.entity.** { *; }

# Keep Gson serialization for NCRB data
-keep class com.raksha.app.data.assets.** { *; }
-keepattributes Signature
-keepattributes *Annotation*
