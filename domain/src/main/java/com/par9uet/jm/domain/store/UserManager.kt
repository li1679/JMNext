package com.par9uet.jm.domain.store

import com.par9uet.jm.core.model.User
import com.par9uet.jm.data.repository.UserRepository
import com.par9uet.jm.data.network.Retrofit
import com.par9uet.jm.data.network.model.LoginResponse
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.storage.CookieStorage
import com.par9uet.jm.data.storage.UserStorage
import com.par9uet.jm.core.common.AppInitTask
import com.par9uet.jm.core.common.AppTaskInfo
import com.par9uet.jm.core.model.CommonUIState
import com.par9uet.jm.core.common.log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class UserManager(
    private val userStorage: UserStorage,
    private val cookieStorage: CookieStorage,
    private val userRepository: UserRepository,
    private val retrofit: Retrofit,
    private val scope: CoroutineScope,
) : AppInitTask {
    private val _userState = MutableStateFlow(CommonUIState<User>())
    val userState = _userState.asStateFlow()

    /**
     * 是否已登录。
     *
     * 必须是有当前值的 StateFlow：做成冷 Flow 的话，每个收集点都得自己传一个
     * 初始值（此前一律传 false），而收集开始得晚一拍时，
     * 依赖它的 `LaunchedEffect(isLogin)` 就会拿着这个假的 false 先跑一遍——
     * 表现为已登录用户打开评论页却被弹去登录页。
     */
    val isLoginState: StateFlow<Boolean> = _userState
        .map { (it.data?.id ?: 0) > 0 }
        .stateIn(
            scope,
            SharingStarted.Eagerly,
            (_userState.value.data?.id ?: 0) > 0
        )

    private val appTaskInfo = AppTaskInfo(
        taskName = "加载上次退出前保存的用户信息",
        sort = 4,
    )

    fun updateUser(user: User) {
        _userState.update {
            it.copy(
                data = user
            )
        }
        userStorage.set(user)
    }

    fun clearUser() {
        _userState.update {
            it.copy(
                data = User.create()
            )
        }
        retrofit.clearCookie()
        userStorage.remove()
        cookieStorage.remove()
    }

    suspend fun autoLogin(username: String, password: String) {
        _userState.update {
            it.copy(
                isLoading = true,
                isError = false,
                errorMsg = ""
            )
        }
        when (val data = userRepository.login(username, password)) {
            is NetWorkResult.Error -> {
                _userState.update {
                    it.copy(
                        isError = true,
                        errorMsg = data.message,
                        data = User.create()
                    )
                }
            }

            is NetWorkResult.Success<LoginResponse> -> {
                updateUser(
                    data.data.toUser(
                        password = password
                    )
                )
            }
        }
        _userState.update {
            it.copy(
                isLoading = false
            )
        }
    }

    override suspend fun init() {
        log("用户信息开始初始化")
        log("加载本地用户、cookie、登录信息")
        _userState.update {
            it.copy(
                data = userStorage.get()
            )
        }
        log("已加载本地用户、cookie、登录信息")
        val userData = _userState.value.data
        if (userData != null && userData.username.isNotEmpty() && userData.password.isNotEmpty()) {
            val username = userData.username
            val password = userData.password
            log("检测到已保存了用户登录信息，开始执行一次用户登录")
            autoLogin(username, password)
        }
        log("用户信息初始化结束")
    }

    override fun getAppTaskInfo(): AppTaskInfo = appTaskInfo
}
