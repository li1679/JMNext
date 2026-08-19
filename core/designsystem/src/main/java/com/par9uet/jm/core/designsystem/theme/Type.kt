package com.par9uet.jm.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/** 等宽数字字形 */
private const val TABULAR_FIGURES = "tnum"

private fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = TABULAR_FIGURES)

/**
 * 应用字体阶梯。
 *
 * 相对 M3 默认值做了两处调整：
 * - 放宽正文行高。M3 默认按拉丁文 x-height 调校，中文方块字无升降部，多行文本会显挤。
 * - 数字用等宽字形。章节号 / 页码 / 计数在列表里需要对齐，比例字形会左右跳动。
 *
 * 字重不在此统一设置，由各组件按自身信息层级决定。
 */
val AppTypography: Typography = Typography().run {
    copy(
        displayLarge = displayLarge.tabular(),
        displayMedium = displayMedium.tabular(),
        displaySmall = displaySmall.tabular(),

        titleLarge = titleLarge.copy(lineHeight = 30.sp),
        titleMedium = titleMedium.copy(lineHeight = 24.sp),

        bodyLarge = bodyLarge.copy(lineHeight = 26.sp),
        bodyMedium = bodyMedium.copy(lineHeight = 22.sp),
        bodySmall = bodySmall.copy(lineHeight = 18.sp),

        labelLarge = labelLarge.tabular(),
        labelMedium = labelMedium.tabular(),
        labelSmall = labelSmall.tabular(),
    )
}
