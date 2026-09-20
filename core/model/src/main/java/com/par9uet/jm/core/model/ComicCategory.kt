package com.par9uet.jm.core.model

data class ComicCategory(
    val id: String,
    val name: String,
    val slug: String,
    val children: List<ComicCategory> = emptyList(),
    val search: Boolean = false,
)

enum class CategoryOrder(val label: String) {
    LATEST("最新"), MOST_VIEWED("最多观看"), MOST_IMAGES("最多图片"), MOST_LIKED("最多喜欢")
}

data class CategoryFilter(
    val categorySlug: String = "0",
    val subCategorySlug: String? = null,
    val order: CategoryOrder = CategoryOrder.LATEST,
    val search: Boolean = false,
) {
    val effectiveSlug: String get() = subCategorySlug?.let { "${categorySlug}_$it" } ?: categorySlug
}

data class CategoryComicPage(val comics: List<Comic>, val nextPage: Int?)
