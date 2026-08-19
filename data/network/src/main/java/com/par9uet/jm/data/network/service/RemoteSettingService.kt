package com.par9uet.jm.data.network.service

import com.par9uet.jm.data.network.annotation.GInit
import com.par9uet.jm.data.network.model.RemoteSettingResponse
import com.par9uet.jm.data.network.model.ResponseWrapper
import retrofit2.http.GET

interface RemoteSettingService {
    @GInit
    @GET("setting")
    suspend fun getRemoteSetting(): ResponseWrapper<RemoteSettingResponse>
}