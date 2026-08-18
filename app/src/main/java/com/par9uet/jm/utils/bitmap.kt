package com.par9uet.jm.utils

import android.graphics.Bitmap
import android.os.Build
import java.io.OutputStream

/**
 * 图片重新编码时的 WebP 质量。
 * 服务端下发的原图已经是有损 WebP，本地再压一次属于二次有损，
 * 质量给太低会明显糊掉漫画的网点与文字。
 */
/**
 * 阅读解码缓存：无损。
 *
 * 解扰只是把横向条带重新排序，不改变像素值，有损重编码是纯粹的画质损失；
 * 下载流程还会读这份缓存再以 [WEBP_QUALITY_DOWNLOAD] 编一次，会叠加成两代有损。
 */
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
