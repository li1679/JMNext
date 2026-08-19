package com.par9uet.jm.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

@Immutable
data class ColorFamily(
    val color: Color,
    val onColor: Color,
    val colorContainer: Color,
    val onColorContainer: Color
)

@Immutable
data class ExtendedColorScheme(
    val contentTag: ColorFamily,
    val roleTag: ColorFamily,
    val workTag: ColorFamily,
)

/**
 * 三类标签（内容/角色/作品）统一为中性描边，不再用颜色区分。
 * 保留三个字段是为了不改动调用方。
 */
fun extendedColorSchemeFor(colorScheme: ColorScheme): ExtendedColorScheme {
    val neutral = ColorFamily(
        color = colorScheme.onSurfaceVariant,
        onColor = colorScheme.surface,
        colorContainer = colorScheme.surfaceContainerLow,
        onColorContainer = colorScheme.onSurfaceVariant,
    )
    return ExtendedColorScheme(
        contentTag = neutral,
        roleTag = neutral,
        workTag = neutral,
    )
}
