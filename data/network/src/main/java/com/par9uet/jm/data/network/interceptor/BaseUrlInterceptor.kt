package com.par9uet.jm.data.network.interceptor

import com.par9uet.jm.core.common.log
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/** 把 Retrofit 占位地址替换成仅供图片配置使用的内部地址。 */
class BaseUrlInterceptor : Interceptor {

    companion object {
        /** 仅供内部图片域名配置请求使用，漫画业务统一走 JmApiClient。 */
        const val INTERNAL_API_BASE_URL = "https://www.cdnhth.club"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val request = rewrite(original, INTERNAL_API_BASE_URL)
            ?: throw IOException("内部 API 地址无效")
        return chain.proceed(request)
    }

    /**
     * 用内部地址的 scheme/host/port 覆写请求地址。
     * 用 HttpUrl 解析而非手工切分字符串：带端口或带路径的地址
     * 直接塞进 host() 会抛异常，让请求整个失败。
     */
    private fun rewrite(request: Request, baseUrl: String): Request? {
        val parsed: HttpUrl = baseUrl.toHttpUrlOrNull()
            ?: "https://$baseUrl".toHttpUrlOrNull()
            ?: run {
                log("BaseUrlInterceptor: 忽略无法解析的 API 线路 $baseUrl")
                return null
            }

        val newUrl = request.url.newBuilder()
            .scheme(parsed.scheme)
            .host(parsed.host)
            .port(parsed.port)
            .build()

        return request.newBuilder().url(newUrl).build()
    }

}
