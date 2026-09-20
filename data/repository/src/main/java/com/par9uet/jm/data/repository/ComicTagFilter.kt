package com.par9uet.jm.data.repository

import com.par9uet.jm.core.common.isBlockedByTags
import com.par9uet.jm.core.common.normalizeBlockedTagList
import com.par9uet.jm.core.model.Comic
import com.par9uet.jm.data.network.model.NetWorkResult
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit

/** Caches tag metadata, never account-specific detail fields or rule-dependent decisions. */
class ComicTagFilter(private val repository: ComicRepository) {
    private data class Tags(val tags: List<String>, val roles: List<String>, val works: List<String>) {
        fun applyTo(comic: Comic) = comic.copy(
            tagList = tags, roleList = roles, workList = works, tagsComplete = true,
        )
    }

    private val cache = object : LinkedHashMap<Int, Tags>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<Int, Tags>?) = size > 512
    }
    private val locks = Array(32) { Mutex() }
    private val requests = Semaphore(4)

    suspend fun filter(comics: List<Comic>, excludedTags: List<String>): NetWorkResult<List<Comic>> {
        val tags = normalizeBlockedTagList(excludedTags)
        if (tags.isEmpty()) return NetWorkResult.Success(comics)
        return try {
            val result = mutableListOf<Comic>()
            // Bound both active requests and the number of allocated child coroutines.
            comics.chunked(8).forEach { batch ->
                result += coroutineScope {
                    batch.map { comic -> async {
                        completeTags(comic).takeUnless { it.isBlockedByTags(tags) }
                    } }.awaitAll().filterNotNull()
                }
            }
            NetWorkResult.Success(result)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetWorkResult.Error("标签核对失败：${e.message ?: "请重试"}")
        }
    }

    private suspend fun completeTags(comic: Comic): Comic {
        if (comic.tagsComplete) return comic
        return locks[(comic.id and Int.MAX_VALUE) % locks.size].withLock {
            val cached = synchronized(cache) { cache[comic.id] }
            if (cached != null) return@withLock cached.applyTo(comic)
            val detail = requests.withPermit { repository.getComicDetail(comic.id) }
            when (detail) {
                is NetWorkResult.Error -> error(detail.message)
                is NetWorkResult.Success -> {
                    val tags = Tags(detail.data.tags, detail.data.actors, detail.data.works)
                    synchronized(cache) { cache[comic.id] = tags }
                    tags.applyTo(comic)
                }
            }
        }
    }
}
