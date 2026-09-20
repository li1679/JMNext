package com.par9uet.jm.data.repository.impl

import com.par9uet.jm.core.model.ComicSearchOrderFilter
import com.par9uet.jm.core.model.ComicCategory
import com.par9uet.jm.core.model.CategoryFilter
import com.par9uet.jm.core.model.CategoryComicPage
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runInterruptible
import com.par9uet.jm.data.repository.BaseRepository
import com.par9uet.jm.data.repository.ComicRepository
import com.par9uet.jm.data.network.model.CollectComicResponse
import com.par9uet.jm.data.network.model.ComicDetailResponse
import com.par9uet.jm.data.network.model.ComicDetailSeriesListItemResponse
import com.par9uet.jm.data.network.model.ComicListResponse
import com.par9uet.jm.data.network.model.ComicPicListResponse
import com.par9uet.jm.data.network.model.CommentComicResponse
import com.par9uet.jm.data.network.model.CommentListResponse
import com.par9uet.jm.data.network.model.HomeSwiperComicListItemResponse
import com.par9uet.jm.data.network.model.LikeComicResponse
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.network.model.WeekRecommendComicResponse
import com.par9uet.jm.data.network.model.WeekResponse
import com.par9uet.jm.core.common.InitManager
import com.par9uet.jm.core.common.DEFAULT_SCRAMBLE_ID
import com.par9uet.jm.core.common.log
import com.par9uet.jm.core.common.logError
import io.github.jukomu.jmcomic.api.enums.Category
import io.github.jukomu.jmcomic.api.enums.FavoriteFolderType
import io.github.jukomu.jmcomic.api.enums.ForumMode
import io.github.jukomu.jmcomic.api.enums.OrderBy
import io.github.jukomu.jmcomic.api.enums.TimeOption
import io.github.jukomu.jmcomic.api.enums.VoteType
import io.github.jukomu.jmcomic.api.model.ForumQuery
import io.github.jukomu.jmcomic.api.model.JmAlbum
import io.github.jukomu.jmcomic.api.model.JmAlbumMeta
import io.github.jukomu.jmcomic.api.model.JmCategoryMeta
import io.github.jukomu.jmcomic.api.model.JmComment
import io.github.jukomu.jmcomic.api.model.JmImage
import io.github.jukomu.jmcomic.api.model.JmSearchPage
import io.github.jukomu.jmcomic.api.model.JmPromoteCategory
import io.github.jukomu.jmcomic.api.model.JmWeeklyPicksDetail
import io.github.jukomu.jmcomic.api.model.SearchQuery
import io.github.jukomu.jmcomic.core.client.impl.JmApiClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

class ComicRepositoryImpl(
    initManager: InitManager,
    private val embeddedClientManager: EmbeddedClientManager,
) : BaseRepository(initManager), ComicRepository {

    override suspend fun getCategories(): NetWorkResult<List<ComicCategory>> {
        return try {
            val client = getEmbeddedClient()
            val categories = runInterruptible(Dispatchers.IO) { client.getCategoriesList() }
            NetWorkResult.Success(categories.categories().map { category ->
                val slug = if (category.id() == "0" && category.slug().isBlank()) "0" else category.slug()
                check(slug.isNotBlank()) { "数据源返回空分类标识" }
                ComicCategory(category.id(), category.name(), slug, category.subCategories().map { sub ->
                    check(sub.slug().isNotBlank()) { "数据源返回空子分类标识" }
                    ComicCategory(sub.cid(), sub.name(), sub.slug())
                }, search = category.type() == "search")
            })
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetWorkResult.Error("获取分类失败：${e.message}")
        }
    }

    override suspend fun getCategoryComics(page: Int, filter: CategoryFilter): NetWorkResult<CategoryComicPage> {
        return try {
            val query = SearchQuery.Builder().page(page).orderBy(OrderBy.valueOf(filter.order.name))
                .text(if (filter.search) filter.categorySlug else "").build()
            val result = if (filter.search) {
                val client = getEmbeddedClient()
                runInterruptible(Dispatchers.IO) { client.search(query) }
            } else embeddedClientManager.getCategories(filter.effectiveSlug, query)
            NetWorkResult.Success(CategoryComicPage(
                comics = result.toComicListResponse("").toComicList(),
                nextPage = if (result.content().isNotEmpty() && page < result.totalPages()) page + 1 else null,
            ))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetWorkResult.Error("获取分类漫画失败：${e.message}")
        }
    }

    companion object {
        private val imageCache = mutableMapOf<Int, List<JmImage>>()
        private val cleanHttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .connectTimeout(15, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .followRedirects(true)
                .build()
        }
    }

    private fun fixImageUrl(url: String): String {
        // 库的 buildImageList 会将域名前缀拼接到 filename 前。
        // 但 API 有时返回的 image 字段本身就是完整 URL，导致双重拼接：
        // https://cdn-msp.jmapiproxy1.cc/media/photos/1452549/https://tencent.jmdanjonproxy.xyz/media/photos/1452549/00004.webp?t=...
        // 正确的 URL 应该从第二个 https:// 开始
        val secondHttps = url.indexOf("https://", 8)
        return if (secondHttps > 0) url.substring(secondHttps) else url
    }

    private fun buildImageRequest(url: String): Request {
        return Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", "Mozilla/5.0 (Linux; Android 9; V1938CT Build/PQ3A.190705.11211812; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/91.0.4472.114 Safari/537.36")
            .header("Referer", "https://18comic.vip")
            .build()
    }

    override suspend fun getComicDetail(id: Int): NetWorkResult<ComicDetailResponse> {
        return getComicDetailFromEmbeddedApi(id)
    }

    override suspend fun likeComic(id: Int): NetWorkResult<LikeComicResponse> {
        return withContext(Dispatchers.IO) {
            try {
                withEmbeddedClient { client -> client.toggleAlbumLike(id.toString()) }
                NetWorkResult.Success(LikeComicResponse(code = 200, msg = "success", status = "ok"))
            } catch (e: Exception) {
                NetWorkResult.Error("内置 API 点赞失败：${e.message ?: "未知错误"}")
            }
        }
    }

    override suspend fun collectComic(id: Int): NetWorkResult<CollectComicResponse> {
        return withContext(Dispatchers.IO) {
            try {
                withEmbeddedClient { client -> client.toggleAlbumFavorite(id.toString(), "0") }
                NetWorkResult.Success(CollectComicResponse(msg = "success", status = "ok", type = "collect"))
            } catch (e: Exception) {
                NetWorkResult.Error("内置 API 收藏失败：${e.message ?: "未知错误"}")
            }
        }
    }

    override suspend fun unCollectComic(id: Int): NetWorkResult<CollectComicResponse> {
        return withContext(Dispatchers.IO) {
            try {
                withEmbeddedClient { client -> client.toggleAlbumFavorite(id.toString(), "0") }
                NetWorkResult.Success(CollectComicResponse(msg = "success", status = "ok", type = "uncollect"))
            } catch (e: Exception) {
                NetWorkResult.Error("内置 API 取消收藏失败：${e.message ?: "未知错误"}")
            }
        }
    }

    override suspend fun getHomeSwiperComicList(): NetWorkResult<List<HomeSwiperComicListItemResponse>> {
        return getHomeSwiperComicListFromEmbeddedApi()
    }

    override suspend fun getComicPicList(id: Int): NetWorkResult<ComicPicListResponse> {
        return getComicPicListFromEmbeddedApi(id)
    }

    override suspend fun getComicList(
        page: Int,
        order: ComicSearchOrderFilter,
        searchContent: String,
    ): NetWorkResult<ComicListResponse> {
        return getComicListFromEmbeddedApi(page, order, searchContent)
    }

    override suspend fun getWeekData(): NetWorkResult<WeekResponse> {
        return withContext(Dispatchers.IO) {
                try {
                    NetWorkResult.Success(withEmbeddedClient { client ->
                        val picks = client.getWeeklyPicksList()
                        WeekResponse(
                            categories = picks.categories.map { category ->
                                WeekResponse.CategoryItem(
                                    id = category.id(),
                                    time = category.time(),
                                    title = category.title()
                                )
                            },
                            type = picks.type.map { type ->
                                WeekResponse.TypeItem(
                                    id = type.id(),
                                    title = type.title()
                                )
                            }
                        )
                    })
                } catch (e: Exception) {
                    NetWorkResult.Error("内置 API 获取周刊数据失败：${e.message ?: "未知错误"}")
                }
        }
    }

    override suspend fun getWeekRecommendComicList(
        page: Int,
        categoryId: String,
        typeId: String,
    ): NetWorkResult<WeekRecommendComicResponse> {
        return withContext(Dispatchers.IO) {
                try {
                    // 内置客户端的周刊接口只接受 categoryId，不支持分页，也没有 typeId 维度，
                    // 一次就返回该分类的全部条目。第二页起必须返回空，
                    // 否则 Paging 会反复拿到同一批数据，列表里出现重复条目。
                    if (page > 1) {
                        return@withContext NetWorkResult.Success(
                            WeekRecommendComicResponse(total = 0, list = emptyList())
                        )
                    }
                    NetWorkResult.Success(withEmbeddedClient { client ->
                        val detail = client.getWeeklyPicksDetail(categoryId)
                        WeekRecommendComicResponse(
                            total = detail.list.size,
                            list = detail.list.map { albumMeta ->
                                WeekRecommendComicResponse.ListItem(
                                    id = albumMeta.id(),
                                    author = albumMeta.authors().joinToString(", "),
                                    description = albumMeta.description(),
                                    name = albumMeta.title(),
                                    image = albumMeta.image() ?: "",
                                    category = WeekRecommendComicResponse.ListItem.Category(
                                        id = albumMeta.category()?.id(),
                                        title = albumMeta.category()?.title()
                                    ),
                                    category_sub = WeekRecommendComicResponse.ListItem.Category(
                                        id = albumMeta.subCategory()?.id(),
                                        title = albumMeta.subCategory()?.title()
                                    ),
                                    liked = false,
                                    is_favorite = false,
                                    update_at = 0
                                )
                            }
                        )
                    })
                } catch (e: Exception) {
                    NetWorkResult.Error("内置 API 获取周刊详情失败：${e.message ?: "未知错误"}")
                }
        }
    }

    override suspend fun getCommentList(
        page: Int,
        comicId: Int
    ): NetWorkResult<CommentListResponse> {
        return withContext(Dispatchers.IO) {
                try {
                    NetWorkResult.Success(withEmbeddedClient { client ->
                        val query = ForumQuery.album(comicId.toString())
                            .mode(ForumMode.ALL)
                            .page(page)
                            .build()
                        val commentList = client.getComments(query)
                        CommentListResponse(
                            list = commentList.list.map { it.toCommentListItem() },
                            total = commentList.total.toString()
                        )
                    })
                } catch (e: Exception) {
                    NetWorkResult.Error("内置 API 获取评论列表失败：${e.message ?: "未知错误"}")
                }
        }
    }

    override suspend fun comment(
        content: String,
        comicId: Int,
        commentId: Int?
    ): NetWorkResult<CommentComicResponse> {
        return withContext(Dispatchers.IO) {
                try {
                    withEmbeddedClient { client ->
                        if (commentId != null) {
                            client.replyToComment(comicId.toString(), content, commentId.toString())
                        } else {
                            client.postComment(comicId.toString(), content)
                        }
                    }
                    NetWorkResult.Success(
                        CommentComicResponse(
                            msg = "success",
                            status = "ok",
                            aid = comicId,
                            cid = 0,
                            spoiler = "0"
                        )
                    )
                } catch (e: Exception) {
                    NetWorkResult.Error("内置 API 评论失败：${e.message ?: "未知错误"}")
                }
        }
    }

    override suspend fun likeComment(commentId: Int): NetWorkResult<CommentComicResponse> {
        return withContext(Dispatchers.IO) {
            try {
                withEmbeddedClient { client -> client.voteComment(commentId.toString(), VoteType.UP) }
                NetWorkResult.Success(
                    CommentComicResponse(msg = "success", status = "ok", aid = 0, cid = 0, spoiler = "0")
                )
            } catch (e: Exception) {
                NetWorkResult.Error("内置 API 点赞评论失败：${e.message ?: "未知错误"}")
            }
        }
    }

    override suspend fun createFavoriteFolder(name: String): NetWorkResult<Unit> {
        return withContext(Dispatchers.IO) {
                try {
                    withEmbeddedClient { client ->
                        client.manageFavoriteFolder(FavoriteFolderType.ADD, "0", name, "")
                    }
                    NetWorkResult.Success(Unit)
                } catch (e: Exception) {
                    logError("ComicRepositoryImpl", "创建收藏夹失败：${e.message}")
                    NetWorkResult.Error("内置API创建收藏夹失败：${e.message ?: "未知错误"}")
                }
        }
    }

    override suspend fun deleteFavoriteFolder(folderId: String): NetWorkResult<Unit> {
        return withContext(Dispatchers.IO) {
                try {
                    withEmbeddedClient { client ->
                        client.manageFavoriteFolder(FavoriteFolderType.DELETE, folderId, "", "")
                    }
                    NetWorkResult.Success(Unit)
                } catch (e: Exception) {
                    logError("ComicRepositoryImpl", "删除收藏夹失败：${e.message}")
                    NetWorkResult.Error("内置API删除收藏夹失败：${e.message ?: "未知错误"}")
                }
        }
    }

    override suspend fun renameFavoriteFolder(folderId: String, newName: String): NetWorkResult<Unit> {
        return withContext(Dispatchers.IO) {
                try {
                    withEmbeddedClient { client ->
                        client.manageFavoriteFolder(FavoriteFolderType.EDIT, folderId, newName, "")
                    }
                    NetWorkResult.Success(Unit)
                } catch (e: Exception) {
                    logError("ComicRepositoryImpl", "重命名收藏夹失败：${e.message}")
                    NetWorkResult.Error("内置API重命名收藏夹失败：${e.message ?: "未知错误"}")
                }
        }
    }

    override suspend fun moveComicToFolder(comicId: Int, folderId: String): NetWorkResult<Unit> {
        return withContext(Dispatchers.IO) {
                try {
                    withEmbeddedClient { client ->
                        client.manageFavoriteFolder(FavoriteFolderType.MOVE, folderId, "", comicId.toString())
                    }
                    NetWorkResult.Success(Unit)
                } catch (e: Exception) {
                    logError("ComicRepositoryImpl", "移动漫画到收藏夹失败：${e.message}")
                    NetWorkResult.Error("内置API移动漫画到收藏夹失败：${e.message ?: "未知错误"}")
                }
        }
    }

    private suspend fun getEmbeddedClient(): JmApiClient = embeddedClientManager.getClient()

    private suspend fun <T> withEmbeddedClient(block: (JmApiClient) -> T): T {
        return block(getEmbeddedClient())
    }

    private suspend fun getComicDetailFromEmbeddedApi(id: Int): NetWorkResult<ComicDetailResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val client = getEmbeddedClient()
                NetWorkResult.Success(runInterruptible(Dispatchers.IO) { client.getAlbum(id.toString()).toComicDetailResponse() })
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                NetWorkResult.Error("内置 API 获取漫画详情失败：${e.message ?: "未知错误"}")
            }
        }
    }

    private suspend fun getHomeSwiperComicListFromEmbeddedApi(): NetWorkResult<List<HomeSwiperComicListItemResponse>> {
        return withContext(Dispatchers.IO) {
            try {
                val client = getEmbeddedClient()
                fun section(id: String, title: String, load: () -> List<JmAlbumMeta>) = HomeSectionRequest(id, title) {
                    val items = runInterruptible(Dispatchers.IO) { load().map { it.toHomeListItem() } }
                    listOf(HomeSwiperComicListItemResponse(id, title, id, "builtin", "", items))
                }
                fun category(id: String, title: String, value: Category) = section(id, title) {
                    client.getCategories(SearchQuery.Builder().category(value).page(1).build()).content().orEmpty()
                }
                fun ordered(id: String, title: String, order: OrderBy, time: TimeOption) = section(id, title) {
                    client.getCategories(SearchQuery.Builder().orderBy(order).time(time).page(1).build()).content().orEmpty()
                }
                loadHomeSections(listOf(
                    HomeSectionRequest("promote", "首页推荐") {
                        runInterruptible(Dispatchers.IO) { client.getPromote().mapNotNull { it.toHomePromoteCategory() } }
                    },
                    section("builtin_latest", "最新上架") { client.getLatest(1).content().orEmpty() },
                    ordered("builtin_week_hot", "本周热门", OrderBy.MOST_VIEWED, TimeOption.WEEK),
                    ordered("builtin_month_hot", "本月热门", OrderBy.MOST_VIEWED, TimeOption.MONTH),
                    ordered("builtin_most_liked", "最多喜欢", OrderBy.MOST_LIKED, TimeOption.ALL),
                    section("builtin_random", "随机推荐") { client.getRandomRecommend().orEmpty() },
                    section("builtin_serialization", "连载系列") { client.getSerialization(1).content().orEmpty() },
                    category("builtin_doujin", "同人", Category.DOUJIN),
                    category("builtin_single", "单本", Category.SINGLE),
                    category("builtin_short", "短篇", Category.SHORT),
                    category("builtin_korean", "韩漫", Category.KOREAN),
                    category("builtin_american", "美漫", Category.AMERICAN),
                    category("builtin_cosplay", "Cosplay", Category.COSPLAY),
                    category("builtin_3d", "3D", Category.IMAGE_3D),
                    ordered("builtin_most_images", "图片最多", OrderBy.MOST_IMAGES, TimeOption.ALL),
                ))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                NetWorkResult.Error("内置 API 获取首页数据失败：${e.message ?: "未知错误"}")
            }
        }
    }

    private suspend fun getComicPicListFromEmbeddedApi(id: Int): NetWorkResult<ComicPicListResponse> {
        return withContext(Dispatchers.IO) {
            try {
                withEmbeddedClient { client ->
                    val photo = runCatching { client.getPhoto(id.toString()) }.getOrNull()
                    val images = photo?.images()?.takeIf { it.isNotEmpty() }
                        ?: client.getComicRead(id.toString()).images().orEmpty()
                    if (images.isEmpty()) {
                        NetWorkResult.Error("内置 API 未返回图片列表")
                    } else {
                        synchronized(imageCache) {
                            imageCache[id] = images
                        }
                        NetWorkResult.Success(
                            ComicPicListResponse(
                                list = images.map { fixImageUrl(it.getDownloadUrl()) },
                                // 解扰用的 aid 必须是章节自身的 photo id（图片路径 /media/photos/{id}/ 里的那个），
                                // 不是本子的 albumId；多章本子两者不同，用错会整章错版
                                __aId = id,
                                __scrambleId = photo?.scrambleId()?.toIntOrNull()
                                    ?: images.firstOrNull()?.scrambleId()?.toIntOrNull()
                                    ?: DEFAULT_SCRAMBLE_ID,
                                __speed = "0"
                            )
                        )
                    }
                }
            } catch (e: Exception) {
                NetWorkResult.Error("内置 API 获取图片列表失败：${e.message ?: "未知错误"}")
            }
        }
    }

    override suspend fun downloadImageBytes(comicId: Int, imageIndex: Int): ByteArray? {
        val images = synchronized(imageCache) { imageCache[comicId] }
        val image = images?.getOrNull(imageIndex) ?: return null
        val imageUrl = fixImageUrl(image.getDownloadUrl())
        return withContext(Dispatchers.IO) {
            try {
                log("ComicRepositoryImpl", "下载图片 comicId=$comicId index=$imageIndex URL=$imageUrl")
                val request = buildImageRequest(imageUrl)
                cleanHttpClient.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        logError("ComicRepositoryImpl", "下载图片失败 comicId=$comicId index=$imageIndex: HTTP ${response.code} URL=$imageUrl")
                        return@withContext null
                    }
                    response.body?.bytes()
                }
            } catch (e: Exception) {
                logError("ComicRepositoryImpl", "下载图片异常 comicId=$comicId index=$imageIndex: ${e.message} URL=$imageUrl")
                null
            }
        }
    }

    private suspend fun getComicListFromEmbeddedApi(
        page: Int,
        order: ComicSearchOrderFilter,
        searchContent: String,
    ): NetWorkResult<ComicListResponse> {
        return withContext(Dispatchers.IO) {
            try {
                NetWorkResult.Success(withEmbeddedClient { client ->
                    val query = SearchQuery.Builder()
                        .text(searchContent)
                        .page(page)
                        .orderBy(order.toEmbeddedOrderBy())
                        .build()
                    client.search(query).toComicListResponse(searchContent, page)
                })
            } catch (e: Exception) {
                NetWorkResult.Error("内置 API 搜索漫画失败：${e.message ?: "未知错误"}")
            }
        }
    }

    private fun ComicSearchOrderFilter.toEmbeddedOrderBy(): OrderBy {
        return when (this) {
            ComicSearchOrderFilter.NEWEST -> OrderBy.LATEST
            ComicSearchOrderFilter.MOST_COLLECT_COUNT -> OrderBy.MOST_VIEWED
            ComicSearchOrderFilter.MOST_PIC_COUNT -> OrderBy.MOST_IMAGES
            ComicSearchOrderFilter.MOST_LIKE_COUNT -> OrderBy.MOST_LIKED
        }
    }

    private fun JmAlbum.toComicDetailResponse(): ComicDetailResponse {
        return ComicDetailResponse(
            id = id().toIntOrNull() ?: 0,
            name = title().orEmpty(),
            description = description().orEmpty(),
            author = authors().orEmpty(),
            total_views = views().toDisplayCount(),
            likes = likes().toDisplayCount(),
            comment_total = commentCount(),
            tags = tags().orEmpty(),
            actors = actors().orEmpty(),
            works = works().orEmpty(),
            is_favorite = isFavorite,
            liked = liked(),
            series = photoMetas().orEmpty().map {
                ComicDetailSeriesListItemResponse(
                    id = it.id().orEmpty(),
                    name = it.title().orEmpty(),
                    sort = it.sortOrder().toString()
                )
            },
            series_id = seriesId().orEmpty(),
            price = price().orEmpty(),
            purchased = purchased().equals("true", ignoreCase = true)
        )
    }

    private fun JmSearchPage.toComicListResponse(searchContent: String, page: Int = 1): ComicListResponse {
        return ComicListResponse(
            search_query = searchContent,
            total = totalItems().toString(),
            redirect_aid = null,
            content = content().orEmpty().map { it.toContentListItem() },
            nextPage = if (content().isNotEmpty() && page < totalPages()) page + 1 else null,
        )
    }

    private fun JmAlbumMeta.toContentListItem(): ComicListResponse.ContentListItem {
        return ComicListResponse.ContentListItem(
            id = id().orEmpty(),
            author = authors().orEmpty().firstOrNull().orEmpty(),
            description = description(),
            name = title().orEmpty(),
            image = image().orEmpty(),
            category = category().toContentCategory(),
            category_sub = subCategory().toContentCategory(),
            liked = false,
            is_favorite = false,
            update_at = 0,
            tags = tags().orEmpty(),
        )
    }

    private fun JmAlbumMeta.toHomeListItem(): HomeSwiperComicListItemResponse.ListItem {
        return HomeSwiperComicListItemResponse.ListItem(
            id = id().orEmpty(),
            author = authors().orEmpty().firstOrNull().orEmpty(),
            description = description(),
            name = title().orEmpty(),
            image = image().orEmpty(),
            category = category().toHomeCategory(),
            category_sub = subCategory().toHomeCategory(),
            liked = false,
            is_favorite = false,
            update_at = 0,
            tags = tags().orEmpty(),
        )
    }

    private fun JmPromoteCategory.toHomePromoteCategory(): HomeSwiperComicListItemResponse? {
        val items = content().mapNotNull { raw ->
            val id = (raw["id"] ?: raw["album_id"]).toIntOrNull() ?: return@mapNotNull null
            val name = (raw["name"] ?: raw["title"] ?: "").toString().trim()
            if (name.isBlank()) return@mapNotNull null
            val author = when (val value = raw["author"] ?: raw["authors"]) {
                is Iterable<*> -> value.joinToString(", ") { it?.toString().orEmpty() }.trim()
                else -> value?.toString().orEmpty().trim()
            }
            val description = raw["description"]?.toString()
            val image = (raw["image"] ?: raw["cover"] ?: "").toString()
            HomeSwiperComicListItemResponse.ListItem(
                id = id.toString(),
                author = author,
                description = description,
                name = name,
                image = image,
                category = HomeSwiperComicListItemResponse.ListItem.Category(null, null),
                category_sub = HomeSwiperComicListItemResponse.ListItem.Category(null, null),
                liked = false,
                is_favorite = false,
                update_at = 0,
            )
        }
        if (items.isEmpty()) return null
        return HomeSwiperComicListItemResponse(
            id = "promote_${id.ifBlank { title }}",
            title = title.ifBlank { "首页推荐" },
            slug = slug,
            type = type,
            filter_val = filterVal,
            content = items,
        )
    }

    private fun Any?.toIntOrNull(): Int? = when (this) {
        is Number -> toInt()
        else -> toString().trim().toDoubleOrNull()?.toInt()
    }

    private fun JmCategoryMeta?.toContentCategory(): ComicListResponse.ContentListItem.Category {
        return ComicListResponse.ContentListItem.Category(
            id = this?.id(),
            title = this?.title()
        )
    }

    private fun JmCategoryMeta?.toHomeCategory(): HomeSwiperComicListItemResponse.ListItem.Category {
        return HomeSwiperComicListItemResponse.ListItem.Category(
            id = this?.id(),
            title = this?.title()
        )
    }

    private fun JmComment.toCommentListItem(): CommentListResponse.ListItem {
        return CommentListResponse.ListItem(
            AID = null,
            BID = commentId(),
            CID = commentId(),
            UID = userId(),
            username = username(),
            nickname = nickname(),
            likes = likes.toString(),
            gender = gender(),
            update_at = updateAt(),
            addtime = postDate(),
            parent_CID = parentCommentId(),
            name = nickname(),
            content = content(),
            photo = photo() ?: "",
            spoiler = if (spoiler()) "1" else "0",
            replys = replys().orEmpty().map { it.toCommentListItem() }
        )
    }

    private fun String?.toDisplayCount(): Int {
        val text = this?.trim().orEmpty()
        if (text.isBlank()) return 0
        val multiplier = when {
            text.endsWith("K", ignoreCase = true) -> 1_000
            text.endsWith("M", ignoreCase = true) -> 1_000_000
            else -> 1
        }
        val numeric = if (multiplier == 1) text else text.dropLast(1)
        return (numeric.toDoubleOrNull()?.times(multiplier)
            ?: text.filter(Char::isDigit).toDoubleOrNull()
            ?: 0.0).toInt()
    }
}
