package com.par9uet.jm.data.network.interceptor

import com.par9uet.jm.data.storage.LocalSettingManager
import com.par9uet.jm.core.common.log
import com.par9uet.jm.core.common.logError
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

/**
 * 把占位 baseUrl 替换成用户选用的 API 线路。
 * CDN 域名寿命很短，失效时依次回退到候选线路，并记住最后可用的一条。
 */
class BaseUrlInterceptor(
    private val localSettingManager: LocalSettingManager
) : Interceptor {

    companion object {
        /** 单个请求最多尝试的线路数，避免网络不通时逐个等超时 */
        private const val MAX_ATTEMPTS = 3
    }

    /** 上次成功的线路，进程内共享 */
    @Volatile
    private var preferredBaseUrl: String? = null

    /**
     * 记录 preferredBaseUrl 是基于哪条用户设置得出的。
     * 用户在设置里换线路后必须让新选择立即生效，否则只要旧线路还连得通，
     * 它会因为排在候选列表首位而一直被优先使用，换线路等于没换。
     */
    @Volatile
    private var preferredForApi: String? = null

    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val candidates = buildCandidates()

        if (candidates.isEmpty()) {
            throw IOException("未配置可用的 API 线路")
        }

        // 带请求体的方法不做重试，避免重复提交
        val allowFailover = original.method == "GET" || original.method == "HEAD"
        val attempts = if (allowFailover) candidates.take(MAX_ATTEMPTS) else candidates.take(1)

        var lastFailure: IOException? = null

        for (baseUrl in attempts) {
            val request = rewrite(original, baseUrl) ?: continue
            try {
                val response = chain.proceed(request)
                if (!allowFailover || !shouldFailover(response.code)) {
                    preferredBaseUrl = baseUrl
                    preferredForApi = localSettingManager.localSettingState.value.api
                    return response
                }
                // 该线路明确不可用，关闭响应后换下一条
                logError("BaseUrlInterceptor", "线路 $baseUrl 返回 ${response.code}，尝试下一条")
                response.close()
                lastFailure = IOException("线路 $baseUrl 返回 ${response.code}")
            } catch (e: IOException) {
                logError("BaseUrlInterceptor", "线路 $baseUrl 连接失败：${e.message}")
                lastFailure = e
                if (!allowFailover) throw e
            }
        }

        throw lastFailure ?: IOException("所有 API 线路均不可用")
    }

    /** 候选线路：上次成功的 > 当前选中的 > 设置里其余的 */
    private fun buildCandidates(): List<String> {
        val setting = localSettingManager.localSettingState.value
        // 用户改过线路后，上次成功的那条不再享有优先权，否则新选择永远轮不上
        val preferred = preferredBaseUrl?.takeIf { preferredForApi == setting.api }
        return listOfNotNull(preferred, setting.api)
            .plus(setting.apiList)
            .map { it.trim().removeSuffix("/") }
            .filter { it.isNotEmpty() }
            .distinct()
    }

    /**
     * 用候选线路的 scheme/host/port 覆写请求地址。
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

    /** 这些状态码说明线路本身有问题，换一条可能就好了 */
    private fun shouldFailover(code: Int): Boolean =
        code == 404 || code == 502 || code == 503 || code == 504 || code == 521 || code == 522
}
