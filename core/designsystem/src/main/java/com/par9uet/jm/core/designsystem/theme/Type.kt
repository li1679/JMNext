package com.par9uet.jm.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp

/** 等宽数字字形 */
private const val TABULAR_FIGURES = "tnum"

private fun TextStyle.tabular(): TextStyle = copy(fontFeatureSettings = TABULAR_FIGURES)

/**
 * 字体阶梯。相对 M3 默认放宽正文行高（中文方块字无升降部，多行会显挤），
 * 数字用等宽字形（章节号/页码/计数需在列表里对齐）。字重由各组件按信息层级自定。
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
