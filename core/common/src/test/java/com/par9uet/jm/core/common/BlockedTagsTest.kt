package com.par9uet.jm.core.common

import com.par9uet.jm.core.model.Comic
import org.junit.Assert.assertEquals
import org.junit.Test

class BlockedTagsTest {
    @Test
    fun filtersGlobalTagsAcrossComicTagFields() {
        val visible = Comic.create(1, "visible", listOf("author"))
        val blocked = Comic(
            id = 2,
            name = "blocked",
            description = "",
            readCount = 0,
            likeCount = 0,
            commentCount = 0,
            tagList = listOf("safe"),
            roleList = listOf("blocked-role"),
            workList = listOf(),
            price = 0,
        )

        assertEquals(listOf(visible), listOf(visible, blocked).filterBlockedTags(listOf("blocked-role")))
    }
}
