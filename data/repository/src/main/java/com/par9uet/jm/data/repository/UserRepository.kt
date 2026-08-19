package com.par9uet.jm.data.repository

import com.par9uet.jm.core.model.CollectComicOrderFilter
import com.par9uet.jm.data.network.model.LoginResponse
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.network.model.SignInDataResponse
import com.par9uet.jm.data.network.model.SignInResponse
import com.par9uet.jm.data.network.model.UserCollectComicListResponse
import com.par9uet.jm.data.network.model.UserHistoryComicListResponse
import com.par9uet.jm.data.network.model.UserHistoryCommentListResponse

interface UserRepository {
    suspend fun login(username: String, password: String): NetWorkResult<LoginResponse>
    suspend fun getCollectComicList(
        page: Int = 1,
        order: CollectComicOrderFilter = CollectComicOrderFilter.COLLECT_TIME,
        folderId: Int = 0
    ): NetWorkResult<UserCollectComicListResponse>

    suspend fun getHistoryComicList(page: Int = 1): NetWorkResult<UserHistoryComicListResponse>
    suspend fun deleteHistoryComic(id: Int): NetWorkResult<Unit>
    suspend fun getHistoryCommentList(
        page: Int = 1,
        userId: Int
    ): NetWorkResult<UserHistoryCommentListResponse>

    suspend fun getSignData(
        userId: Int,
    ): NetWorkResult<SignInDataResponse>

    suspend fun signIn(
        userId: Int,
        dailyId: Int,
    ): NetWorkResult<SignInResponse>
}