package com.par9uet.jm.core.common

/**
 * 启动窗口背景用的主题标记。
 * 设置存在加密 prefs 里读取是异步的，而 Activity 在 setContent 前就要决定深浅色，
 * 否则冷启动会闪屏，故用一份明文 prefs 同步读取。
 */
const val LAUNCH_THEME_PREFS = "launch-theme"
const val LAUNCH_THEME_KEY_DARK = "dark"
