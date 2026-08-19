package com.par9uet.jm.data.repository.impl

import com.par9uet.jm.data.storage.CookieStorage
import com.par9uet.jm.core.common.log
import io.github.jukomu.jmcomic.api.enums.ClientType
import io.github.jukomu.jmcomic.core.client.impl.JmApiClient
import io.github.jukomu.jmcomic.core.config.JmConfiguration
import io.github.jukomu.jmcomic.core.net.OkHttpBuilder
import okhttp3.Cookie
import java.time.Duration

/**
 * 共享的 JmApiClient 实例：login() 写入的登录态与 Cookie 需被所有 Repository 看到，
 * 否则内置 API 模式下 POST（如创建收藏夹）会返回 401。
 *
 * Android 6 兼容：JmDomainManager 域名探活走 ForkJoinPool，在 Android 6 上可能初始化失败
 * 导致 blockUntilInitialized 永久阻塞，故用守护线程超时解除。
 */
class EmbeddedClientManager(
    private val cookieStorage: CookieStorage,
) {
    @Volatile
    private var client: JmApiClient? = null

    fun getClient(): JmApiClient {
        return client ?: synchronized(this) {
            client ?: createClient().also { client = it }
        }
    }

    private fun createClient(): JmApiClient {
        val config = JmConfiguration.Builder()
            .clientType(ClientType.API)
            .timeout(Duration.ofSeconds(20))
            .imageTimeout(Duration.ofSeconds(60))
            .downloadThreadPoolSize(2)
            .domainProbeTimeoutMs(3000)
            .build()
        val context = OkHttpBuilder.build(config)
        val domainManager = context.domainManager
        val clientWithCookieInjection = context.client.newBuilder()
            .addInterceptor { chain ->
                val cookies = cookieStorage.get()
                val request = if (cookies.isNotEmpty()) {
                    val cookieHeader = cookies.joinToString("; ") { "${it.name}=${it.value}" }
                    chain.request().newBuilder()
                        .header("Cookie", cookieHeader)
                        .build()
                } else {
                    chain.request()
                }
                val response = chain.proceed(request)
                // 从响应头提取 Set-Cookie，同步到 cookieStorage，保证登录态持久化
                val setCookieHeaders = response.headers("Set-Cookie")
                if (setCookieHeaders.isNotEmpty()) {
                    val newCookies = setCookieHeaders.mapNotNull { Cookie.parse(request.url, it) }
                    if (newCookies.isNotEmpty()) {
                        val existing = cookieStorage.get().toMutableList()
                        val newKeys = newCookies.map { "${it.domain}:${it.path}:${it.name}" }.toSet()
                        existing.removeAll { "${it.domain}:${it.path}:${it.name}" in newKeys }
                        existing.addAll(newCookies)
                        cookieStorage.set(existing)
                    }
                }
                response
            }
            .build()
        val jmClient = JmApiClient(config, clientWithCookieInjection, context.cookieManager, domainManager)

        // 守护线程：域名探活初始化超时后强制解除阻塞，避免 Android 6 上永久卡死
        Thread({
            try {
                // 等待 8 秒让域名探活完成
                Thread.sleep(8000)
                if (!domainManager.isInitialized) {
                    log("EmbeddedClientManager: 域名探活初始化超时，强制解除阻塞")
                    domainManager.setInitialized(true)
                }
            } catch (e: InterruptedException) {
            }
        }, "embedded-domain-init-guard").apply {
            isDaemon = true
            start()
        }

        return jmClient
    }
}
