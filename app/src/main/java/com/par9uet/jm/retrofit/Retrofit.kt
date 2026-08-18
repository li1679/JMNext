package com.par9uet.jm.retrofit

import com.par9uet.jm.BuildConfig
import com.par9uet.jm.retrofit.converter.PrimitiveToRequestBodyConverterFactory
import com.par9uet.jm.retrofit.converter.ResponseConverterFactory
import com.par9uet.jm.retrofit.interceptor.BaseUrlInterceptor
import com.par9uet.jm.retrofit.interceptor.InitInterceptor
import com.par9uet.jm.retrofit.interceptor.ToastInterceptor
import com.par9uet.jm.retrofit.interceptor.TokenInterceptor
import com.par9uet.jm.storage.CookieStorage
import com.par9uet.jm.task.AppInitTask
import com.par9uet.jm.task.AppTaskInfo
import com.par9uet.jm.utils.applyTlsCompat
import com.par9uet.jm.utils.log
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
    initInterceptor: InitInterceptor,
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
            // 连接超时压短一些：BaseUrlInterceptor 会逐条试线路，
            // 单条等太久会让整体等待时间成倍放大
            .connectTimeout(8, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            // 含线路重试的总上限，避免故障转移把一次请求拖到无限久
            .callTimeout(45, TimeUnit.SECONDS)
            .addInterceptor(initInterceptor)
            // 放在 baseUrlInterceptor 外层：只对线路重试后的最终结果提示，
            // 中间每条失败线路不重复打扰用户
            .addInterceptor(toastInterceptor)
            .addInterceptor(baseUrlInterceptor)
            // 放在 baseUrlInterceptor 内层：每次换线路重试都会重算签名，
            // 保证时间戳始终新鲜
            .addInterceptor(tokenInterceptor)
            .apply {
                // HTTP 日志只在 debug 构建挂载：release 下它既是无谓开销，
                // 也会把完整请求 URL 打进 logcat
                if (BuildConfig.DEBUG) {
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
