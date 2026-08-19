package com.par9uet.jm.core.model

data class WeekData(
    val categoryList: List<Pair<String, String>> = listOf(),
    val typeList: List<Pair<String, String>> = listOf()
)