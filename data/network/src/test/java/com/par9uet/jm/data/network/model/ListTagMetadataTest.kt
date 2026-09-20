package com.par9uet.jm.data.network.model

import org.junit.Assert.assertEquals
import org.junit.Test

class ListTagMetadataTest {
    @Test fun historyPreservesTagsAlongsideCategories() {
        val category = UserHistoryComicListResponse.ListItem.Category(null, "category")
        val item = UserHistoryComicListResponse.ListItem(
            "1", "author", null, "item", "", category, category,
            tags = listOf("blocked", "", "blocked"),
        )
        assertEquals(listOf("blocked", "category"), UserHistoryComicListResponse(listOf(item), 1).toComicList().single().tagList)
    }

    @Test fun weeklyPreservesTagsAlongsideCategories() {
        val category = WeekRecommendComicResponse.ListItem.Category(null, "category")
        val item = WeekRecommendComicResponse.ListItem(
            "1", "author", null, "item", "", category, category, false, false, 0,
            tags = listOf("blocked", "", "blocked"),
        )
        assertEquals(listOf("blocked", "category"), WeekRecommendComicResponse(1, listOf(item)).toComicList().single().tagList)
    }
}
