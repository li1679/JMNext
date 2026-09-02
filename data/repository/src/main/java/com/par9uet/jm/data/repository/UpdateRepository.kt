package com.par9uet.jm.data.repository

import com.par9uet.jm.core.model.GithubRelease
import com.par9uet.jm.data.network.model.NetWorkResult

interface UpdateRepository {
    companion object {
        const val RELEASE_API = "https://api.github.com/repos/li1679/JMNext/releases/latest"
        const val RELEASES_URL = "https://github.com/li1679/JMNext/releases"
    }

    suspend fun getLatestRelease(): NetWorkResult<GithubRelease>
}
