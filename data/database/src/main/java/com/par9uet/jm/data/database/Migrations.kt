package com.par9uet.jm.data.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * 数据库迁移脚本。
 *
 * 与 [AppDatabase] 的 version 一一对应，缺一条升级就会在启动时抛异常
 * （没有配置破坏性重建兜底，这是有意的：静默清空下载记录比崩溃更难发现）。
 * 新增字段时同步补一条迁移，并在 MigrationTest 里加断言。
 */
val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE download_comics ADD COLUMN groupId INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE download_comics ADD COLUMN groupName TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE download_comics ADD COLUMN chapterName TEXT NOT NULL DEFAULT ''")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE download_comics ADD COLUMN tagList TEXT NOT NULL DEFAULT '[]'")
    }
}

val ALL_MIGRATIONS = arrayOf(MIGRATION_2_3, MIGRATION_3_4)
