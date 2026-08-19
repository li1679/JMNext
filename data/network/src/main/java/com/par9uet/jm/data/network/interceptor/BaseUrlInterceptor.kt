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
 * 把占位 baseUrl 替换成用户实际选用的 API 线路。
 *
 * 禁漫的 CDN 域名寿命很短，单一域名一旦失效整个应用都不可用，
 * 因此这里会在当前线路失败时依次回退到设置里的其它候选线路，
 * 并记住最后一个可用的线路，避免每次请求都从已失效的域名开始试探。
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

    /**
     * 候选线路：优先用上次成功的线路，然后是用户当前选中的，最后是设置里的其余线路。
     */
    private fun buildCandidates(): List<String> {
        val setting = localSettingManager.localSettingState.value
        return listOfNotNull(preferredBaseUrl, setting.api)
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
