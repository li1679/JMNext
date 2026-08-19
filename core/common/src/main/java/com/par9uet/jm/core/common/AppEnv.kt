package com.par9uet.jm.core.common

/**
 * 应用级运行环境标志。
 *
 * `BuildConfig` 由各 Gradle 模块各自生成，library 模块里拿到的 `DEBUG` 反映的是该 library
 * 自身的构建类型，并不等同于宿主应用的构建类型。因此这里由 `:app` 在 Application 启动时
 * 用真正的 `BuildConfig.DEBUG` 注入一次，下层模块统一读这个值来决定是否输出日志。
 */
object AppEnv {
    @Volatile
    var isDebug: Boolean = false
        private set

    /** 仅供 `:app` 在 `Application.onCreate` 中调用一次。 */
    fun init(isDebug: Boolean) {
        this.isDebug = isDebug
    }
}
