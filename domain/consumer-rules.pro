# 本模块的混淆规则，随模块一起提供给使用方。

# 备份文件结构经 Gson 读写，字段名即 JSON 键
-keep class com.par9uet.jm.domain.store.BackupMeta { *; }
-keep class com.par9uet.jm.domain.store.BackupFile { *; }
-keep class com.par9uet.jm.domain.store.BackupContentOptions { *; }
-keep class com.par9uet.jm.domain.store.ComicCacheBackup { *; }
-keep class com.par9uet.jm.domain.store.ComicGroupBackup { *; }
-keep class com.par9uet.jm.domain.store.ChapterBackup { *; }
# 下载目录里的 config.json 同样经 Gson 读写，且需要跨版本兼容
-keep class com.par9uet.jm.domain.cache.DownloadComicCacheConfig { *; }
-keep class com.par9uet.jm.domain.cache.DownloadComicCacheChapter { *; }

# WorkManager 按类名反射实例化 Worker
-keep class * extends androidx.work.ListenableWorker { *; }
-dontwarn androidx.work.**
