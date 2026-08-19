package com.par9uet.jm.data.network.interceptor

import com.par9uet.jm.core.common.ToastManager
import com.par9uet.jm.core.common.logError
import okhttp3.Interceptor
import okhttp3.Response

/**
 * HTTP 失败一律写日志（「日志查看」含具体 URL），但只有用户能据此反应的错误才弹提示。
 * 对每个非 2xx 都弹窗会在线路失效时刷屏。去重节流由 ToastManager 负责。
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

    /** 返回 null 表示只记日志、不打扰用户 */
    private fun describe(code: Int): String? = when (code) {
        // 线路失效或接口路径变动，已有自动换线路兜底，弹出来用户也无从处理
        404 -> null
        401, 403 -> "登录状态已失效或该地区被限制访问"
        429 -> "请求过于频繁，请稍后再试"
        in 500..599 -> "服务器暂时无法响应（$code），可尝试切换 API 线路"
        else -> "网络错误：$code"
    }
}
