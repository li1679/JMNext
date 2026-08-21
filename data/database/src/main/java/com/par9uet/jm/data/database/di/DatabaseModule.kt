package com.par9uet.jm.data.database.di

import androidx.room.Room
import com.par9uet.jm.data.database.ALL_MIGRATIONS
import com.par9uet.jm.data.database.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** 数据库层：Room 实例与 DAO。迁移脚本见 [com.par9uet.jm.data.database.Migrations]。 */
val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "app_database"
        )
            .addMigrations(*ALL_MIGRATIONS)
            // 不要加 fallbackToDestructiveMigration：它的参数只控制是否连非 Room 表
            // 一起删，调用本身就等于允许「缺迁移就重建库」，会静默清空下载记录。
            // 缺迁移时直接崩更好——问题在开发期就能暴露。
            .build()
    }
    single { get<AppDatabase>().downloadComicDao() }
}
