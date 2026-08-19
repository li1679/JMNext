package com.par9uet.jm.core.common

import kotlin.math.roundToInt

/**
 * 字节数格式化。
 *
 * 原先在 PDF 导出与应用更新下载两处各有一份实现，输出格式还略有出入
 * （同一个大小在两个页面上显示不一致）。统一收敛到这里。
 */
fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 B"
    val units = listOf("B", "KB", "MB", "GB")
    var value = bytes.toDouble()
    var unitIndex = 0
    while (value >= 1024.0 && unitIndex < units.lastIndex) {
        value /= 1024.0
        unitIndex++
    }
    return if (unitIndex == 0) {
        "${value.roundToInt()} ${units[unitIndex]}"
    } else {
        String.format("%.1f %s", value, units[unitIndex])
    }
}
