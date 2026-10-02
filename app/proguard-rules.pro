# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
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

# MyTools finance models and parser
-keep class com.example.aidetest.FinanceTx { *; }
-keep class com.example.aidetest.FinanceCategories { *; }
-keep class com.example.aidetest.MainActivity { public <init>(); *; }


# ---- R8 (Step 16) ----
# Jaga seluruh kode aplikasi: refleksi pada tool opsional, receiver/service di manifest, dan nama kelas di stack trace.
-keep class com.example.aidetest.** { *; }
-dontobfuscate
-keepattributes SourceFile,LineNumberTable,Signature,InnerClasses,EnclosingMethod,*Annotation*

# Library: aturan konsumen (ML Kit, WorkManager, Lottie) sudah ikut AAR; ini hanya pengaman peringatan.
-dontwarn com.google.mlkit.**
-dontwarn com.airbnb.lottie.**
-dontwarn javax.annotation.**
