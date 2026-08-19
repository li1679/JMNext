package com.par9uet.jm.core.model

data class Comic(
    val id: Int,
    val name: String,
    val authorList: List<String> = listOf(),
    val description: String,
    val readCount: Int,
    val likeCount: Int,
    val commentCount: Int,
    val tagList: List<String>,
    val roleList: List<String>,
    val workList: List<String>,
    val isLike: Boolean = false,
    val isCollect: Boolean = false,
    val relateComicList: List<Comic> = listOf(),
    val comicChapterList: List<ComicChapter> = listOf(),
    val seriesId: String = "",
    val price: Int,
    val isBuy: Boolean = false,
) {
    companion object {
        fun create(
            id: Int,
            name: String,
            authorList: List<String>,
        ): Comic {
            return Comic(
                id = id,
                name = name,
                authorList = authorList,
                description = "",
                readCount = 0,
                likeCount = 0,
                commentCount = 0,
                tagList = listOf(),
                roleList = listOf(),
                workList = listOf(),
                isLike = false,
                isCollect = false,
                relateComicList = listOf(),
                comicChapterList = listOf(),
                seriesId = "",
                price = 0,
                isBuy = false,
            )
        }
    }
}
