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
 * 三类标签（内容 / 角色 / 作品）的配色。
 *
 * 原先分别取 primary / tertiary / secondary，详情页上会同时出现三种彩色 chip，
 * 是整个界面里最抢眼的一处。现在统一成中性描边样式：底色用最浅的容器色、
 * 文字用次要文字色，三类只在语义上区分，不再靠颜色区分。
 *
 * 保留三个字段而非合并成一个，是为了不改动调用方；将来若要恢复彩色区分，
 * 只需改这里。
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
