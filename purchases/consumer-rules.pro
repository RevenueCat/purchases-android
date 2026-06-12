# EnumDeserializerWithDefault matches JSON against enum CONSTANT NAMES read reflectively via
# Class.enumConstants (value.name.lowercase()) and silently falls back to a default on mismatch.
# Keep enum constant names so an obfuscated name can't turn into a silent wrong-value bug.
-keepclassmembers enum com.revenuecat.** {
    <fields>;
    public static **[] values();
    public static ** valueOf(java.lang.String);
}

-keep class androidx.lifecycle.DefaultLifecycleObserver
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.amazon.**
-dontwarn coil.**
# Adding temporarily to fix issue after adding kotlin serialization
-dontwarn java.lang.ClassValue

# These rules target kotlinx.serialization 1.5.1 (Kotlin 2.0.21).
# Adding these because when target Android is 14 but compile version is lower than 14 there are r8 issues
# https://github.com/RevenueCat/purchases-android/pull/1606
-keep class kotlinx.serialization.internal.ClassValueParametrizedCache$initClassValue$1 { ** computeValue(java.lang.Class); }
-keep class kotlinx.serialization.internal.ClassValueCache$initClassValue$1 { ** computeValue(java.lang.Class); }

# The org.json package is part of the Android framework, so the classes are always available.
# However, some apps add it to their classpath, either explicitly or transitively. When
# this happens, it becomes susceptible to be shrunk. The following rule avoids that.
-keep class org.json.* { *; }
