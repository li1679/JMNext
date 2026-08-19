package com.par9uet.jm.data.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.par9uet.jm.data.database.converter.ListStringToStringConverter
import com.par9uet.jm.data.database.dao.DownloadComicDao
import com.par9uet.jm.data.database.model.DownloadComic

@Database(entities = [DownloadComic::class], version = 4, exportSchema = false)
@TypeConverters(ListStringToStringConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun downloadComicDao(): DownloadComicDao
}
