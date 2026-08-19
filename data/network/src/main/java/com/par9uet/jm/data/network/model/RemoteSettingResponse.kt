package com.par9uet.jm.data.network.model

import com.par9uet.jm.core.model.RemoteSetting

data class RemoteSettingResponse(
    val img_host: String,
    val app_shunts: List<Shunt>,
) {
    class Shunt(val title: String, val key: String)

    fun toRemoteSetting(): RemoteSetting = RemoteSetting(
        imgHost = img_host
    )
}
