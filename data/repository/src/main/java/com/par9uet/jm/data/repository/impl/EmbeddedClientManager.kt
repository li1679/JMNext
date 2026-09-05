package com.par9uet.jm.data.repository.impl

import com.par9uet.jm.data.storage.CookieStorage
import io.github.jukomu.jmcomic.api.enums.ClientType
import io.github.jukomu.jmcomic.core.client.impl.JmApiClient
import io.github.jukomu.jmcomic.core.config.JmConfiguration
import io.github.jukomu.jmcomic.core.net.OkHttpBuilder
import okhttp3.Cookie
import java.time.Duration
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

/**
 * 共享的 JmApiClient 实例：login() 写入的登录态与 Cookie 需被所有 Repository 看到，
 * 否则内置 API 模式下 POST（如创建收藏夹）会返回 401。
 *
 * 域名探活在后台完成；请求侧使用可取消的有限等待，失败时让真实错误返回到界面。
 */
class EmbeddedClientManager(
    private val cookieStorage: CookieStorage,
) {
    @Volatile
    private var client: JmApiClient? = null
    @Volatile
    private var isDomainInitialized: (() -> Boolean)? = null

    suspend fun getClient(): JmApiClient = withContext(Dispatchers.IO) {
        val current = client ?: synchronized(this@EmbeddedClientManager) {
            client ?: createClient().also { client = it }
        }
        awaitDomainInitialization()
        current
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
        isDomainInitialized = { domainManager.isInitialized }
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

        return jmClient
    }

    private suspend fun awaitDomainInitialization() {
        val initialized = isDomainInitialized ?: return
        if (initialized()) return
        withTimeoutOrNull(DOMAIN_INIT_TIMEOUT_MS) {
            while (!initialized()) delay(DOMAIN_INIT_POLL_MS)
        }
        check(initialized()) { "域名初始化失败" }
    }

    private companion object {
        const val DOMAIN_INIT_TIMEOUT_MS = 8_000L
        const val DOMAIN_INIT_POLL_MS = 50L
    }
}
