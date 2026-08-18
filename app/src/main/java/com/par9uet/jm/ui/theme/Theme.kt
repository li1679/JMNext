package com.par9uet.jm.ui.theme

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
 * 三类标签（内容 / 角色 / 作品）的配色，四个字段成套取自同一个 role。
 * 不要把 onColor 单独写死：自定义取色时会与 color 撞色。
 */
fun extendedColorSchemeFor(colorScheme: ColorScheme): ExtendedColorScheme = ExtendedColorScheme(
    contentTag = ColorFamily(
        color = colorScheme.primary,
        onColor = colorScheme.onPrimary,
        colorContainer = colorScheme.primaryContainer,
        onColorContainer = colorScheme.onPrimaryContainer,
    ),
    roleTag = ColorFamily(
        color = colorScheme.tertiary,
        onColor = colorScheme.onTertiary,
        colorContainer = colorScheme.tertiaryContainer,
        onColorContainer = colorScheme.onTertiaryContainer,
    ),
    workTag = ColorFamily(
        color = colorScheme.secondary,
        onColor = colorScheme.onSecondary,
        colorContainer = colorScheme.secondaryContainer,
        onColorContainer = colorScheme.onSecondaryContainer,
    ),
)
