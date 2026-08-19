package com.par9uet.jm

import android.app.Application
import com.par9uet.jm.core.common.AppEnv
import com.par9uet.jm.core.common.di.commonModule
import com.par9uet.jm.data.database.di.databaseModule
import com.par9uet.jm.data.network.di.networkModule
import com.par9uet.jm.data.repository.di.repositoryModule
import com.par9uet.jm.data.storage.di.storageModule
import com.par9uet.jm.di.appModule
import com.par9uet.jm.domain.cache.trimPicDecodeCache
import com.par9uet.jm.domain.di.domainModule
import com.par9uet.jm.domain.notification.NotificationIcon
import com.par9uet.jm.domain.notification.ensureAppNotificationChannels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.androidx.workmanager.koin.workManagerFactory
import org.koin.core.context.startKoin

/** 自下而上装配：底层基础设施先于依赖它的上层模块。 */
private val moduleList = listOf(
    commonModule,
    storageModule,
    networkModule,
    databaseModule,
    repositoryModule,
    domainModule,
    appModule
)

class JmApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // 下层模块拿不到宿主的 BuildConfig 与 R，这两项由应用壳注入一次
        AppEnv.init(BuildConfig.DEBUG)
        NotificationIcon.init(R.drawable.ic_download_notification)

        ensureAppNotificationChannels(this)

        startKoin {
            androidContext(this@JmApplication)
            workManagerFactory()
            modules(moduleList)
        }

        // 解码缓存没有上限也没有自动清理，长期使用会无限增长。
        // 放在后台做，不阻塞启动。
        CoroutineScope(Dispatchers.IO).launch {
            trimPicDecodeCache(this@JmApplication)
        }
    }
}
