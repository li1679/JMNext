package com.par9uet.jm.data.network.model

import com.par9uet.jm.core.model.Comment
import com.par9uet.jm.core.common.translateCommentTime

data class CommentListResponse(
    val list: List<ListItem>,
    val total: String,
) {
    data class ListItem(
        val AID: String?,
        val BID: String?,
        val CID: String?,
        val UID: String?,
        val username: String?,
        val nickname: String?,
        val likes: String?,
        val gender: String?,
        val update_at: String?,
        val addtime: String?,
        val parent_CID: String?,
        val name: String?,
        val content: String?,
        val photo: String?,
        val spoiler: String?, // 是否剧透 1 和 0
        val replys: List<ListItem>?
    ) {
        fun toComment(): Comment = Comment(
            userId = UID.toIntOrZero(),
            comicId = AID.toIntOrZero(),
            id = CID.toIntOrZero(),
            time = translateCommentTime(addtime.orEmpty()),
            content = content.orEmpty(),
            likeCount = likes.toIntOrZero(),
            username = username.orEmpty(),
            nickname = nickname.orEmpty().ifBlank { username.orEmpty() },
            avatar = photo.orEmpty(),
            parentId = parent_CID.toIntOrZero(),
            spoiler = spoiler == "1",
            replyCommentList = replys?.map { it.toComment() } ?: listOf(),
            sourceComicName = name.orEmpty(),
            sourceChapterId = BID.orEmpty(),
            sourceBlogId = BID.orEmpty()
        )
    }

    fun toCommentList(): List<Comment> {
        return list.map {
            it.toComment()
        }
    }
}

private fun String?.toIntOrZero(): Int = this?.toIntOrNull() ?: 0
