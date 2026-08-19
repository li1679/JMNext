package com.par9uet.jm

import android.content.Context
import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.par9uet.jm.core.common.LAUNCH_THEME_KEY_DARK
import com.par9uet.jm.core.common.LAUNCH_THEME_PREFS
import com.par9uet.jm.core.designsystem.theme.AppTheme

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
     * 在 super.onCreate() 前按应用主题选定启动窗口背景。
     * 只靠 values-night 跟的是系统深浅色，而应用主题是用户选的 light/dark/auto，
     * 两者不一致时冷启动会闪白。主题存在加密 prefs 里，此刻解密代价过大，
     * 故由 AppTheme 另写一份明文标记供这里读取。
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
