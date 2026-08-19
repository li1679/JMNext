# 本模块的混淆规则，随模块一起提供给使用方。

# Room 实体与 DAO：类名/字段名在运行时被查找
-keep class com.par9uet.jm.data.database.model.** { *; }
-keep class com.par9uet.jm.data.database.dao.** { *; }
-keep class * extends androidx.room.RoomDatabase { *; }
-dontwarn androidx.room.**
