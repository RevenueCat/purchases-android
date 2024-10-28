-keep class com.revenuecat.** { *; }
-keep class androidx.lifecycle.DefaultLifecycleObserver
-dontwarn com.google.errorprone.annotations.CanIgnoreReturnValue
-dontwarn com.google.errorprone.annotations.Immutable
-dontwarn com.amazon.**
-dontwarn coil.**
# Adding temporarily to fix issue after adding kotlin serialization
-dontwarn java.lang.ClassValue

# START Keep kotlinx.serialization annotations.

# Keep `Companion` object fields of serializable classes.
# This avoids serializer lookup through `getDeclaredClasses` as done for named companion objects.
-if @kotlinx.serialization.Serializable class **
-keepclassmembers class <1> {
    static <1>$Companion Companion;
}

# Keep `serializer()` on companion objects (both default and named) of serializable classes.
-if @kotlinx.serialization.Serializable class ** {
    static **$* *;
}
-keepclassmembers class <2>$<3> {
    kotlinx.serialization.KSerializer serializer(...);
}

# Keep `INSTANCE.serializer()` of serializable objects.
-if @kotlinx.serialization.Serializable class ** {
    public static ** INSTANCE;
}
-keepclassmembers class <1> {
    public static <1> INSTANCE;
    kotlinx.serialization.KSerializer serializer(...);
}

# @Serializable and @Polymorphic are used at runtime for polymorphic serialization.
-keepattributes RuntimeVisibleAnnotations,AnnotationDefault

# Serializer for classes with named companion objects are retrieved using `getDeclaredClasses`.
# If you have any, uncomment and replace classes with those containing named companion objects.
#-keepattributes InnerClasses # Needed for `getDeclaredClasses`.
#-if @kotlinx.serialization.Serializable class
#com.example.myapplication.HasNamedCompanion, # <-- List serializable classes with named companions.
#com.example.myapplication.HasNamedCompanion2
#{
#    static **$* *;
#}
#-keepnames class <1>$$serializer { # -keepnames suffices; class is kept when serializer() is kept.
#    static <1>$$serializer INSTANCE;
#}

# Adding these because when target Android is 14 but compile version is lower than 14 there are r8 issues
# https://github.com/RevenueCat/purchases-android/pull/1606
-keep class kotlinx.serialization.internal.ClassValueParametrizedCache$initClassValue$1 { ** computeValue(java.lang.Class); }
-keep class kotlinx.serialization.internal.ClassValueCache$initClassValue$1 { ** computeValue(java.lang.Class); }
# END Keep kotlinx.serialization annotations.

# The org.json package is part of the Android framework, so the classes are always available.
# However, sometimes it gets added to the app's classpath, either explicitly or transitively. When
# this happens, it becomes susceptible to be shrunk. The following rule avoids that.
-keep class org.json.* { *; }
