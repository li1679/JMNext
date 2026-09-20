package com.par9uet.jm.data.network.model

import com.par9uet.jm.core.model.Comic
import com.par9uet.jm.core.model.HomeComicSwiperItem

class HomeSwiperComicListItemResponse(
    val id: String,
    val title: String,
    val slug: String,
    val type: String,
    val filter_val: String,
    val content: List<ListItem>,
    val errorMessage: String? = null,
) {
    data class ListItem(
        val id: String,
        val author: String,
        val description: String?,
        val name: String,
        val image: String,
        val category: Category,
        val category_sub: Category,
        val liked: Boolean,
        val is_favorite: Boolean,
        val update_at: Int,
        val tags: List<String> = emptyList(),
    ) {
        data class Category(
            val id: String?,
            val title: String?
        )
    }

    fun toHomeComicSwiperItem(): HomeComicSwiperItem {
        return HomeComicSwiperItem(
            id = id,
            title = title,
            errorMessage = errorMessage,
            list = content.map {
                Comic(
                    id = it.id.toInt(),
                    name = it.name,
                    authorList = listOf(it.author),
                    description = it.description ?: "",
                    readCount = 0,
                    likeCount = 0,
                    commentCount = 0,
                    tagList = it.tags,
                    roleList = listOf(),
                    workList = listOf(),
                    isLike = false,
                    isCollect = false,
                    comicChapterList = listOf(),
                    price = 0,
                    isBuy = false,
                )
            }
        )
    }
}
