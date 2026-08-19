package com.par9uet.jm.core.common

/** 运行环境标志。library 模块的 BuildConfig.DEBUG 反映的是自身构建类型，故由 :app 注入。 */
object AppEnv {
    @Volatile
    var isDebug: Boolean = false
        private set

    fun init(isDebug: Boolean) {
        this.isDebug = isDebug
    }
}
