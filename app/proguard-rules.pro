# Ofuscación agresiva
-optimizationpasses 5
-allowaccessmodification
-repackageclasses ''
-overloadaggressively

# Mantener solo lo necesario del app
-keep class com.sensis.goodff.MainActivity
-keep class com.sensis.goodff.auth.** { *; }

# OkHttp
-dontwarn okhttp3.**
-dontwarn okio.**
-keep class okhttp3.** { *; }
-keep class okio.** { *; }

# JSON
-keep class org.json.** { *; }

# Android esencial
-keepattributes *Annotation*
-keepattributes SourceFile,LineNumberTable
-keep public class * extends android.app.Activity
-keep public class * extends android.app.Service
-keep public class * extends android.content.BroadcastReceiver

# Ofuscar todo lo demás agresivamente
-obfuscationdictionary proguard-dict.txt
-classobfuscationdictionary proguard-dict.txt
-packageobfuscationdictionary proguard-dict.txt
