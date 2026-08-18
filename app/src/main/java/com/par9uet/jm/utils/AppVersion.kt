package com.par9uet.jm.utils

import android.content.Context

/** 应用版本号与图标读取，关于页与更新检查页共用 */
@Suppress("DEPRECATION")
internal fun appVersionName(context: Context): String {
    return runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName
    }.getOrNull().orEmpty().ifBlank { "unknown" }
}

@Suppress("DEPRECATION")
internal fun appVersionCode(context: Context): String {
    return runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionCode.toString()
    }.getOrNull().orEmpty().ifBlank { "unknown" }
}

@Suppress("DEPRECATION")
internal fun loadAppIconBitmap(context: Context) = runCatching {
    val drawable = context.packageManager.getApplicationIcon(context.packageName)
    val width = drawable.intrinsicWidth.takeIf { it > 0 } ?: 128
    val height = drawable.intrinsicHeight.takeIf { it > 0 } ?: 128
    val bitmap = android.graphics.Bitmap.createBitmap(
        width,
        height,
        android.graphics.Bitmap.Config.ARGB_8888
    )
    val canvas = android.graphics.Canvas(bitmap)
    drawable.setBounds(0, 0, canvas.width, canvas.height)
    drawable.draw(canvas)
    bitmap
}.getOrNull()
