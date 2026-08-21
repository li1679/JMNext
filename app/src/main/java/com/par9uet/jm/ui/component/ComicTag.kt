package com.par9uet.jm.ui.component

import android.net.Uri
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.par9uet.jm.navigation.LocalMainNavController
import com.par9uet.jm.core.designsystem.theme.ColorFamily
import com.par9uet.jm.core.designsystem.theme.ExtendedTheme

/** 可点击跳搜索的标签 chip。描边而非填色：详情页一次铺开十几个，实心色块会连成一片盖过封面。 */
@Composable
private fun ComicTag(label: String, family: ColorFamily) {
    val mainNavController = LocalMainNavController.current
    AssistChip(
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        colors = AssistChipDefaults.assistChipColors(
            containerColor = MaterialTheme.colorScheme.surface,
            labelColor = family.onColorContainer,
        ),
        onClick = {
            mainNavController.navigate("comicSearchResult/${Uri.encode(label)}")
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
