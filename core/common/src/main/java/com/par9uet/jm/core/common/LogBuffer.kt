package com.par9uet.jm.core.common

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

    // ArrayDeque：日志在解码热路径上高频调用，ArrayList.removeAt(0) 是 O(n)
    private val entries = ArrayDeque<LogEntry>(MAX_ENTRIES)

    // SimpleDateFormat 而非 java.time：兼容 Android 6
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
