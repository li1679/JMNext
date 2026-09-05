package com.par9uet.jm.data.database.di

import androidx.room.Room
import com.par9uet.jm.data.database.AppDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/** 新应用数据库：schema 与当前实体保持一致，不提供历史版本迁移。 */
val databaseModule = module {
    single {
        Room.databaseBuilder(
            androidContext(),
            AppDatabase::class.java,
            "app_database"
        )
            .build()
    }
    single { get<AppDatabase>().downloadComicDao() }
}
