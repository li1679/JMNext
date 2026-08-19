package com.par9uet.jm.core.common

import android.graphics.Bitmap
import android.os.Build
import java.io.OutputStream

// WebP 重编码质量。原图已是有损，再压一次属二次有损，过低会糊掉网点与文字。

/** 阅读解码缓存：无损。解扰只重排条带不改像素，有损重编码是纯损失，且下载会再编一次叠加成两代。 */
const val WEBP_QUALITY_CACHE = 100

/** 下载留存：用户长期保存的成品，优先保画质 */
const val WEBP_QUALITY_DOWNLOAD = 92

/** 封面缩略图：显示尺寸小，压狠一点无所谓 */
const val WEBP_QUALITY_COVER = 75

fun Bitmap.compressWebpCompat(quality: Int, stream: OutputStream): Boolean {
    val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        if (quality >= 100) Bitmap.CompressFormat.WEBP_LOSSLESS else Bitmap.CompressFormat.WEBP_LOSSY
    } else {
        @Suppress("DEPRECATION")
        Bitmap.CompressFormat.WEBP
    }
    return compress(format, quality, stream)
}
