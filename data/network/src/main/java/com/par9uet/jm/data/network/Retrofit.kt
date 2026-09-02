package com.par9uet.jm.data.network

import com.par9uet.jm.core.common.AppEnv
import com.par9uet.jm.data.network.converter.PrimitiveToRequestBodyConverterFactory
import com.par9uet.jm.data.network.converter.ResponseConverterFactory
import com.par9uet.jm.data.network.interceptor.BaseUrlInterceptor
import com.par9uet.jm.data.network.interceptor.ToastInterceptor
import com.par9uet.jm.data.network.interceptor.TokenInterceptor
import com.par9uet.jm.data.storage.CookieStorage
import com.par9uet.jm.core.common.AppInitTask
import com.par9uet.jm.core.common.AppTaskInfo
import com.par9uet.jm.core.common.applyTlsCompat
import com.par9uet.jm.core.common.log
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.util.concurrent.TimeUnit

class Retrofit(
    baseUrlInterceptor: BaseUrlInterceptor,
    toastInterceptor: ToastInterceptor,
    tokenInterceptor: TokenInterceptor,
    private val scalarsConverterFactory: ScalarsConverterFactory,
    private val responseConverterFactory: ResponseConverterFactory,
    private val primitiveToRequestBodyConverterFactory: PrimitiveToRequestBodyConverterFactory,
    private val cookieStorage: CookieStorage
) : AppInitTask {
    private val appTaskInfo = AppTaskInfo(
        taskName = "Retrofit 配置",
        sort = 1
    )
    private var cookieList = listOf<Cookie>()
    private val cookieJar = object : CookieJar {

        override fun saveFromResponse(
            url: HttpUrl,
            cookies: List<Cookie>
        ) {
            cookieList =
                (cookieList + cookies).associateBy { "${it.domain}:${it.path}:${it.name}" }.values.toList()
            cookieStorage.set(cookieList)
        }

        override fun loadForRequest(url: HttpUrl): List<Cookie> {
            cookieList = cookieStorage.get()
            return cookieList
        }

    }
    private val okHttpClient =
        OkHttpClient.Builder()
            // 远端图片域名配置仅用于启动时读取，避免拖慢应用启动
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            // 远端配置请求的总上限，失败时沿用图片域名兜底值
            .callTimeout(45, TimeUnit.SECONDS)
            // 远端图片配置失败时只提示一次
            .addInterceptor(toastInterceptor)
            .addInterceptor(baseUrlInterceptor)
            // 内部配置请求使用统一签名
            .addInterceptor(tokenInterceptor)
            .apply {
                // HTTP 日志只在 debug 构建挂载：release 下它既是无谓开销，
                // 也会把完整请求 URL 打进 logcat
                if (AppEnv.isDebug) {
                    addInterceptor(HttpLoggingInterceptor().apply {
                        level = HttpLoggingInterceptor.Level.BASIC
                    })
                }
            }
            .cookieJar(cookieJar)
            .applyTlsCompat()
            .build()
    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl("https://placeholder.com/") // 占位，会在 okhttp 的拦截器中进行动态替换
            .client(okHttpClient)
            .addConverterFactory(scalarsConverterFactory)
            .addConverterFactory(responseConverterFactory)
            .addConverterFactory(primitiveToRequestBodyConverterFactory)
            .build()
    }

    fun <T> createService(cls: Class<T>): T {
        val service = retrofit.create(cls)
        return service
    }

    fun clearCookie() {
        cookieList = listOf()
    }

    override suspend fun init() {
        log("Retrofit 开始初始化")
        log("恢复 Retrofit Cookie")
        cookieList = cookieStorage.get()
        log("已恢复 Retrofit Cookie")
        log("Retrofit 初始化结束")
    }

    override fun getAppTaskInfo(): AppTaskInfo = appTaskInfo
}
