package com.par9uet.jm.domain.cache

import kotlinx.coroutines.sync.Mutex

/** Bounded lock stripes coordinate worker shutdown and shared file commits. */
object DownloadFileLocks {
    private val chapters = Array(256) { Mutex() }
    private val groups = Array(256) { Mutex() }
    fun chapter(id: Int): Mutex = chapters[Math.floorMod(id, chapters.size)]
    fun group(id: Int): Mutex = groups[Math.floorMod(id, groups.size)]
}
