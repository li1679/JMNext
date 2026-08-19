package com.par9uet.jm.data.repository

import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.network.model.RemoteSettingResponse

interface RemoteSettingRepository {
    suspend fun getRemoteSetting(): NetWorkResult<RemoteSettingResponse>
}