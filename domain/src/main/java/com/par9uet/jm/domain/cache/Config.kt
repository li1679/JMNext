package com.par9uet.jm.domain.cache

import android.content.Context
import com.par9uet.jm.core.common.tryCreateDir
import java.io.File

private const val DOWNLOAD_DIR_NAME = "download"

fun getCommonCacheDir(context: Context) = tryCreateDir(File(context.cacheDir, "common"))

fun getCommonPicDecodeCacheDir(context: Context, comicId: Int) =
    tryCreateDir(File(context.cacheDir, "pic_decode/$comicId"))

/**
 * 离线下载的漫画根目录。
 *
 * 不能放在 cacheDir：系统在存储紧张时会清空该目录，用户在系统设置里
 * 「清除缓存」也会一并删掉，而离线下载是期望长期保留的内容。
 *
 * 优先使用外部应用专属目录：用户可用文件管理器查看导出，卸载时系统自动清理，
 * 且不需要存储权限。外部存储不可用时回落到内部 filesDir。
 */
fun getDownloadDir(context: Context): File {
    val external = context.getExternalFilesDir(DOWNLOAD_DIR_NAME)
    if (external != null) return tryCreateDir(external)
    return tryCreateDir(File(context.filesDir, DOWNLOAD_DIR_NAME))
}

/** 解码缓存的容量上限 */
private const val PIC_DECODE_CACHE_MAX_BYTES = 512L * 1024 * 1024

/**
 * 把解码缓存裁剪到 [PIC_DECODE_CACHE_MAX_BYTES] 以内，按最后访问时间淘汰。
 * 内容可随时重新解码生成，删除是安全的。
 */
fun trimPicDecodeCache(context: Context) {
    runCatching {
        val root = File(context.cacheDir, "pic_decode")
        if (!root.isDirectory) return
        val files = root.walkBottomUp().filter { it.isFile }.toList()
        var total = files.sumOf { it.length() }
        if (total <= PIC_DECODE_CACHE_MAX_BYTES) return
        // 最久未访问的先删
        files.sortedBy { it.lastModified() }.forEach { file ->
            if (total <= PIC_DECODE_CACHE_MAX_BYTES) return@forEach
            val size = file.length()
            if (file.delete()) total -= size
        }
        // 清掉因此空掉的漫画子目录
        root.listFiles()?.forEach { dir ->
            if (dir.isDirectory && dir.listFiles()?.isEmpty() == true) dir.delete()
        }
    }
}
