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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

class UserManager(
    private val userStorage: UserStorage,
    private val cookieStorage: CookieStorage,
    private val userRepository: UserRepository,
    private val retrofit: Retrofit
) : AppInitTask {
    private val _userState = MutableStateFlow(CommonUIState<User>())
    val userState = _userState.asStateFlow()

    val isLoginState = _userState.map { (it.data?.id ?: 0) > 0 }

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
