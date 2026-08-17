package com.par9uet.jm.store

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

/**
 * 全局提示。
 *
 * 内置去重节流：并发请求同时失败时（例如一条 API 线路整体失效），
 * 相同内容的提示会被反复触发，不加节流会在界面上刷成一片。
 */
class ToastManager {
    companion object {
        /** 同一条提示的最小间隔 */
        private const val THROTTLE_MS = 8000L
    }

    private val _message = MutableSharedFlow<String>()
    val message = _message.asSharedFlow()

    @Volatile
    private var lastMessage: String = ""

    @Volatile
    private var lastShownAt: Long = 0L

    fun showAsync(text: String) {
        val now = System.currentTimeMillis()
        synchronized(this) {
            if (text == lastMessage && now - lastShownAt < THROTTLE_MS) {
                return
            }
            lastMessage = text
            lastShownAt = now
        }
        CoroutineScope(Dispatchers.Main).launch {
            _message.emit(text)
        }
    }
}
