package com.par9uet.jm.domain.di

import com.par9uet.jm.core.common.AppInitTask
import com.par9uet.jm.domain.coil.createAsyncImageLoader
import com.par9uet.jm.domain.store.AppUpdateDownloadManager
import com.par9uet.jm.domain.store.DownloadManager
import com.par9uet.jm.domain.store.DownloadToastAggregator
import com.par9uet.jm.domain.store.HistorySearchManager
import com.par9uet.jm.domain.store.ReadHistoryManager
import com.par9uet.jm.domain.store.RemoteSettingManager
import com.par9uet.jm.domain.store.UserManager
import com.par9uet.jm.domain.worker.DownloadComicWorker
import org.koin.androidx.workmanager.dsl.worker
import org.koin.dsl.bind
import org.koin.dsl.module

/** 服务层：跨页面共享的业务状态、下载编排与图片加载器。 */
val domainModule = module {
    single { UserManager(get(), get(), get(), get()) } bind AppInitTask::class
    single { RemoteSettingManager(get()) } bind AppInitTask::class
    single { HistorySearchManager(get()) } bind AppInitTask::class
    single { ReadHistoryManager(get()) } bind AppInitTask::class

    single { DownloadToastAggregator(get()) }
    single { AppUpdateDownloadManager(get(), get(), get()) }
    single { DownloadManager(get(), get(), get(), get()) }

    single { createAsyncImageLoader(get()) }

    worker { DownloadComicWorker(get(), get(), get(), get(), get(), get(), get(), get()) }
}
