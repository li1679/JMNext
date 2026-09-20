package com.par9uet.jm.domain.worker

import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException

internal suspend fun <T : Any> withDownloadTimeout(timeoutMs: Long, block: suspend () -> T): T =
    withTimeoutOrNull(timeoutMs) { block() }
        ?: throw IOException("下载操作超时")
