package com.par9uet.jm.data.network

import com.par9uet.jm.core.common.md5

/**
 * 管理 API 请求的上下文信息。
 *
 * 每个请求都使用自己的时间戳：token 与响应解密密钥都由它派生，
 * 服务端会校验时间戳新鲜度，因此不能用进程启动时算好的固定值
 * （应用长时间驻留后台后该值会过期，表现为突然大面积请求失败）。
 *
 * TokenInterceptor 在发出请求前写入，ResponseConverterFactory 在
 * 拦截器链返回后于同一线程读取，两者之间用 ThreadLocal 传递。
 * 每个请求都必定经过 TokenInterceptor 覆盖写入，因此线程池复用线程
 * 不会读到上一个请求的残留值；也正因为读取发生在拦截器返回之后，
 * 不能在拦截器里清除该值，否则解密会拿不到正确的时间戳。
 */
object ApiContext {
    private val perRequestTimestamp = ThreadLocal<Long>()

    fun setTimestamp(ts: Long) {
        perRequestTimestamp.set(ts)
    }

    fun getTimestamp(): Long {
        return perRequestTimestamp.get() ?: (System.currentTimeMillis() / 1000)
    }

    /**
     * 获取数据解密密钥：md5(timestamp + API_TOKEN_SECRET)。
     */
    fun getDataDecryptKey(): String {
        return md5("${getTimestamp()}$API_TOKEN_SECRET")
    }
}
