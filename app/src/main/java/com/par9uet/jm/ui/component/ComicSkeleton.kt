package com.par9uet.jm.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.par9uet.jm.core.designsystem.util.shimmer

/**
 * 漫画卡片的骨架屏。
 * 结构、圆角、行数需与 [Comic] 真实卡片一致，否则加载完成时布局会跳。
 */
@Composable
fun ComicSkeleton(
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(MaterialTheme.shapes.medium)
                .shimmer()
        )
        // 标题占两行
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .shimmer()
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(14.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .shimmer()
        )
        // 作者行更短
        Box(
            modifier = Modifier
                .fillMaxWidth(0.4f)
                .height(11.dp)
                .clip(MaterialTheme.shapes.extraSmall)
                .shimmer()
        )
    }
}
