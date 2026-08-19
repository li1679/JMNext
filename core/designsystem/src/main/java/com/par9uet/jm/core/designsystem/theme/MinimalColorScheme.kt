package com.par9uet.jm.core.designsystem.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

/**
 * 极简白：手写的中性色板。
 *
 * 不走种子色推导。material-kolor 的 TonalSpot 会往中性色板注入彩度，
 * background / surface / surfaceContainer 全都会带一层色调，看上去是
 * 「染了色的白」而不是纯白。要精确落到指定灰阶只能直接给值。
 *
 * 两条约定：
 * - 强调色用深灰而非彩色，颜色只在按钮、选中态这类必要处承担功能；
 * - error 保留红色。删除、取消收藏这类破坏性操作需要靠颜色警示，
 *   全灰会让人误点。
 *
 * surfaceTint 特意设成与 surface 同色：M3 的 tonalElevation 靠 surfaceTint
 * 染色来表达层级，若保留默认值，任何抬升的容器都会重新染上主色调。
 * 层级改由留白和描边表达。
 */
val MinimalLightColorScheme: ColorScheme = lightColorScheme(
    primary = Color(0xFF2B2B2B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFF0F0F0),
    onPrimaryContainer = Color(0xFF1A1A1A),
    inversePrimary = Color(0xFFD4D4D4),

    secondary = Color(0xFF3D3D3D),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFF0F0F0),
    onSecondaryContainer = Color(0xFF1A1A1A),

    tertiary = Color(0xFF4F4F4F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF0F0F0),
    onTertiaryContainer = Color(0xFF1A1A1A),

    background = Color(0xFFFFFFFF),
    onBackground = Color(0xFF1A1A1A),

    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1A1A1A),
    surfaceVariant = Color(0xFFF5F5F5),
    onSurfaceVariant = Color(0xFF6B6B6B),
    surfaceTint = Color(0xFFFFFFFF),

    surfaceDim = Color(0xFFEDEDED),
    surfaceBright = Color(0xFFFFFFFF),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFAFAFA),
    surfaceContainer = Color(0xFFF5F5F5),
    surfaceContainerHigh = Color(0xFFF0F0F0),
    surfaceContainerHighest = Color(0xFFEBEBEB),

    outline = Color(0xFFD4D4D4),
    outlineVariant = Color(0xFFEEEEEE),

    inverseSurface = Color(0xFF2B2B2B),
    inverseOnSurface = Color(0xFFF5F5F5),

    error = Color(0xFFB3261E),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF9DEDC),
    onErrorContainer = Color(0xFF410E0B),

    scrim = Color(0xFF000000),
)

/** 极简黑：浅色方案的深色对应，同样保持无彩。 */
val MinimalDarkColorScheme: ColorScheme = darkColorScheme(
    primary = Color(0xFFF0F0F0),
    onPrimary = Color(0xFF1A1A1A),
    primaryContainer = Color(0xFF2A2A2A),
    onPrimaryContainer = Color(0xFFF0F0F0),
    inversePrimary = Color(0xFF2B2B2B),

    secondary = Color(0xFFDCDCDC),
    onSecondary = Color(0xFF1A1A1A),
    secondaryContainer = Color(0xFF2A2A2A),
    onSecondaryContainer = Color(0xFFF0F0F0),

    tertiary = Color(0xFFC8C8C8),
    onTertiary = Color(0xFF1A1A1A),
    tertiaryContainer = Color(0xFF2A2A2A),
    onTertiaryContainer = Color(0xFFF0F0F0),

    background = Color(0xFF0E0E0E),
    onBackground = Color(0xFFF0F0F0),

    surface = Color(0xFF0E0E0E),
    onSurface = Color(0xFFF0F0F0),
    surfaceVariant = Color(0xFF1A1A1A),
    onSurfaceVariant = Color(0xFFA0A0A0),
    surfaceTint = Color(0xFF0E0E0E),

    surfaceDim = Color(0xFF0E0E0E),
    surfaceBright = Color(0xFF353535),
    surfaceContainerLowest = Color(0xFF090909),
    surfaceContainerLow = Color(0xFF141414),
    surfaceContainer = Color(0xFF1A1A1A),
    surfaceContainerHigh = Color(0xFF242424),
    surfaceContainerHighest = Color(0xFF2E2E2E),

    outline = Color(0xFF3A3A3A),
    outlineVariant = Color(0xFF2A2A2A),

    inverseSurface = Color(0xFFF0F0F0),
    inverseOnSurface = Color(0xFF1A1A1A),

    error = Color(0xFFF2B8B5),
    onError = Color(0xFF601410),
    errorContainer = Color(0xFF8C1D18),
    onErrorContainer = Color(0xFFF9DEDC),

    scrim = Color(0xFF000000),
)
