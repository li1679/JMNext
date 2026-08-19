package com.par9uet.jm.core.model

enum class LauncherDisguise(
    val id: String,
    val label: String,
    val aliasClassName: String,
) {
    Default("default", "JMNext", ".DefaultLauncherAlias"),
    SystemTools("system_tools", "系统工具", ".SystemToolsLauncherAlias"),
    Gallery("gallery", "相册", ".GalleryLauncherAlias");

    companion object {
        fun fromId(id: String?): LauncherDisguise {
            return entries.firstOrNull { it.id == id } ?: Default
        }
    }
}
