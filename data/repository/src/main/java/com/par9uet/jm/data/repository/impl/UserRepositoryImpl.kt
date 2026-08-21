package com.par9uet.jm.data.repository.impl

import com.par9uet.jm.core.model.CollectComicOrderFilter
import com.par9uet.jm.core.model.COMIC_API_SOURCE_BUILTIN
import com.par9uet.jm.core.model.COMIC_API_SOURCE_MIXED
import com.par9uet.jm.data.repository.BaseRepository
import com.par9uet.jm.data.repository.UserRepository
import com.par9uet.jm.core.common.log
import com.par9uet.jm.core.common.logError
import com.par9uet.jm.data.network.model.LoginResponse
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.network.model.SignInDataResponse
import com.par9uet.jm.data.network.model.SignInResponse
import com.par9uet.jm.data.network.model.UserCollectComicListResponse
import com.par9uet.jm.data.network.model.UserHistoryComicListResponse
import com.par9uet.jm.data.network.model.UserHistoryCommentListResponse
import com.par9uet.jm.data.network.service.UserService
import com.par9uet.jm.data.storage.CookieStorage
import com.par9uet.jm.core.common.InitManager
import com.par9uet.jm.data.storage.LocalSettingManager
import io.github.jukomu.jmcomic.api.enums.ClientType
import io.github.jukomu.jmcomic.api.model.ForumQuery
import io.github.jukomu.jmcomic.api.model.FavoriteQuery
import io.github.jukomu.jmcomic.api.model.JmAlbumMeta
import io.github.jukomu.jmcomic.api.model.JmCategoryMeta
import io.github.jukomu.jmcomic.api.model.JmComment
import io.github.jukomu.jmcomic.api.model.JmCommentList
import io.github.jukomu.jmcomic.api.model.JmDailyCheckInStatus
import io.github.jukomu.jmcomic.api.model.JmUserInfo
import io.github.jukomu.jmcomic.core.client.impl.JmApiClient
import io.github.jukomu.jmcomic.core.config.JmConfiguration
import io.github.jukomu.jmcomic.core.net.OkHttpBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import java.time.Duration

class UserRepositoryImpl(
    private val service: UserService,
    private val localSettingManager: LocalSettingManager,
    initManager: InitManager,
    private val cookieStorage: CookieStorage,
    private val embeddedClientManager: EmbeddedClientManager,
) : BaseRepository(initManager), UserRepository {

    private companion object {
        /** 补全 tags 的并发上限，避免一页几十项同时打满连接池 */
        const val TAG_FETCH_CONCURRENCY = 4

        /** tags 缓存容量。收藏页翻页与标签统计会反复碰到同一批漫画 */
        const val TAG_CACHE_MAX_ENTRIES = 512
    }

    /** album id -> 完整 tags。按访问顺序淘汰，只缓存成功结果 */
    private val tagCache = object : LinkedHashMap<String, List<String>>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, List<String>>) =
            size > TAG_CACHE_MAX_ENTRIES
    }

    private val tagFetchSemaphore = Semaphore(TAG_FETCH_CONCURRENCY)

    /**
     * 取某本漫画的完整 tags，失败时回退到列表自带的 tags。
     *
     * 失败结果不入缓存：一次网络抖动导致的空 tags 若被记住，
     * 这本漫画在后续的标签排除里会一直被当成「不含任何标签」而漏过滤。
     */
    private suspend fun fetchFullTags(
        client: JmApiClient,
        albumId: String,
        fallback: List<String>,
    ): List<String> {
        if (albumId.isEmpty()) return fallback
        synchronized(tagCache) { tagCache[albumId] }?.let { return it }
        return tagFetchSemaphore.withPermit {
            synchronized(tagCache) { tagCache[albumId] }?.let { return@withPermit it }
            runCatching { client.getAlbum(albumId).tags().orEmpty() }
                .onSuccess { tags -> synchronized(tagCache) { tagCache[albumId] = tags } }
                .getOrDefault(fallback)
        }
    }

    override suspend fun login(username: String, password: String): NetWorkResult<LoginResponse> {
        if (useEmbeddedApi()) {
            return withContext(Dispatchers.IO) {
                try {
                    val userInfo = withEmbeddedClient { client ->
                        client.login(username, password)
                    }
                    NetWorkResult.Success(userInfo.toLoginResponse())
                } catch (e: Exception) {
                    NetWorkResult.Error("内置API登录失败：${e.message ?: "未知错误"}")
                }
            }
        }
        return safeApiCall {
            service.login(username, password)
        }
    }

    override suspend fun getCollectComicList(
        page: Int,
        order: CollectComicOrderFilter,
        folderId: Int
    ): NetWorkResult<UserCollectComicListResponse> {
        if (useEmbeddedApi()) {
            return withContext(Dispatchers.IO) {
                try {
                    val client = embeddedClientManager.getClient()
                    // 内置客户端的 FavoriteQuery 只接受 folderId 与 page，
                    // 不提供排序维度，order 在这条链路上无法生效（上游库限制）。
                    val query = FavoriteQuery.Builder()
                        .folderId(folderId)
                        .page(page)
                        .build()
                    val favPage = client.getFavorites(query)
                    val metas = favPage.content().orEmpty()
                    // 列表接口给的 tags 不全，需要逐项取详情补齐。
                    // 这里必须限流并缓存：标签统计会连翻上百页，
                    // 不加约束时一次统计能打出上千个详情请求。
                    val listWithFullTags = coroutineScope {
                        metas.map { meta ->
                            async {
                                val id = meta.id().orEmpty()
                                val fallback = meta.tags().orEmpty()
                                meta.toListItem(fetchFullTags(client, id, fallback))
                            }
                        }.map { it.await() }
                    }
                    NetWorkResult.Success(
                        UserCollectComicListResponse(
                            count = favPage.totalItems(),
                            folder_list = favPage.folderList(),
                            list = listWithFullTags,
                            total = favPage.totalItems()
                        )
                    )
                } catch (e: Exception) {
                    NetWorkResult.Error("内置API获取收藏列表失败：${e.message ?: "未知错误"}")
                }
            }
        }
        return safeApiCall {
            service.getCollectComicList(page, order.value, folderId)
        }
    }

    override suspend fun getHistoryComicList(page: Int): NetWorkResult<UserHistoryComicListResponse> {
        if (useEmbeddedApi()) {
            return withContext(Dispatchers.IO) {
                try {
                    NetWorkResult.Success(withEmbeddedClient { client ->
                        val albumMetas = client.getWatchHistory(page)
                        UserHistoryComicListResponse(
                            list = albumMetas.map { it.toHistoryListItem() },
                            total = albumMetas.size
                        )
                    })
                } catch (e: Exception) {
                    NetWorkResult.Error("内置API获取历史漫画失败：${e.message ?: "未知错误"}")
                }
            }
        }
        return safeApiCall {
            service.getHistoryComicList(page)
        }
    }

    override suspend fun deleteHistoryComic(id: Int): NetWorkResult<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                withEmbeddedClient { client ->
                    client.deleteWatchHistory(id.toString())
                }
                NetWorkResult.Success(Unit)
            } catch (e: Exception) {
                logError("UserRepositoryImpl", "删除历史记录 id=$id 失败: ${e.message}")
                NetWorkResult.Error("删除历史记录失败：${e.message ?: "未知错误"}")
            }
        }
    }

    override suspend fun getHistoryCommentList(
        page: Int,
        userId: Int
    ): NetWorkResult<UserHistoryCommentListResponse> {
        if (useEmbeddedApi()) {
            return withContext(Dispatchers.IO) {
                try {
                    NetWorkResult.Success(withEmbeddedClient { client ->
                        val query = ForumQuery.user(userId.toString())
                            .page(page)
                            .build()
                        val commentList = client.getComments(query)
                        UserHistoryCommentListResponse(
                            list = commentList.list.map { it.toHistoryCommentListItem() },
                            total = commentList.total
                        )
                    })
                } catch (e: Exception) {
                    NetWorkResult.Error("内置API获取评论历史失败：${e.message ?: "未知错误"}")
                }
            }
        }
        return safeApiCall {
            service.getCommentList(page, userId)
        }
    }

    override suspend fun getSignData(userId: Int): NetWorkResult<SignInDataResponse> {
        if (useEmbeddedApi()) {
            return withContext(Dispatchers.IO) {
                try {
                    NetWorkResult.Success(withEmbeddedClient { client ->
                        val status = client.getDailyCheckInStatus(userId.toString())
                        status.toSignInDataResponse()
                    })
                } catch (e: Exception) {
                    NetWorkResult.Error("内置API获取签到数据失败：${e.message ?: "未知错误"}")
                }
            }
        }
        return safeApiCall {
            service.getSignInData(userId)
        }
    }

    override suspend fun signIn(userId: Int, dailyId: Int): NetWorkResult<SignInResponse> {
        if (useEmbeddedApi()) {
            return withContext(Dispatchers.IO) {
                try {
                    withEmbeddedClient { client ->
                        client.doDailyCheckin(userId.toString(), dailyId.toString())
                    }
                    NetWorkResult.Success(SignInResponse(msg = "签到成功"))
                } catch (e: Exception) {
                    NetWorkResult.Error("内置API签到失败：${e.message ?: "未知错误"}")
                }
            }
        }
        return safeApiCall {
            service.signIn(userId, dailyId)
        }
    }

    private fun useEmbeddedApi(): Boolean {
        val source = localSettingManager.localSettingState.value.comicApiSource
        return source == COMIC_API_SOURCE_BUILTIN || source == COMIC_API_SOURCE_MIXED
    }

    private fun <T> withEmbeddedClient(block: (JmApiClient) -> T): T {
        return block(embeddedClientManager.getClient())
    }

    private fun JmComment.toHistoryCommentListItem(): UserHistoryCommentListResponse.ListItem {
        return UserHistoryCommentListResponse.ListItem(
            AID = aid(),
            BID = bid(),
            CID = commentId(),
            UID = userId(),
            username = username(),
            nickname = nickname(),
            likes = likes().toString(),
            gender = gender(),
            update_at = updateAt(),
            addtime = postDate(),
            parent_CID = parentCommentId(),
            name = name(),
            content = content(),
            photo = photo() ?: "",
            spoiler = spoiler(),
            replys = replys()?.map { it.toHistoryCommentListItem() }
        )
    }

    private fun JmAlbumMeta.toListItem(fullTags: List<String> = tags().orEmpty()): UserCollectComicListResponse.ListItem {
        return UserCollectComicListResponse.ListItem(
            id = id().orEmpty(),
            author = authors().orEmpty().firstOrNull().orEmpty(),
            description = description(),
            name = title().orEmpty(),
            image = image().orEmpty(),
            category = category().toCollectCategory(),
            category_sub = subCategory().toCollectCategory(),
            tags = if (fullTags.isEmpty()) null else fullTags
        )
    }

    private fun JmAlbumMeta.toHistoryListItem(): UserHistoryComicListResponse.ListItem {
        return UserHistoryComicListResponse.ListItem(
            id = id().orEmpty(),
            author = authors().orEmpty().firstOrNull().orEmpty(),
            description = description(),
            name = title().orEmpty(),
            image = image().orEmpty(),
            category = category().toHistoryCategory(),
            category_sub = subCategory().toHistoryCategory()
        )
    }

    private fun JmCategoryMeta?.toHistoryCategory(): UserHistoryComicListResponse.ListItem.Category {
        return UserHistoryComicListResponse.ListItem.Category(
            id = this?.id(),
            title = this?.title()
        )
    }

    private fun JmCategoryMeta?.toCollectCategory(): UserCollectComicListResponse.ListItem.Category {
        return UserCollectComicListResponse.ListItem.Category(
            id = this?.id(),
            title = this?.title()
        )
    }

    private fun JmUserInfo.toLoginResponse(): LoginResponse {
        return LoginResponse(
            uid = uid.toIntOrNull() ?: 0,
            username = username,
            email = email,
            photo = avatarUrl,
            coin = coin.toString(),
            album_favorites = albumFavorites,
            level_name = levelName,
            level = level,
            nextLevelExp = nextLevelExp.toInt(),
            exp = currentExp.toInt(),
            expPercent = expPercent,
            album_favorites_max = maxAlbumFavorites,
        )
    }

    private fun JmDailyCheckInStatus.toSignInDataResponse(): SignInDataResponse {
        return SignInDataResponse(
            daily_id = dailyId,
            three_days_coin = threeDaysCoin,
            three_days_exp = threeDaysExp,
            seven_days_coin = sevenDaysCoin,
            seven_days_exp = sevenDaysExp,
            event_name = eventName,
            background_pc = backgroundPc,
            background_phone = backgroundPhone,
            currentProgress = currentProgress,
            record = record.map { week ->
                week.map { item ->
                    SignInDataResponse.RecordItem(
                        date = item.date,
                        signed = item.signed ?: false,
                        bonus = item.bonus,
                    )
                }
            }
        )
    }
}
