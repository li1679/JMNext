package com.par9uet.jm.data.network

import com.par9uet.jm.core.common.md5

/**
 * 每请求独立的时间戳：token 与响应解密密钥均由它派生，服务端校验其新鲜度，
 * 用进程启动时的固定值会在应用久驻后台后大面积失败。
 *
 * TokenInterceptor 发出请求前写入，ResponseConverterFactory 于拦截器链返回后同线程读取。
 * 不可在拦截器内清除，否则解密取不到时间戳。
 */
object ApiContext {
    private val perRequestTimestamp = ThreadLocal<Long>()

    fun setTimestamp(ts: Long) {
        perRequestTimestamp.set(ts)
    }

    fun getTimestamp(): Long {
        return perRequestTimestamp.get() ?: (System.currentTimeMillis() / 1000)
    }

    /** 数据解密密钥：md5(timestamp + API_TOKEN_SECRET) */
    fun getDataDecryptKey(): String {
        return md5("${getTimestamp()}$API_TOKEN_SECRET")
    }
}
