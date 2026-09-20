package com.par9uet.jm.data.repository

import com.par9uet.jm.core.common.filterBlockedTags
import com.par9uet.jm.core.model.Comic
import com.par9uet.jm.data.network.model.NetWorkResult

/** Filter known metadata locally; missing tags must not block list loading. */
class ComicTagFilter {
    suspend fun filter(comics: List<Comic>, excludedTags: List<String>): NetWorkResult<List<Comic>> =
        NetWorkResult.Success(comics.filterBlockedTags(excludedTags))
}
