package com.par9uet.jm.data.network.model

data class CommentComicResponse(
    val msg: String,
    val status: String,
    val aid: Int,
    val cid: Int,
    val spoiler: String,
)