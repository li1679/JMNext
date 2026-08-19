package com.par9uet.jm.data.repository.impl

import com.par9uet.jm.data.repository.BaseRepository
import com.par9uet.jm.data.repository.RemoteSettingRepository
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.network.model.RemoteSettingResponse
import com.par9uet.jm.data.network.service.RemoteSettingService
import com.par9uet.jm.core.common.InitManager

class RemoteSettingRepositoryImpl(
    private val service: RemoteSettingService,
    initManager: InitManager
) : BaseRepository(initManager), RemoteSettingRepository {
    override suspend fun getRemoteSetting(): NetWorkResult<RemoteSettingResponse> {
        return safeApiCall {
            service.getRemoteSetting()
        }
    }
}