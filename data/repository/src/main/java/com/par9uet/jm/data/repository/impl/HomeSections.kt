package com.par9uet.jm.data.repository.impl

import com.par9uet.jm.data.network.model.HomeSwiperComicListItemResponse
import com.par9uet.jm.data.network.model.NetWorkResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope

internal data class HomeSectionRequest(
    val id: String,
    val title: String,
    val load: suspend () -> List<HomeSwiperComicListItemResponse>,
)

internal suspend fun loadHomeSections(requests: List<HomeSectionRequest>): NetWorkResult<List<HomeSwiperComicListItemResponse>> = coroutineScope {
    val results = requests.map { request -> async {
        try {
            NetWorkResult.Success(request.load())
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetWorkResult.Error("${request.title}：${e.message ?: "加载失败"}")
        }
    } }.awaitAll()
    if (results.all { it is NetWorkResult.Error }) {
        NetWorkResult.Error("首页加载失败，请重试：${(results.firstOrNull() as? NetWorkResult.Error)?.message.orEmpty()}")
    } else {
        NetWorkResult.Success(results.flatMapIndexed { index, result ->
            when (result) {
                is NetWorkResult.Success -> result.data
                is NetWorkResult.Error -> listOf(HomeSwiperComicListItemResponse(
                    requests[index].id, requests[index].title, requests[index].id,
                    "builtin", "", emptyList(), errorMessage = result.message,
                ))
            }
        })
    }
}
