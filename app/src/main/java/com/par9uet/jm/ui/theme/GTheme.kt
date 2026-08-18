package com.par9uet.jm.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.materialkolor.PaletteStyle
import com.materialkolor.rememberDynamicColorScheme
import com.par9uet.jm.LAUNCH_THEME_KEY_DARK
import com.par9uet.jm.LAUNCH_THEME_PREFS
import com.par9uet.jm.data.models.COLOR_PALETTE_PRESET_DEFAULT
import com.par9uet.jm.data.models.COLOR_PALETTE_PRESET_FOREST
import com.par9uet.jm.data.models.COLOR_PALETTE_PRESET_LAVENDER
import com.par9uet.jm.data.models.COLOR_PALETTE_PRESET_MONET
import com.par9uet.jm.data.models.COLOR_PALETTE_PRESET_OCEAN
import com.par9uet.jm.data.models.COLOR_PALETTE_PRESET_SUNSET
import com.par9uet.jm.data.models.LocalSetting
import com.par9uet.jm.store.LocalSettingManager
import org.koin.compose.getKoin

val LocalExtendedColors = staticCompositionLocalOf<ExtendedColorScheme> {
    error("No extended color scheme provided")
}

object ExtendedTheme {
    val colors: ExtendedColorScheme
        @Composable
        get() = LocalExtendedColors.current
}

/**
 * 各配色预设的种子色。
 *
 * 一个种子即可：整套 ColorScheme（含 surface / background / outline / scrim 等
 * 全部 role）都由它经 HCT 色调板推导，不要退回手工逐个覆盖强调色——
 * 那样 surface 系列不会跟着变，各预设的整体观感会趋同。
 */
private val PRESET_SEEDS: Map<String, Color> = mapOf(
    COLOR_PALETTE_PRESET_DEFAULT to Color(0xFF4F5F7F),
    COLOR_PALETTE_PRESET_OCEAN to Color(0xFF00696D),
    COLOR_PALETTE_PRESET_SUNSET to Color(0xFF8C5000),
    COLOR_PALETTE_PRESET_FOREST to Color(0xFF2E6B3E),
    COLOR_PALETTE_PRESET_LAVENDER to Color(0xFF6750A4),
)

private val FALLBACK_SEED = PRESET_SEEDS.getValue(COLOR_PALETTE_PRESET_DEFAULT)

@Composable
fun AppTheme(
    localSettingManager: LocalSettingManager = getKoin().get(),
    content: @Composable () -> Unit
) {
    val setting by localSettingManager.localSettingState.collectAsState()
    val context = LocalContext.current
    val isDark = when (setting.theme) {
        "auto" -> isSystemInDarkTheme()
        "dark" -> true
        else -> false
    }
    // 仅当用户选择「莫奈取色」预设时才用系统动态色，其余一律走种子色推导
    val supportDynamic = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val useMonet = setting.colorPalettePreset == COLOR_PALETTE_PRESET_MONET && supportDynamic

    val colorScheme: ColorScheme = when {
        useMonet && isDark -> dynamicDarkColorScheme(context)
        useMonet -> dynamicLightColorScheme(context)
        else -> rememberDynamicColorScheme(
            seedColor = setting.seedColor(),
            isDark = isDark,
            isAmoled = false,
            // 用户未单独指定的 role 传 null，由种子和谐推导
            secondary = setting.customColorSecondary?.toColorOrNull(),
            tertiary = setting.customColorTertiary?.toColorOrNull(),
            error = setting.customColorError?.toColorOrNull(),
            style = PaletteStyle.TonalSpot,
        )
    }
    val extendedColorScheme = extendedColorSchemeFor(colorScheme)

    // 明文标记供 MainActivity 在 onCreate 阶段挑选启动窗口背景，
    // 那里无法承受解密加密 prefs 的代价
    LaunchedEffect(isDark) {
        runCatching {
            context.getSharedPreferences(LAUNCH_THEME_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(LAUNCH_THEME_KEY_DARK, isDark)
                .apply()
        }
    }

    // 切换深浅色时同步刷新系统栏图标颜色
    val view = LocalView.current
    if (!view.isInEditMode) {
        DisposableEffect(isDark) {
            // 必须走 findActivity()：view.context 未必是 Activity
            //（对话框窗口、ComposeView 容器等），直接转型会 ClassCastException
            val window = view.context.findActivity()?.window
            if (window != null) {
                val controller = WindowCompat.getInsetsController(window, view)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }
            onDispose {}
        }
    }

    CompositionLocalProvider(LocalExtendedColors provides extendedColorScheme) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AppTypography,
            shapes = AppShapes,
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = colorScheme.background,
                content = content
            )
        }
    }
}

/** 当前配色的种子色：用户自定义主色优先，否则取预设种子 */
private fun LocalSetting.seedColor(): Color =
    customColorPrimary?.toColorOrNull()
        ?: PRESET_SEEDS[colorPalettePreset]
        ?: FALLBACK_SEED

private fun String.toColorOrNull(): Color? = runCatching {
    val hex = removePrefix("#")
    val value = if (hex.length == 6) "FF$hex".toLong(16) else hex.toLong(16)
    Color(value.toInt())
}.getOrNull()

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
