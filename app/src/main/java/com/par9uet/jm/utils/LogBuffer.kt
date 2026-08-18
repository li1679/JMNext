package com.par9uet.jm.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

data class LogEntry(
    val timestamp: String,
    val tag: String,
    val message: String,
    val level: String
) {
    val formatted: String get() = "[$timestamp][$level][$tag] $message"
}

object LogBuffer {
    private const val MAX_ENTRIES = 500

    // ArrayDeque：环形缓冲满后每次追加都要移除最老一条，
    // ArrayList.removeAt(0) 是 O(n)，而日志在解码等热路径上高频调用
    private val entries = ArrayDeque<LogEntry>(MAX_ENTRIES)

    // 使用 SimpleDateFormat 替代 java.time，确保 Android 6 兼容性
    private val dateFormatter = SimpleDateFormat("MM-dd HH:mm:ss", Locale.getDefault())

    @Synchronized
    fun append(tag: String, message: String, level: String = "D") {
        val time = dateFormatter.format(Date())
        entries.addLast(LogEntry(time, tag, message, level))
        while (entries.size > MAX_ENTRIES) {
            entries.removeFirst()
        }
    }

    @Synchronized
    fun appendError(tag: String, message: String) {
        append(tag, message, "E")
    }

    @Synchronized
    fun getLogs(): List<LogEntry> {
        return entries.toList()
    }

    @Synchronized
    fun getLogText(): String {
        return entries.joinToString("\n") { it.formatted }
    }

    @Synchronized
    fun clear() {
        entries.clear()
    }
}
