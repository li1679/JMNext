package com.par9uet.jm.retrofit.interceptor

import com.par9uet.jm.store.ToastManager
import com.par9uet.jm.utils.logError
import okhttp3.Interceptor
import okhttp3.Response

/**
 * 统一处理 HTTP 层失败的提示。
 *
 * 所有失败都会写入日志（可在「日志查看」中回溯，含具体 URL），
 * 但只有用户真正能据此做出反应的错误才弹提示：
 * 之前对每个非 2xx 响应都无条件弹窗，一条线路失效就会刷出成片的
 * 「网络错误: 404」。提示的去重节流由 ToastManager 统一负责。
 */
class ToastInterceptor(
    private val toastManager: ToastManager
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)

        if (!response.isSuccessful) {
            logError(
                "ToastInterceptor",
                "请求失败 HTTP ${response.code} ${request.method} ${request.url}"
            )
            describe(response.code)?.let { toastManager.showAsync(it) }
        }
        return response
    }

    /**
     * 把状态码翻译成用户看得懂的说明；返回 null 表示这个错误只记日志、不打扰用户。
     */
    private fun describe(code: Int): String? = when (code) {
        // 线路失效或接口路径变动，已有自动换线路兜底，弹出来用户也无从处理
        404 -> null
        401, 403 -> "登录状态已失效或该地区被限制访问"
        429 -> "请求过于频繁，请稍后再试"
        in 500..599 -> "服务器暂时无法响应（$code），可尝试切换 API 线路"
        else -> "网络错误：$code"
    }
}
