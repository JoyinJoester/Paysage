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

# JavaMail / Activation use provider lookup and reflective loading for transports
# and handlers. Keep them conservative so release shrinking does not break
# forwarding at runtime.
-keep class javax.mail.** { *; }
-keep class javax.activation.** { *; }
-keep class com.sun.mail.** { *; }
-keep class com.sun.activation.** { *; }
-dontwarn javax.mail.**
-dontwarn javax.activation.**
-dontwarn com.sun.mail.**
-dontwarn com.sun.activation.**

# Tink (pulled by AndroidX Security Crypto) references compile-time annotation
# types that are not needed at runtime. R8 can safely ignore these.
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.CheckReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.google.errorprone.annotations.RestrictedApi
-dontwarn javax.annotation.Nullable
-dontwarn javax.annotation.concurrent.GuardedBy

# lpac-jni calls these Kotlin/Java APIs from native code by exact class,
# method, field, and constructor names. R8 must not rename or strip them.
-keep,includedescriptorclasses class net.typeblog.lpac_jni.** { *; }
-keep,includedescriptorclasses class * implements net.typeblog.lpac_jni.ApduInterface { *; }
-keep,includedescriptorclasses class * implements net.typeblog.lpac_jni.HttpInterface { *; }
