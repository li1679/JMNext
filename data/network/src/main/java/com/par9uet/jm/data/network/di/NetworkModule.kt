package com.par9uet.jm.data.network.di

import com.par9uet.jm.core.common.AppInitTask
import com.par9uet.jm.data.network.Retrofit
import com.par9uet.jm.data.network.converter.PrimitiveToRequestBodyConverterFactory
import com.par9uet.jm.data.network.converter.ResponseConverterFactory
import com.par9uet.jm.data.network.interceptor.BaseUrlInterceptor
import com.par9uet.jm.data.network.interceptor.InitInterceptor
import com.par9uet.jm.data.network.interceptor.ToastInterceptor
import com.par9uet.jm.data.network.interceptor.TokenInterceptor
import com.par9uet.jm.data.network.service.ComicService
import com.par9uet.jm.data.network.service.RemoteSettingService
import com.par9uet.jm.data.network.service.UserService
import org.koin.dsl.bind
import org.koin.dsl.module
import retrofit2.converter.scalars.ScalarsConverterFactory

/** 网络层：OkHttp/Retrofit 装配、拦截器链与各业务 Service。 */
val networkModule = module {
    single {
        Retrofit(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get()
        )
    } bind AppInitTask::class
    single<ComicService> { get<Retrofit>().createService(ComicService::class.java) }
    single<RemoteSettingService> { get<Retrofit>().createService(RemoteSettingService::class.java) }
    single<UserService> { get<Retrofit>().createService(UserService::class.java) }
    single { BaseUrlInterceptor(get()) }
    single { TokenInterceptor() }
    single { InitInterceptor(get()) }
    single { ToastInterceptor(get()) }
    single { ResponseConverterFactory(get()) }
    single { PrimitiveToRequestBodyConverterFactory() }
    single { ScalarsConverterFactory.create() }
}
