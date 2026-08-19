package com.par9uet.jm.core.common

import android.util.Log

/** logcat 输出仅 debug 生效（release 下会带出 URL 与漫画 id）；LogBuffer 两种构建都保留，供应用内「日志查看」使用。 */
inline fun <reified T> T.log(msg: String) {
    val tag = T::class.java.simpleName
    writeLog(tag, msg)
}

fun log(tag: String, msg: String) {
    writeLog(tag, msg)
}

fun logError(tag: String, msg: String) {
    try {
        Log.e("[JM-MOBILE] $tag", msg)
        LogBuffer.appendError(tag, msg)
    } catch (_: Throwable) {
        // 日志失败不应影响业务逻辑
    }
}

@PublishedApi
internal fun writeLog(tag: String, msg: String) {
    try {
        if (AppEnv.isDebug) {
            Log.d("[JM-MOBILE] $tag", msg)
        }
        LogBuffer.append(tag, msg)
    } catch (_: Throwable) {
        // 日志失败不应影响业务逻辑
    }
}
