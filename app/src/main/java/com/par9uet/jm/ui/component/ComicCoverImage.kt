package com.par9uet.jm.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ImageNotSupported
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.compose.AsyncImagePainter
import coil.request.ImageRequest
import com.par9uet.jm.core.designsystem.util.shimmer
import com.par9uet.jm.core.model.Comic
import com.par9uet.jm.domain.store.RemoteSettingManager
import com.par9uet.jm.core.common.ToastManager
import org.koin.compose.getKoin

@Composable
fun ComicCoverImage(
    comic: Comic,
    modifier: Modifier = Modifier.fillMaxWidth(),
    showIdChip: Boolean = false,
    koin: org.koin.core.Koin = getKoin(),
    remoteSettingManager: RemoteSettingManager = koin.get(),
    imageLoader: ImageLoader = koin.get(),
    toastManager: ToastManager = koin.get(),
) {
    val remoteSetting by remoteSettingManager.remoteSettingState.collectAsStateWithLifecycle()
    val clipboardManager = LocalClipboardManager.current

    Box(modifier = modifier) {
        // 封面加载前后都占同一块 3:4 区域，避免网格抖动。
        // 加载中走微光占位，失败给一个淡图标——不再显示写着尺寸的占位图。
        var painterState by remember(comic.id) {
            mutableStateOf<AsyncImagePainter.State>(AsyncImagePainter.State.Empty)
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
        ) {
            when (painterState) {
                is AsyncImagePainter.State.Success -> Unit
                is AsyncImagePainter.State.Error -> Icon(
                    imageVector = Icons.Rounded.ImageNotSupported,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.35f),
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(32.dp)
                )

                else -> Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .shimmer()
                )
            }
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data("${remoteSetting.imgHost}/media/albums/${comic.id}_3x4.jpg")
                    .crossfade(200)
                    .build(),
                imageLoader = imageLoader,
                contentDescription = "${comic.name}的封面",
                contentScale = ContentScale.Crop,
                onState = { painterState = it },
                modifier = Modifier.fillMaxSize(),
            )
        }
        if (showIdChip) {
            // 点击复制漫画编码
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = MaterialTheme.shapes.small,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(end = 10.dp, top = 10.dp)
                    .clickable {
                        clipboardManager.setText(AnnotatedString(comic.id.toString()))
                        toastManager.showAsync("已复制漫画编码：${comic.id}")
                    },
            ) {
                Text(
                    text = "JM${comic.id}",
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.labelSmall,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
