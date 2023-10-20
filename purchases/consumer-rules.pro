-keep class com.revenuecat.** { *; }
-keep class androidx.lifecycle.DefaultLifecycleObserver
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.amazon.**
-dontwarn coil.**
# Adding temporarily to fix issue after adding kotlin serialization
-dontwarn java.lang.ClassValue
