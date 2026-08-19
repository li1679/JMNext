package com.par9uet.jm.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import coil.ImageLoader
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.par9uet.jm.R
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
    val remoteSetting by remoteSettingManager.remoteSettingState.collectAsState()
    val clipboardManager = LocalClipboardManager.current

    Box(modifier = modifier) {
        AsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data("${remoteSetting.imgHost}/media/albums/${comic.id}_3x4.jpg")
                .crossfade(200)
                .build(),
            imageLoader = imageLoader,
            contentDescription = "${comic.name}的封面",
            contentScale = ContentScale.Crop,
            placeholder = painterResource(R.drawable.comic_cover_placeholder),
            error = painterResource(R.drawable.comic_cover_placeholder),
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4f)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
        )
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
