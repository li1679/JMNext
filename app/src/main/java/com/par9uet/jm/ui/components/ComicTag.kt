package com.par9uet.jm.ui.components

import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import com.par9uet.jm.ui.screens.LocalMainNavController
import com.par9uet.jm.ui.theme.ColorFamily
import com.par9uet.jm.ui.theme.ExtendedTheme

/** 可点击跳搜索的标签 chip，内容 / 角色 / 作品三种只差色彩 token */
@Composable
private fun ComicTag(label: String, family: ColorFamily) {
    val mainNavController = LocalMainNavController.current
    AssistChip(
        border = null,
        colors = AssistChipDefaults.assistChipColors(
            containerColor = family.colorContainer,
            labelColor = family.onColorContainer,
        ),
        onClick = {
            mainNavController.navigate("comicSearchResult/$label")
        },
        label = { Text(text = label) }
    )
}

@Composable
fun ComicContentTag(label: String) = ComicTag(label, ExtendedTheme.colors.contentTag)

@Composable
fun ComicRoleTag(label: String) = ComicTag(label, ExtendedTheme.colors.roleTag)

@Composable
fun ComicWorkTag(label: String) = ComicTag(label, ExtendedTheme.colors.workTag)
