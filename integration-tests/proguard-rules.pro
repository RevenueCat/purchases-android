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
-keep class com.revenuecat.purchases.** { *; }

# Needed due to robolectric update
-dontwarn javax.lang.model.element.Modifier
-dontwarn com.google.errorprone.annotations.IncompatibleModifiers

# The app and androidTest APKs are minified separately but share a single classloader at
# runtime, and the test APK links against the Kotlin runtime and AndroidX test plumbing that
# ship inside the app APK. R8 optimization prunes whatever the app itself stops referencing,
# so the runner dies with NoSuchMethodError/NoClassDefFoundError before any test executes.
# This is an internal test app, so the size cost of keeping them does not matter.
-keep class kotlin.** { *; }
-keep class androidx.tracing.** { *; }
