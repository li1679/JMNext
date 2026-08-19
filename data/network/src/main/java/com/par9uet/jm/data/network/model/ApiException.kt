package com.par9uet.jm.data.network.model

class ApiException(
    val errorCode: Int,
    override val message: String
): Exception(message)