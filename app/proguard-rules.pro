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

# 保持数据模型类不被混淆
-keep class com.ziegler.kighelper.data.** { *; }

# Gson 相关的通用混淆规则
-keepattributes Signature
-keepattributes *Annotation*
-dontwarn sun.misc.**
-keep class com.google.gson.** { *; }

# sherpa-onnx JNI 会按 Java/Kotlin 字段名读取 OfflineTtsConfig 等配置对象。
# release 混淆如果改名这些字段，native GetFieldID 会拿到 null 并直接 abort。
-keep class com.k2fsa.sherpa.onnx.** { *; }
-dontwarn com.k2fsa.sherpa.onnx.**

# ONNX Runtime Java 层同样包含 JNI 入口，保守 keep 以避免 release-only native 绑定问题。
-keep class ai.onnxruntime.** { *; }
-dontwarn ai.onnxruntime.**

-keepclasseswithmembernames class * {
    native <methods>;
}
