package com.par9uet.jm.ui.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.par9uet.jm.core.common.compareVersion
import com.par9uet.jm.core.model.GithubRelease
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.repository.UpdateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

internal sealed class UpdateState {
    data object Idle : UpdateState()
    data object Checking : UpdateState()
    data class Success(val release: GithubRelease, val hasUpdate: Boolean) : UpdateState()
    data class Error(val message: String) : UpdateState()
}

internal class CheckUpdateViewModel(
    private val updateRepository: UpdateRepository,
) : ViewModel() {
    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state = _state.asStateFlow()
    private var checkJob: Job? = null

    fun checkUpdate(currentVersion: String) {
        checkJob?.cancel()
        _state.value = UpdateState.Checking
        checkJob = viewModelScope.launch {
            _state.value = when (val result = updateRepository.getLatestRelease()) {
                is NetWorkResult.Success -> UpdateState.Success(
                    release = result.data,
                    hasUpdate = compareVersion(result.data.version, currentVersion) > 0,
                )
                is NetWorkResult.Error -> UpdateState.Error(result.message)
            }
        }
    }
}
