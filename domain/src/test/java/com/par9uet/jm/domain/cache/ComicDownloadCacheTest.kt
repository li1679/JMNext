package com.par9uet.jm.domain.cache

import com.par9uet.jm.data.database.model.DownloadComic
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * 锁定章节目录命名的唯一性。
 *
 * 章节目录一旦发生碰撞，多个章节会写进同一个物理目录，
 * 而页面文件名是 0.webp/1.webp…，后下载的章节会覆盖前一章、
 * 页数多出来的部分残留上一章的图，表现为「几章内容掺杂在一起」，
 * 且已经写坏的下载只能删掉重来，代价很高。
 */
class ComicDownloadCacheTest {

    private fun chapter(id: Int, chapterName: String, name: String = "某本子") = DownloadComic(
        id = id,
        name = name,
        authorList = emptyList(),
        coverPath = "",
        zipPath = "",
        progress = 0f,
        status = "pending",
        createTime = 0L,
        chapterName = chapterName,
    )

    @Test
    fun 章节名为空的多个章节不会落到同一目录() {
        // 章节名为空时旧实现一律回退成「单篇」，同一本书的每一章都会撞在一起
        val a = getChapterCacheName(chapter(id = 111, chapterName = ""))
        val b = getChapterCacheName(chapter(id = 222, chapterName = ""))

        assertNotEquals(a, b)
    }

    @Test
    fun 章节名重复的多个章节不会落到同一目录() {
        val a = getChapterCacheName(chapter(id = 111, chapterName = "第一话"))
        val b = getChapterCacheName(chapter(id = 222, chapterName = "第一话"))

        assertNotEquals(a, b)
    }

    @Test
    fun 同一章节的目录名保持稳定() {
        // 名称稳定才能实现断点续传：重试时要能找回上次下载了一半的目录
        val first = getChapterCacheName(chapter(id = 333, chapterName = "第二话"))
        val second = getChapterCacheName(chapter(id = 333, chapterName = "第二话"))

        assertEquals(first, second)
    }

    @Test
    fun 目录名保留章节名便于人工辨认() {
        val name = getChapterCacheName(chapter(id = 444, chapterName = "第三话"))

        assertTrue("目录名应包含章节名: $name", name.contains("第三话"))
        assertTrue("目录名应包含章节 id: $name", name.contains("444"))
    }

    @Test
    fun 章节名中的非法路径字符被替换() {
        val name = getChapterCacheName(chapter(id = 555, chapterName = "第一话/上"))

        assertTrue("不应残留路径分隔符: $name", !name.contains("/"))
        assertTrue("不应残留路径分隔符: $name", !name.contains("\\"))
    }

    @Test
    fun 章节名全为非法字符时仍能生成唯一目录() {
        val a = getChapterCacheName(chapter(id = 666, chapterName = "///"))
        val b = getChapterCacheName(chapter(id = 777, chapterName = "///"))

        assertNotEquals(a, b)
        assertTrue("目录名不应为空", a.isNotBlank())
    }
}
