package com.par9uet.jm.core.common

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import com.par9uet.jm.core.model.LauncherDisguise

class LauncherDisguiseApplier(
    private val context: Context,
) {
    fun apply(disguise: LauncherDisguise) {
        val packageManager = context.packageManager
        // applicationId 可与源码 namespace 不同，别用 context.packageName 拼 alias；
        // Class.getPackageName() 又只在 API 31+ 可用，因此从应用类全名截取源码包名。
        val componentClassPrefix = context.applicationContext::class.java.name.substringBeforeLast('.')
        LauncherDisguise.entries.forEach { item ->
            runCatching {
                packageManager.setComponentEnabledSetting(
                    ComponentName(context.packageName, "$componentClassPrefix${item.aliasClassName}"),
                    if (item == disguise) {
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                    } else {
                        PackageManager.COMPONENT_ENABLED_STATE_DISABLED
                    },
                    PackageManager.DONT_KILL_APP
                )
            }.onFailure {
                log("切换桌面图标入口失败：${item.id}，原因：${it.message}")
            }
        }
    }
}
