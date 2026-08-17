package com.par9uet.jm.retrofit.interceptor

import com.par9uet.jm.retrofit.API_TOKEN_SECRET
import com.par9uet.jm.retrofit.API_VERSION
import com.par9uet.jm.retrofit.ApiContext
import com.par9uet.jm.utils.md5
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

/**
 * 为每个请求附加禁漫接口要求的签名头。
 *
 * 两种 API 模式使用完全相同的算法与密钥，不再分支：
 * 之前内置模式用的 "18comicAPP" 并非有效密钥，会让签名校验失败。
 */
class TokenInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest: Request = chain.request()

        // 每个请求都取当前时间戳，避免长时间运行后签名过期
        val timestamp = System.currentTimeMillis() / 1000
        val token = md5("$timestamp$API_TOKEN_SECRET")
        val tokenParam = "$timestamp,$API_VERSION"

        // 供 ResponseConverterFactory 解密响应体使用
        ApiContext.setTimestamp(timestamp)

        val newRequest = originalRequest.newBuilder()
            .header("tokenparam", tokenParam)
            .header("token", token)
            .build()
        return chain.proceed(newRequest)
    }
}
