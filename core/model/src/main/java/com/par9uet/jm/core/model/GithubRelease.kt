package com.par9uet.jm.core.model

data class GithubRelease(
    val version: String,
    val name: String,
    val url: String,
    val body: String,
    val downloadUrl: String,
    val fileName: String,
)
