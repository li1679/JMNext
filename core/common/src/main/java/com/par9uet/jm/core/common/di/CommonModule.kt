package com.par9uet.jm.core.common.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.Strictness
import com.par9uet.jm.core.common.InitManager
import com.par9uet.jm.core.common.LauncherDisguiseApplier
import com.par9uet.jm.core.common.ToastManager
import com.par9uet.jm.core.common.log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import org.koin.dsl.module

/**
 * 跨层共用的基础设施：全局协程作用域、序列化器、初始化闸门与提示总线。
 * 这些对象没有业务语义，网络层、存储层、服务层与界面层都会用到，因此放在最底层注册。
 */
val commonModule = module {
    single {
        CoroutineScope(SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, throwable ->
            log("全局协程捕获到了异常: $throwable")
        })
    }

    single<Gson> { GsonBuilder().setStrictness(Strictness.LENIENT).serializeNulls().create() }

    single { ToastManager() }
    single { InitManager() }
    single { LauncherDisguiseApplier(get()) }
}
