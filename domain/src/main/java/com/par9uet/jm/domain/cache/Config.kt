package com.par9uet.jm.domain.cache

import android.content.Context
import com.par9uet.jm.core.common.tryCreateDir
import java.io.File

private const val DOWNLOAD_DIR_NAME = "download"

fun getCommonCacheDir(context: Context) = tryCreateDir(File(context.cacheDir, "common"))

fun getCommonPicDecodeCacheDir(context: Context, comicId: Int) =
    tryCreateDir(File(context.cacheDir, "pic_decode/$comicId"))

/**
 * 离线下载根目录。不可用 cacheDir：系统存储紧张或用户「清除缓存」时会被清空，
 * 而下载内容期望长期保留。优先外部应用专属目录（免权限、卸载自动清理），不可用时回落 filesDir。
 */
fun getDownloadDir(context: Context): File {
    val external = context.getExternalFilesDir(DOWNLOAD_DIR_NAME)
    if (external != null) return tryCreateDir(external)
    return tryCreateDir(File(context.filesDir, DOWNLOAD_DIR_NAME))
}

/** 解码缓存的容量上限 */
private const val PIC_DECODE_CACHE_MAX_BYTES = 512L * 1024 * 1024

/** 按最后访问时间裁剪解码缓存到上限内。内容可随时重新生成，删除安全。 */
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
