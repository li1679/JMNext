package com.par9uet.jm.data.storage.di

import com.par9uet.jm.core.common.AppInitTask
import com.par9uet.jm.data.storage.CookieStorage
import com.par9uet.jm.data.storage.HistorySearchStorage
import com.par9uet.jm.data.storage.LocalSettingManager
import com.par9uet.jm.data.storage.LocalSettingStorage
import com.par9uet.jm.data.storage.ReadHistoryStorage
import com.par9uet.jm.data.storage.SecureStorage
import com.par9uet.jm.data.storage.UserStorage
import org.koin.dsl.bind
import org.koin.dsl.module

/** 存储层：本地持久化与其上的内存态封装。 */
val storageModule = module {
    single { SecureStorage(get()) }
    single { UserStorage(get()) }
    single { CookieStorage(get()) }
    single { LocalSettingStorage(get()) }
    single { HistorySearchStorage(get()) }
    single { ReadHistoryStorage(get()) }

    single { LocalSettingManager(get(), get()) } bind AppInitTask::class
}
