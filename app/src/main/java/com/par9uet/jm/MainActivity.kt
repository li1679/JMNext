package com.par9uet.jm

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.par9uet.jm.ui.theme.AppTheme

/** 记录上次解析出的深浅色，供下次冷启动挑选启动窗口背景 */
const val LAUNCH_THEME_PREFS = "launch-theme"
const val LAUNCH_THEME_KEY_DARK = "dark"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        applyLaunchTheme()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            AppTheme {
                App()
            }
        }
    }

    /**
     * 在 super.onCreate() 之前按应用主题选择启动窗口背景。
     *
     * 启动窗口背景由系统在 Compose 画出第一帧前显示。若只靠 values-night，
     * 跟的是系统深浅色，而应用主题是用户选的 light / dark / auto——
     * 两者不一致时冷启动会闪白。
     *
     * 主题设置存在加密 SharedPreferences 里，此刻解密取值代价过大，
     * 因此由 AppTheme 解析出深浅色后额外写一份明文标记供这里读取。
     */
    private fun applyLaunchTheme() {
        val prefs = getSharedPreferences(LAUNCH_THEME_PREFS, Context.MODE_PRIVATE)
        val dark = if (prefs.contains(LAUNCH_THEME_KEY_DARK)) {
            prefs.getBoolean(LAUNCH_THEME_KEY_DARK, false)
        } else {
            // 还没记录过（首次安装）时跟随系统
            val nightMask = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
            nightMask == Configuration.UI_MODE_NIGHT_YES
        }
        setTheme(if (dark) R.style.Theme_Jmmobile_Dark else R.style.Theme_Jmmobile_Light)
    }
}
