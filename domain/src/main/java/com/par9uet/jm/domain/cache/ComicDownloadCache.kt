package com.par9uet.jm.domain.cache

import android.content.Context
import com.google.gson.Gson
import com.par9uet.jm.data.database.model.DownloadComic
import com.par9uet.jm.core.common.tryCreateDir
import java.io.File

private const val CONFIG_FILE_NAME = "config.json"
private const val COVER_FILE_NAME = "cover.webp"

data class DownloadComicCacheConfig(
    val id: Int,
    val title: String,
    val authors: List<String>,
    val tags: List<String>,
    val cachePath: String,
    val coverPath: String,
    val chapters: List<DownloadComicCacheChapter>,
)

data class DownloadComicCacheChapter(
    val id: Int,
    val name: String,
    val path: String,
    val status: String,
    val imageCount: Int,
)

fun getComicDownloadRootDir(context: Context, comic: DownloadComic): File {
    return getComicDownloadRootDir(context, comic.groupName.ifBlank { comic.name })
}

fun getComicDownloadRootDir(context: Context, comicName: String): File {
    return tryCreateDir(File(getDownloadDir(context), safeCacheFileName(comicName)))
}

fun getComicChapterDownloadDir(context: Context, comic: DownloadComic): File {
    return tryCreateDir(File(getComicDownloadRootDir(context, comic), getChapterCacheName(comic)))
}

/**
 * 旧版本按「纯章节名」命名的章节目录。
 * 仅用于读取历史下载内容，新的下载一律走 [getChapterCacheName]。
 */
fun getLegacyComicChapterDownloadDir(context: Context, comic: DownloadComic): File {
    return File(getComicDownloadRootDir(context, comic), getLegacyChapterCacheName(comic))
}

fun getComicCoverDownloadFile(context: Context, comic: DownloadComic): File {
    return File(getComicDownloadRootDir(context, comic), COVER_FILE_NAME)
}

fun getComicConfigFile(context: Context, comic: DownloadComic): File {
    return File(getComicDownloadRootDir(context, comic), CONFIG_FILE_NAME)
}

/**
 * 章节目录名，必须带章节 id。
 * 章节名可能为空或在同一本书内重复，仅用名字会让多章落到同一目录，
 * 而页面文件名是 0.webp/1.webp…，后写入的章节会覆盖前一章，表现为几章内容掺杂。
 */
fun getChapterCacheName(comic: DownloadComic): String {
    return "${getLegacyChapterCacheName(comic)}_${comic.id}"
}

/** 旧版命名规则，保留用于读取历史下载内容 */
private fun getLegacyChapterCacheName(comic: DownloadComic): String {
    return safeCacheFileName(comic.chapterName.ifBlank { "单篇" })
}

fun listComicImageFiles(dir: File): List<File> {
    return dir.listFiles()
        ?.filter { it.isFile && it.extension.lowercase() in setOf("webp", "jpg", "jpeg", "png") }
        ?.sortedWith(compareBy<File> { it.nameWithoutExtension.toIntOrNull() ?: Int.MAX_VALUE }.thenBy { it.name })
        .orEmpty()
}

fun writeComicCacheConfig(
    context: Context,
    comic: DownloadComic,
    chapters: List<DownloadComic>,
    gson: Gson = Gson()
) {
    val rootDir = getComicDownloadRootDir(context, comic)
    val chapterConfigs = chapters.sortedBy { it.createTime }.map { chapter ->
        val chapterDir = chapter.zipPath.takeIf { it.isNotBlank() }?.let(::File)?.takeIf { it.isDirectory }
            ?: File(rootDir, getChapterCacheName(chapter))
        DownloadComicCacheChapter(
            id = chapter.id,
            name = chapter.chapterName.ifBlank { if (chapters.size > 1) chapter.name else "单篇" },
            path = chapterDir.absolutePath,
            status = chapter.status,
            imageCount = listComicImageFiles(chapterDir).size,
        )
    }
    val config = DownloadComicCacheConfig(
        id = comic.groupId.takeIf { it != 0 } ?: comic.id,
        title = comic.groupName.ifBlank { comic.name },
        authors = comic.authorList,
        tags = comic.tagList,
        cachePath = rootDir.absolutePath,
        coverPath = getComicCoverDownloadFile(context, comic).absolutePath,
        chapters = chapterConfigs,
    )
    getComicConfigFile(context, comic).writeText(gson.toJson(config), Charsets.UTF_8)
}

fun safeCacheFileName(name: String): String {
    val cleaned = name
        .replace(Regex("""[\\/:*?"<>|]"""), "_")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .trimEnd('.')
    return cleaned.ifBlank { "未命名漫画" }
}
