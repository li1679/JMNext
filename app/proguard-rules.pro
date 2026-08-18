# Project ProGuard / R8 rules.
#
# 原则：只保留「运行时靠反射 / 名字查找」的东西。
# 之前这里有大量 `-keep class com.par9uet.jm.utils.** { *; }` 这类整包规则，
# 把 utils / store / repository / storage / di 全保留了下来，
# 等于 isMinifyEnabled=true 基本没生效 —— 既不缩包也不混淆。
# Koin 用的是编译期 lambda（single { ... }），不需要保留业务类；
# 真正需要 keep 的只有 Gson 反序列化用到的模型。

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

# Gson：字段名即 JSON 键，参与序列化的模型不能被混淆
-keep class com.par9uet.jm.retrofit.model.** { *; }
-keep class com.par9uet.jm.data.models.** { *; }
-keep class com.par9uet.jm.database.model.** { *; }
-keep class com.par9uet.jm.ui.models.** { *; }
# 本地持久化（设置 / Cookie / 备份）也走 Gson
-keep class com.par9uet.jm.store.BackupMeta { *; }
-keep class com.par9uet.jm.store.BackupFile { *; }
-keep class com.par9uet.jm.store.BackupContentOptions { *; }
-keep class com.par9uet.jm.store.ComicCacheBackup { *; }
-keep class com.par9uet.jm.store.ComicGroupBackup { *; }
-keep class com.par9uet.jm.store.ChapterBackup { *; }
-keep class com.par9uet.jm.cache.DownloadComicCacheConfig { *; }
-keep class com.par9uet.jm.cache.DownloadComicCacheChapter { *; }
-keep class com.par9uet.jm.utils.SimpleRecommender$PreferenceData { *; }
-keepclassmembers,allowobfuscation class * {
    @com.google.gson.annotations.SerializedName <fields>;
}

# Room 与 WorkManager：类名在运行时被查找
-keep class * extends androidx.room.RoomDatabase { *; }
-keep class com.par9uet.jm.database.dao.** { *; }
-keep class * extends androidx.work.ListenableWorker { *; }
-dontwarn androidx.room.**
-dontwarn androidx.work.**

-dontwarn org.koin.**

# JMComic-Api-Java: keep AndroidImageProcessor and SPI service files so
# ServiceLoader can discover the Android-compatible ImageProcessor at runtime
# instead of falling back to AwtImageProcessor (which uses java.awt unavailable on Android).
-keep class io.github.jukomu.jmcomic.android.support.** { *; }
-keep class io.github.jukomu.jmcomic.core.image.spi.** { *; }
-dontwarn io.github.jukomu.jmcomic.core.image.AwtImageProcessor
-keepnames class io.github.jukomu.jmcomic.core.image.spi.** { *; }
# 内置 API 的模型同样经 Gson/反射构造
-keep class io.github.jukomu.jmcomic.api.model.** { *; }
-keep class io.github.jukomu.jmcomic.core.config.** { *; }

# Transitive dependency com.luciad.imageio.webp references javax.imageio.* which
# is not available on Android. Android natively supports WebP via BitmapFactory,
# so these classes are never used at runtime.
-dontnote com.luciad.imageio.webp.**
-dontwarn com.luciad.imageio.webp.**
-dontwarn javax.imageio.ImageReader
-dontwarn javax.imageio.spi.ImageReaderSpi
-dontwarn javax.imageio.spi.ImageWriterSpi
-dontwarn javax.imageio.stream.ImageInputStream
-dontwarn javax.imageio.stream.ImageOutputStream

# OkHttp cookies are persisted with Gson.
-keep class okhttp3.Cookie { *; }
-dontwarn okhttp3.**

# Core library desugaring: keep desugared java.time classes (j$.* package)
# Prevents R8 from stripping desugared java.time APIs in release builds on Android 6/7
-keep class j$.** { *; }
-dontwarn j$.**
-dontwarn java.time.**
