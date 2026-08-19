# 本模块的混淆规则，随模块一起提供给使用方。


# 内置数据源：ServiceLoader 需要按名字找到 Android 版 ImageProcessor，
# 否则会回落到使用 java.awt 的 AwtImageProcessor（Android 上不可用）
-keep class io.github.jukomu.jmcomic.android.support.** { *; }
-keep class io.github.jukomu.jmcomic.core.image.spi.** { *; }
-keepnames class io.github.jukomu.jmcomic.core.image.spi.** { *; }
-dontwarn io.github.jukomu.jmcomic.core.image.AwtImageProcessor
# 内置 API 的模型同样经 Gson/反射构造
-keep class io.github.jukomu.jmcomic.api.model.** { *; }
-keep class io.github.jukomu.jmcomic.core.config.** { *; }

# 传递依赖 com.luciad.imageio.webp 引用了 Android 上不存在的 javax.imageio.*，
# Android 由 BitmapFactory 原生支持 WebP，这些类在运行时不会被触及
-dontnote com.luciad.imageio.webp.**
-dontwarn com.luciad.imageio.webp.**
-dontwarn javax.imageio.**
