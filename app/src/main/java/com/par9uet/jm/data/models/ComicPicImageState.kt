package com.par9uet.jm.data.models

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.toBitmap
import coil.ImageLoader
import coil.request.ErrorResult
import coil.request.ImageRequest
import coil.request.SuccessResult
import coil.size.Size
import com.par9uet.jm.cache.getCommonPicDecodeCacheDir
import com.par9uet.jm.utils.WEBP_QUALITY_CACHE
import com.par9uet.jm.utils.calculateScrambleSeed
import com.par9uet.jm.utils.compressWebpCompat
import com.par9uet.jm.utils.logError
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

sealed class ImageResultState {
    object Loading : ImageResultState()
    data class Success(
        val decodeImageBitmap: ImageBitmap,
        val decodeImageAspectRatio: Float
    ) :
        ImageResultState()

    data class Failure(val reason: String) : ImageResultState()
}

class ComicPicImageState(
    val index: Int,
    val comicId: Int,
    val originSrc: String,
    val __scrambleId: Int,
    val __speed: String,
    private val picImageLoader: ImageLoader,
    private val imageFetcher: (suspend () -> ByteArray?)? = null,
    /**
     * 解扰计算使用的 aid，即 photo 页面里的 `var aid`。
     * 禁漫的分块数由 aid 而非本子 id 决定，多章本子两者并不相同，
     * 传错会导致整章错版。取值 <= 0 时回退到 comicId 保持旧行为。
     */
    val __aId: Int = 0,
) {

    /** 真正参与分块数计算的 aid */
    private val scrambleAid: Int
        get() = if (__aId > 0) __aId else comicId

    var imageResultState by mutableStateOf<ImageResultState>(ImageResultState.Loading)

    suspend fun decode(context: Context, downscale: Boolean = false) {
        withContext(Dispatchers.Default) {
            imageResultState = ImageResultState.Loading
            try {
                decodeImage(context, downscale)
            } catch (e: OutOfMemoryError) {
                logError("ComicPicImage", "解码图片 OOM: ${e.message}")
                System.gc()
                imageResultState = ImageResultState.Failure("内存不足，无法解码图片")
            } catch (e: Exception) {
                logError("ComicPicImage", "解码图片异常: ${e.stackTraceToString()}")
                imageResultState = ImageResultState.Failure("图片解码失败：${e.message ?: "未知错误"}")
            }
        }
    }

    private suspend fun decodeImage(context: Context, downscale: Boolean = false) {
        val cacheDir = getCommonPicDecodeCacheDir(context, comicId)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val page = extractPageFromUrl()
        // GIF 与服务端标记 speed=1 的图不做解扰，其余由 aid/scrambleId 决定分块数
        val seed = if (isGif() || __speed == "1") 0 else calculateSeed(page)
        // 缓存文件名带上分块数指纹：解扰参数一旦变化，旧缓存自动失效而不会被误当作成品复用。
        // 这是「同一本有时错版有时正常」的根因——之前的缓存键只有页码，一次错误解码会被永久沿用。
        val cacheFile = File(cacheDir, "${page}_s$seed.webp")

        // 检查缓存文件是否存在
        if (cacheFile.exists()) {
            try {
                val options = if (downscale) {
                    BitmapFactory.Options().apply { inSampleSize = 2 }
                } else null
                val decodeImageBitmap =
                    BitmapFactory.decodeFile(cacheFile.absolutePath, options)?.asImageBitmap()
                        ?: run {
                            cacheFile.delete()
                            throw IllegalStateException("缓存图片解码为空")
                        }
                val decodeImageAspectRatio =
                    decodeImageBitmap.width * 1.0f / decodeImageBitmap.height
                imageResultState = ImageResultState.Success(decodeImageBitmap, decodeImageAspectRatio)
                return
            } catch (e: Exception) {
                logError("ComicPicImage", "缓存图片解码失败，删除并重新解码: ${e.message}")
                cacheFile.delete()
            }
        }

        // 加载原始图片
        val imageData = File(originSrc).takeIf { it.exists() } ?: originSrc
        val request = ImageRequest.Builder(context)
            .data(imageData)
            // 这里必须使用原始 size ，不然解密会有问题，出现白线
            .size { Size.ORIGINAL }
            .allowHardware(false)
            .build()

        when (val result = picImageLoader.execute(request)) {
            is SuccessResult -> {
                imageResultState = processBitmap(result.drawable.toBitmap(), seed, cacheFile)
            }

            is ErrorResult -> {
                // Coil 加载失败，尝试使用内置 API 的 imageFetcher 回退
                val fetchedBytes = try {
                    imageFetcher?.invoke()
                } catch (e: Exception) {
                    logError("ComicPicImage", "imageFetcher 调用失败: ${e.stackTraceToString()}")
                    null
                }
                if (fetchedBytes != null) {
                    try {
                        val options = if (downscale) {
                            BitmapFactory.Options().apply { inSampleSize = 2 }
                        } else null
                        val originalBitmap = BitmapFactory
                            .decodeByteArray(fetchedBytes, 0, fetchedBytes.size, options)
                        if (originalBitmap != null) {
                            imageResultState = processBitmap(originalBitmap, seed, cacheFile)
                            return
                        }
                    } catch (e: OutOfMemoryError) {
                        logError("ComicPicImage", "内置API图片解码 OOM: ${e.message}")
                        System.gc()
                        imageResultState = ImageResultState.Failure("内存不足")
                        return
                    } catch (e: Exception) {
                        logError("ComicPicImage", "内置API图片解码失败: ${e.stackTraceToString()}")
                    }
                }
                logError("ComicPicImage", "图片加载失败: ${result.throwable.stackTraceToString()}")
                imageResultState = ImageResultState.Failure("网络错误")
            }
        }
    }

    /**
     * 按分块数还原图片并写入缓存。seed 为 0 表示该图无需解扰。
     * 两条加载路径（Coil / 内置 API 回退）共用，避免解扰判断出现分叉。
     */
    private suspend fun processBitmap(
        originalBitmap: Bitmap,
        seed: Int,
        cacheFile: File
    ): ImageResultState {
        return try {
            val aspectRatio = originalBitmap.width * 1.0f / originalBitmap.height
            val finalBitmap = if (seed == 0) {
                originalBitmap
            } else {
                decodeBitmap(originalBitmap, seed)
            }
            saveBitmapAsWebp(finalBitmap, cacheFile)
            ImageResultState.Success(finalBitmap.asImageBitmap(), aspectRatio)
        } catch (e: OutOfMemoryError) {
            logError("ComicPicImage", "图片处理 OOM: ${e.message}")
            System.gc()
            ImageResultState.Failure("内存不足")
        } catch (e: Exception) {
            logError("ComicPicImage", "图片处理失败: ${e.stackTraceToString()}")
            ImageResultState.Failure("图片处理失败：${e.message ?: "未知错误"}")
        }
    }

    private fun decodeBitmap(originalBitmap: Bitmap, seed: Int): Bitmap {
        val naturalWidth = originalBitmap.width
        val naturalHeight = originalBitmap.height
        val remainder = naturalHeight % seed

        val decodedBitmap =
            createBitmap(naturalWidth, naturalHeight)
        val canvas = Canvas(decodedBitmap.asImageBitmap())
        val paint = Paint().apply {
            this.isAntiAlias = false
        }
        val originImageBitmap = originalBitmap.asImageBitmap()

        for (i in 0 until seed) {
            var height = naturalHeight / seed
            var dy = height * i
            val sy = naturalHeight - height * (i + 1) - remainder
            if (i == 0) {
                height += remainder
            } else {
                dy += remainder
            }

            val srcOffset = IntOffset(0, sy)
            val srcSize = IntSize(naturalWidth, height)
            val destOffset = IntOffset(0, dy)
            val destSize = IntSize(naturalWidth, height)

            canvas.drawImageRect(
                originImageBitmap,
                srcOffset,
                srcSize,
                destOffset,
                destSize,
                paint
            )
        }

        return decodedBitmap
    }

    /**
     * 计算图片的纵向分块数，返回 0 表示该图无需解扰。
     */
    private fun calculateSeed(pageStr: String): Int =
        calculateScrambleSeed(aid = scrambleAid, scrambleId = __scrambleId, pageStr = pageStr)

    private fun extractPageFromUrl(): String {
        return originSrc.substringBefore('?').substringAfterLast('/').substringBeforeLast('.')
    }

    private suspend fun saveBitmapAsWebp(bitmap: Bitmap, file: File) {
        withContext(Dispatchers.IO) {
            FileOutputStream(file).use { out ->
                bitmap.compressWebpCompat(WEBP_QUALITY_CACHE, out)
            }
        }
    }

    private fun isGif(): Boolean {
        return originSrc.substringBefore('?').endsWith(".gif", ignoreCase = true)
    }
}
