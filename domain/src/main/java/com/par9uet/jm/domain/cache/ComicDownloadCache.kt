package com.par9uet.jm.domain.cache

import android.content.Context
import com.google.gson.Gson
import com.par9uet.jm.data.database.model.DownloadComic
import com.par9uet.jm.core.common.tryCreateDir
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

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
    return tryCreateDir(File(getDownloadDir(context), getComicRootCacheName(comic)))
}

fun getComicRootCacheName(comic: DownloadComic): String =
    "${comic.groupId.takeIf { it != 0 } ?: comic.id}_${safeCacheFileName(comic.groupName.ifBlank { comic.name })}"

fun comicChapterDownloadCandidates(context: Context, comic: DownloadComic): List<File> =
    listOfNotNull(
        comic.zipPath.takeIf { it.isNotBlank() }?.let(::File),
        File(File(getDownloadDir(context), getComicRootCacheName(comic)), getChapterCacheName(comic)),
        File(File(getDownloadDir(context), safeCacheFileName(comic.groupName.ifBlank { comic.name })), getChapterCacheName(comic)),
    ).distinctBy { it.absolutePath }

fun resolveComicChapterDownloadDir(context: Context, comic: DownloadComic): File? =
    comicChapterDownloadCandidates(context, comic)
        .firstOrNull { it.isDirectory && listComicImageFiles(it).isNotEmpty() }

fun getComicChapterDownloadDir(context: Context, comic: DownloadComic): File {
    return tryCreateDir(File(getComicDownloadRootDir(context, comic), getChapterCacheName(comic)))
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
    return "${safeCacheFileName(comic.chapterName.ifBlank { "单篇" })}_${comic.id}"
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
        coverPath = comic.coverPath.takeIf { it.isNotBlank() && File(it).isFile }
            ?: getComicCoverDownloadFile(context, comic).absolutePath,
        chapters = chapterConfigs,
    )
    val target = getComicConfigFile(context, comic)
    val temp = File.createTempFile("config-", ".tmp", rootDir)
    try {
        temp.writeText(gson.toJson(config), Charsets.UTF_8)
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
    } finally {
        temp.delete()
    }
}

fun safeCacheFileName(name: String): String {
    val cleaned = name
        .replace(Regex("""[\\/:*?"<>|]"""), "_")
        .replace(Regex("""\s+"""), " ")
        .trim()
        .trimEnd('.')
    return cleaned.ifBlank { "未命名漫画" }
}
