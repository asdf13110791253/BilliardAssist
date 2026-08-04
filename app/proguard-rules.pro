# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.

# OpenCV 混淆规则
-keep class org.opencv.** { *; }
-dontwarn org.opencv.**

# 保留 JNI 接口
-keepclasseswithmembernames class * {
    native <methods>;
}
