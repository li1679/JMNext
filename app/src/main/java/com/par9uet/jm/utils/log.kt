package com.par9uet.jm.utils

import android.util.Log
import com.par9uet.jm.BuildConfig

/**
 * 日志。
 *
 * logcat 输出只在 debug 构建生效：release 下它是纯开销，而且这些日志
 * 会带上 URL、漫画 id 等信息。应用内的「日志查看」是个正式功能，
 * 因此 LogBuffer 两种构建都保留。
 */
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
        if (BuildConfig.DEBUG) {
            Log.d("[JM-MOBILE] $tag", msg)
        }
        LogBuffer.append(tag, msg)
    } catch (_: Throwable) {
        // 日志失败不应影响业务逻辑
    }
}
