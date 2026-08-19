package com.par9uet.jm.domain.store

import com.par9uet.jm.core.model.RemoteSetting
import com.par9uet.jm.data.repository.RemoteSettingRepository
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.network.model.RemoteSettingResponse
import com.par9uet.jm.core.common.AppInitTask
import com.par9uet.jm.core.common.AppTaskInfo
import com.par9uet.jm.core.common.log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withTimeoutOrNull

class RemoteSettingManager(
    private val remoteSettingRepository: RemoteSettingRepository
) : AppInitTask {

    companion object {
        /**
         * 图片域名的兜底值。
         * 远端设置拉取失败时若留空，封面地址会退化成不含域名的相对路径
         * （形如 /media/albums/123_3x4.jpg），导致全站封面加载失败。
         */
        const val FALLBACK_IMG_HOST = "https://cdn-msp.jmapiproxy1.cc"

        /** 拉取远程设置的时间上限，超过就放弃并继续启动 */
        private const val REMOTE_SETTING_TIMEOUT_MS = 12_000L
    }

    private val _remoteSettingState = MutableStateFlow(RemoteSetting(
        imgHost = FALLBACK_IMG_HOST
    ))
    val remoteSettingState = _remoteSettingState.asStateFlow()

    private var appTaskInfo = AppTaskInfo(
        taskName = "加载 app 远端应用数据",
        sort = 2,
    )

    private suspend fun getRemoteSetting() {
        when (val data = remoteSettingRepository.getRemoteSetting()) {
            is NetWorkResult.Error -> {
                log("获取远程应用设置失败，封面沿用兜底图片域名：${data.message}")
                appTaskInfo = appTaskInfo.copy(
                    isError = true,
                    errorMsg = data.message
                )
            }

            is NetWorkResult.Success<RemoteSettingResponse> -> {
                log("获取远程应用设置成功")
                _remoteSettingState.update { current ->
                    val fetched = data.data.toRemoteSetting()
                    // 服务端偶尔会返回空的 img_host，此时保留当前可用值
                    if (fetched.imgHost.isBlank()) current else fetched
                }
                appTaskInfo = appTaskInfo.copy(
                    isError = false,
                    errorMsg = "",
                )
            }
        }
    }

    override suspend fun init() {
        log("远程应用设置开始初始化")
        // 初始化任务串行执行，且未完成前其它请求都阻塞在 InitInterceptor 上，
        // 因此这里必须封顶：线路全部失效时不能让启动一直卡着，
        // 图片域名已有兜底值，拿不到远程设置也不影响继续使用。
        val finished = withTimeoutOrNull(REMOTE_SETTING_TIMEOUT_MS) {
            getRemoteSetting()
            true
        }
        if (finished == null) {
            log("获取远程应用设置超时，跳过并沿用兜底图片域名")
            appTaskInfo = appTaskInfo.copy(
                isError = true,
                errorMsg = "获取远程应用设置超时"
            )
        }
        log("远程应用设置初始化结束")
    }

    override fun getAppTaskInfo(): AppTaskInfo = appTaskInfo
}