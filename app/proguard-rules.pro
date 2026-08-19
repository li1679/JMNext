# 应用级 ProGuard / R8 规则。
#
# 原则：
# 1. 只保留「运行时靠反射 / 名字查找」的东西；
# 2. 与某个模块的类绑定的规则，写在那个模块的 consumer-rules.pro 里，
#    不要集中到这里——规则匹配不到类时只会静默失效，而模块化拆分后
#    包名一旦调整，放在别处的规则不会有人想起来改。
#    （曾经就因为此处沿用了拆分前的旧包名，release 版所有 Gson 模型
#      的字段被混淆，反序列化出的对象字段全为 null。）
#
# 这里只留跨模块通用、以及第三方库要求的规则。

# Kotlin 元数据与协程
-keepattributes Signature
-keepattributes *Annotation*
-keepattributes RuntimeVisibleAnnotations
-keepattributes RuntimeInvisibleAnnotations
-keepattributes EnclosingMethod
-keepattributes InnerClasses

-keep class kotlin.Metadata { *; }
-dontwarn kotlinx.coroutines.**

# Retrofit：接口方法上的注解要在运行时可读，泛型签名要保留
-keepattributes RuntimeVisibleParameterAnnotations
-keepclasseswithmembers class * {
    @retrofit2.http.* <methods>;
}
-keep,allowobfuscation interface <1>
-dontwarn retrofit2.**
-dontwarn javax.annotation.**
-dontwarn org.codehaus.mojo.animal_sniffer.**

# Gson：显式标注了 JSON 键名的字段一律保留
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# 保留业务类名（成员名仍混淆，未使用代码仍会被移除）。
# 日志 tag 取自 `T::class.java.simpleName`，类名一旦混淆，
# 「日志查看」里就只剩 xz2 / ni0 这类无从判读的标识。
-keepnames class com.par9uet.jm.**

-dontwarn org.koin.**

# OkHttp 的 Cookie 经 Gson 持久化
-keep class okhttp3.Cookie { *; }
-dontwarn okhttp3.**

# 核心库脱糖：保留脱糖后的 java.time 实现（j$.* 包），
# 否则 Android 6/7 上 release 版会缺失这些 API
-keep class j$.** { *; }
-dontwarn j$.**
-dontwarn java.time.**
