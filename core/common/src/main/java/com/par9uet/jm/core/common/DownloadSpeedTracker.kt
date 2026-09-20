package com.par9uet.jm.core.common

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * 按任务采样并聚合到组；速度为压缩后落盘字节，不是网络流量。
 */
object DownloadSpeedTracker {
    private data class SpeedSample(
        val groupId: Int,
        val totalBytes: Long,
        val startedAtNanos: Long,
    )

    private val _speedByGroup = MutableStateFlow<Map<Int, Float>>(emptyMap())
    val speedByGroup: StateFlow<Map<Int, Float>> = _speedByGroup.asStateFlow()

    private val samplesByTask = mutableMapOf<Int, SpeedSample>()

    /**
     * 开始跟踪一个下载任务的速度
     */
    @Synchronized
    fun startTracking(taskId: Int, groupId: Int) {
        samplesByTask[taskId] = SpeedSample(
            groupId = groupId,
            totalBytes = 0L,
            startedAtNanos = System.nanoTime()
        )
        publish()
    }

    /**
     * 增加已下载字节数，并更新速度（bytes/s）
     */
    @Synchronized
    fun addBytes(taskId: Int, bytes: Long) {
        require(bytes >= 0)
        val sample = samplesByTask[taskId] ?: return
        val newTotal = sample.totalBytes + bytes
        samplesByTask[taskId] = sample.copy(totalBytes = newTotal)
        publish()
    }

    /**
     * 停止跟踪，移除速度数据
     */
    @Synchronized
    fun stopTracking(taskId: Int) {
        samplesByTask.remove(taskId)
        publish()
    }

    private fun publish() {
        val now = System.nanoTime()
        _speedByGroup.value = samplesByTask.values.groupBy { it.groupId }.mapValues { (_, tasks) ->
            tasks.sumOf { it.totalBytes / ((now - it.startedAtNanos).coerceAtLeast(1) / 1_000_000_000.0) }.toFloat()
        }
    }

}
