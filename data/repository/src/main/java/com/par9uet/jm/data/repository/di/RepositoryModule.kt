package com.par9uet.jm.data.repository.di

import com.par9uet.jm.data.repository.ComicRepository
import com.par9uet.jm.data.repository.RemoteSettingRepository
import com.par9uet.jm.data.repository.UserRepository
import com.par9uet.jm.data.repository.impl.ComicRepositoryImpl
import com.par9uet.jm.data.repository.impl.EmbeddedClientManager
import com.par9uet.jm.data.repository.impl.RemoteSettingRepositoryImpl
import com.par9uet.jm.data.repository.impl.UserRepositoryImpl
import org.koin.dsl.bind
import org.koin.dsl.module

/** 仓库层：对上暴露统一接口，对下屏蔽「内置客户端 / HTTP 线路」的差异。 */
val repositoryModule = module {
    single { EmbeddedClientManager(get()) }
    single { ComicRepositoryImpl(get(), get(), get(), get(), get()) } bind ComicRepository::class
    single { UserRepositoryImpl(get(), get(), get(), get(), get()) } bind UserRepository::class
    single { RemoteSettingRepositoryImpl(get(), get()) } bind RemoteSettingRepository::class
}
