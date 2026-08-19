package com.par9uet.jm.core.common

/**
 * 启动主题的本地缓存。
 *
 * 应用设置存在 DataStore/SharedPreferences 里，读取是异步的；而 Activity 在
 * `setContent` 之前就需要知道该用日间还是夜间背景，否则冷启动会闪一下白屏。
 * 这里用一份极小的同步 SharedPreferences 记录上次的主题，供启动瞬间直接读取。
 */
const val LAUNCH_THEME_PREFS = "launch-theme"
const val LAUNCH_THEME_KEY_DARK = "dark"
