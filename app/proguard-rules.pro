# LibretroDroid: native code (JNI) calls back into these classes by name and reads Kotlin/Java members.
-keep class com.swordfish.libretrodroid.** { *; }
-keepclassmembers class com.swordfish.libretrodroid.** { native <methods>; }
-dontwarn com.swordfish.libretrodroid.**

# kotlinx.serialization: keep generated serializers and companions of our @Serializable models.
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.**
-keep,includedescriptorclasses class app.totv.retro.**$$serializer { *; }
-keepclassmembers class app.totv.retro.** { *** Companion; }
-keepclasseswithmembers class app.totv.retro.** { kotlinx.serialization.KSerializer serializer(...); }

# OkHttp / Okio optional platform integrations
-dontwarn okhttp3.internal.platform.**
-dontwarn org.conscrypt.**
-dontwarn org.bouncycastle.**
-dontwarn org.openjsse.**
