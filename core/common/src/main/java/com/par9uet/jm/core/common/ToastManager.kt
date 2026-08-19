package com.par9uet.jm.core.common

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

/** 全局提示。内置去重节流：线路整体失效时相同提示会被反复触发，不节流会刷屏。 */
class ToastManager {
    companion object {
        /** 同一条提示的最小间隔 */
        private const val THROTTLE_MS = 8000L

        /** 去重表的容量上限，防止长时间运行后无限增长 */
        private const val MAX_TRACKED = 32
    }

    // 必须带缓冲 + tryEmit：收集方要等 Snackbar 显示完（数秒）才回来取下一条，
    // 用无缓冲的 emit 会挂起排队，导致提示在事件过去很久后成串弹出。
    private val _message = MutableSharedFlow<String>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val message = _message.asSharedFlow()

    // 按内容分别记时，否则两种错误交替出现时节流会失效
    private val lastShownAt = LinkedHashMap<String, Long>()

    fun showAsync(text: String) {
        val now = System.currentTimeMillis()
        synchronized(lastShownAt) {
            val previous = lastShownAt[text]
            if (previous != null && now - previous < THROTTLE_MS) {
                return
            }
            lastShownAt[text] = now
            if (lastShownAt.size > MAX_TRACKED) {
                val oldest = lastShownAt.keys.firstOrNull()
                if (oldest != null) lastShownAt.remove(oldest)
            }
        }
        _message.tryEmit(text)
    }
}
