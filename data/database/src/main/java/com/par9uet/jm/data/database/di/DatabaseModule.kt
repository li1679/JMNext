package com.par9uet.jm.data.database.di

import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.par9uet.jm.data.database.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** 数据库层：Room 实例、迁移脚本与 DAO。 */
val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "app_database"
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            .fallbackToDestructiveMigration(false)
            .build()
    }
    single { get<AppDatabase>().downloadComicDao() }
}

private val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE download_comics ADD COLUMN groupId INTEGER NOT NULL DEFAULT 0")
        db.execSQL("ALTER TABLE download_comics ADD COLUMN groupName TEXT NOT NULL DEFAULT ''")
        db.execSQL("ALTER TABLE download_comics ADD COLUMN chapterName TEXT NOT NULL DEFAULT ''")
    }
}

private val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE download_comics ADD COLUMN tagList TEXT NOT NULL DEFAULT '[]'")
    }
}
