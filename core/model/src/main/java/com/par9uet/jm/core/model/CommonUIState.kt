package com.par9uet.jm.core.model

data class CommonUIState<T>(
    val isLoading: Boolean = false,
    val isError: Boolean = false,
    val data: T? = null,
    val errorMsg: String? = null
) {}
