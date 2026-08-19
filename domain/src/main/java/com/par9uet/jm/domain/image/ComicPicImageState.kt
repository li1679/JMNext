package com.par9uet.jm.domain.image

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.asAndroidBitmap
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
import com.par9uet.jm.domain.cache.getCommonPicDecodeCacheDir
import com.par9uet.jm.core.common.WEBP_QUALITY_CACHE
import com.par9uet.jm.core.common.calculateScrambleSeed
import com.par9uet.jm.core.common.compressWebpCompat
import com.par9uet.jm.core.common.logError
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
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
     * 解扰用的 aid（photo 页面里的 `var aid`），与本子 id 在多章本子上不同，传错会整章错版。
     * <= 0 时回退到 comicId。
     */
    val __aId: Int = 0,
) {

    /** 真正参与分块数计算的 aid */
    private val scrambleAid: Int
        get() = if (__aId > 0) __aId else comicId

    private val stateLock = Any()
    private var decodeGeneration = 0L

    var imageResultState by mutableStateOf<ImageResultState>(ImageResultState.Loading)

    private fun beginDecode(): Long = synchronized(stateLock) {
        decodeGeneration += 1
        imageResultState = ImageResultState.Loading
        decodeGeneration
    }

    /**
     * 只允许当前这一轮解码写回。
     *
     * cancel 与位图处理不在同一线程，单独调用 ensureActive 后仍存在“检查通过、随后被
     * release、最后旧任务写回”的竞态。把代次检查和状态写入放进同一临界区后，旧任务
     * 无法复活已经被逐出保留窗口的全尺寸位图。
     */
    private fun publishIfCurrent(generation: Long, state: ImageResultState): Boolean =
        synchronized(stateLock) {
            if (generation != decodeGeneration) {
                false
            } else {
                imageResultState = state
                true
            }
        }

    /** 释放位图引用并退回未加载态。只丢引用不 recycle：该位图可能正被 Compose 绘制。 */
    fun release() {
        synchronized(stateLock) {
            decodeGeneration += 1
            imageResultState = ImageResultState.Loading
        }
    }

    suspend fun decode(context: Context) {
        val generation = beginDecode()
        withContext(Dispatchers.Default) {
            try {
                decodeImage(context, generation)
            } catch (e: CancellationException) {
                // 快速翻页时，上一批预加载会被成批取消，这是正常流程而非故障。
                // 必须原样抛出：捕获它等于吞掉协程的取消信号，
                // 记成错误还会让日志里刷满并不存在的失败。
                throw e
            } catch (e: OutOfMemoryError) {
                logError("ComicPicImage", "解码图片内存不足: ${e.message}")
                publishIfCurrent(generation, ImageResultState.Failure("内存不足，无法解码图片"))
            } catch (e: Exception) {
                logError("ComicPicImage", "解码图片异常: ${e.stackTraceToString()}")
                publishIfCurrent(
                    generation,
                    ImageResultState.Failure("图片解码失败：${e.message ?: "未知错误"}")
                )
            }
        }
    }

    private suspend fun decodeImage(context: Context, generation: Long) {
        val cacheDir = getCommonPicDecodeCacheDir(context, comicId)
        if (!cacheDir.exists()) {
            cacheDir.mkdirs()
        }
        val page = extractPageFromUrl()
        // GIF 与服务端标记 speed=1 的图不做解扰，其余由 aid/scrambleId 决定分块数
        val seed = if (isGif() || __speed == "1") 0 else calculateSeed(page)
        // 缓存文件名必须带上分块数指纹：解扰参数变化时旧缓存要自动失效，
        // 否则一次错误解码的结果会被当作成品长期复用，表现为「同一本有时错版」。
        val cacheFile = File(cacheDir, "${page}_s$seed.webp")

        if (cacheFile.exists()) {
            var decodedBitmap: Bitmap? = null
            var published = false
            try {
                decodedBitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
                    ?: run {
                        cacheFile.delete()
                        throw IllegalStateException("缓存图片解码为空")
                    }
                val decodeImageAspectRatio =
                    decodedBitmap.width * 1.0f / decodedBitmap.height
                currentCoroutineContext().ensureActive()
                published = publishIfCurrent(
                    generation,
                    ImageResultState.Success(decodedBitmap.asImageBitmap(), decodeImageAspectRatio)
                )
                if (!published) decodedBitmap.recycle()
                return
            } catch (e: CancellationException) {
                if (!published && decodedBitmap?.isRecycled == false) decodedBitmap.recycle()
                throw e
            } catch (e: Exception) {
                if (!published && decodedBitmap?.isRecycled == false) decodedBitmap.recycle()
                logError("ComicPicImage", "缓存图片解码失败，删除并重新解码: ${e.message}")
                cacheFile.delete()
            }
        }

        val imageData = File(originSrc).takeIf { it.exists() } ?: originSrc
        val request = ImageRequest.Builder(context)
            .data(imageData)
            // 这里必须使用原始 size ，不然解密会有问题，出现白线
            .size { Size.ORIGINAL }
            .allowHardware(false)
            .build()

        when (val result = picImageLoader.execute(request)) {
            is SuccessResult -> {
                val drawable = result.drawable
                // BitmapDrawable 时 toBitmap() 直接返回底层位图，而那张图归 Coil 的
                // 内存缓存所有；其余类型才是这里新建的，可以安全回收。
                val bitmap = drawable.toBitmap()
                val owns = drawable !is BitmapDrawable || drawable.bitmap !== bitmap
                currentCoroutineContext().ensureActive()
                val processed = processBitmap(bitmap, seed, cacheFile, ownsOriginal = owns)
                val published = publishIfCurrent(generation, processed)
                recycleUnpublishedSuccess(processed, published, seed != 0 || owns)
            }

            is ErrorResult -> {
                // Coil 加载失败，尝试使用内置 API 的 imageFetcher 回退
                val fetchedBytes = try {
                    imageFetcher?.invoke()
                } catch (e: CancellationException) {
                    // 回退请求同样属于可取消工作，不能被当成网络失败吞掉。
                    throw e
                } catch (e: Exception) {
                    logError("ComicPicImage", "imageFetcher 调用失败: ${e.stackTraceToString()}")
                    null
                }
                if (fetchedBytes != null) {
                    try {
                        val originalBitmap = BitmapFactory
                            .decodeByteArray(fetchedBytes, 0, fetchedBytes.size)
                        if (originalBitmap != null) {
                            // 这张位图由本方解码，无人共享，可以回收
                            val processed = processBitmap(
                                originalBitmap,
                                seed,
                                cacheFile,
                                ownsOriginal = true
                            )
                            currentCoroutineContext().ensureActive()
                            val published = publishIfCurrent(generation, processed)
                            recycleUnpublishedSuccess(processed, published, ownsBitmap = true)
                            return
                        }
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: OutOfMemoryError) {
                        logError("ComicPicImage", "内置API图片解码内存不足: ${e.message}")
                        publishIfCurrent(generation, ImageResultState.Failure("内存不足"))
                        return
                    } catch (e: Exception) {
                        logError("ComicPicImage", "内置API图片解码失败: ${e.stackTraceToString()}")
                    }
                }
                currentCoroutineContext().ensureActive()
                logError("ComicPicImage", "图片加载失败: ${result.throwable.stackTraceToString()}")
                publishIfCurrent(generation, ImageResultState.Failure("网络错误"))
            }
        }
    }

    /**
     * 按分块数还原并写入缓存，seed 为 0 表示无需解扰。
     * [ownsOriginal]：来自 Coil 的位图归其内存缓存所有，回收后缓存再命中会拿到废图而崩溃。
     */
    private suspend fun processBitmap(
        originalBitmap: Bitmap,
        seed: Int,
        cacheFile: File,
        ownsOriginal: Boolean
    ): ImageResultState {
        var finalBitmap: Bitmap? = null
        var originalRecycled = false
        return try {
            val aspectRatio = originalBitmap.width * 1.0f / originalBitmap.height
            val processedBitmap = if (seed == 0) {
                originalBitmap
            } else {
                // 解扰产出同尺寸新位图，原图随即无人引用。
                // 但只有独占它时才能回收，否则会波及 Coil 的内存缓存。
                decodeBitmap(originalBitmap, seed).also {
                    if (ownsOriginal) {
                        originalBitmap.recycle()
                        originalRecycled = true
                    }
                }
            }
            finalBitmap = processedBitmap
            saveBitmapAsWebp(processedBitmap, cacheFile)
            ImageResultState.Success(processedBitmap.asImageBitmap(), aspectRatio)
        } catch (e: CancellationException) {
            cacheFile.delete()
            recycleOwnedBitmaps(
                originalBitmap = originalBitmap,
                ownsOriginal = ownsOriginal,
                finalBitmap = finalBitmap,
                originalRecycled = originalRecycled
            )
            throw e
        } catch (e: OutOfMemoryError) {
            cacheFile.delete()
            recycleOwnedBitmaps(
                originalBitmap = originalBitmap,
                ownsOriginal = ownsOriginal,
                finalBitmap = finalBitmap,
                originalRecycled = originalRecycled
            )
            logError("ComicPicImage", "图片处理内存不足: ${e.message}")
            ImageResultState.Failure("内存不足")
        } catch (e: Exception) {
            cacheFile.delete()
            recycleOwnedBitmaps(
                originalBitmap = originalBitmap,
                ownsOriginal = ownsOriginal,
                finalBitmap = finalBitmap,
                originalRecycled = originalRecycled
            )
            logError("ComicPicImage", "图片处理失败: ${e.stackTraceToString()}")
            ImageResultState.Failure("图片处理失败：${e.message ?: "未知错误"}")
        }
    }

    private fun recycleUnpublishedSuccess(
        state: ImageResultState,
        published: Boolean,
        ownsBitmap: Boolean
    ) {
        if (published || !ownsBitmap || state !is ImageResultState.Success) return
        state.decodeImageBitmap.asAndroidBitmap().let { bitmap ->
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private fun recycleOwnedBitmaps(
        originalBitmap: Bitmap,
        ownsOriginal: Boolean,
        finalBitmap: Bitmap?,
        originalRecycled: Boolean
    ) {
        if (
            finalBitmap != null &&
            (finalBitmap !== originalBitmap || ownsOriginal) &&
            !finalBitmap.isRecycled
        ) {
            finalBitmap.recycle()
        }
        if (
            ownsOriginal &&
            !originalRecycled &&
            finalBitmap !== originalBitmap &&
            !originalBitmap.isRecycled
        ) {
            originalBitmap.recycle()
        }
    }

    private suspend fun decodeBitmap(originalBitmap: Bitmap, seed: Int): Bitmap {
        val naturalWidth = originalBitmap.width
        val naturalHeight = originalBitmap.height
        val remainder = naturalHeight % seed

        val decodedBitmap = createBitmap(naturalWidth, naturalHeight)
        var completed = false
        try {
            val canvas = Canvas(decodedBitmap.asImageBitmap())
            val paint = Paint().apply {
                this.isAntiAlias = false
            }
            val originImageBitmap = originalBitmap.asImageBitmap()

            for (i in 0 until seed) {
                currentCoroutineContext().ensureActive()
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

            completed = true
            return decodedBitmap
        } finally {
            if (!completed && !decodedBitmap.isRecycled) decodedBitmap.recycle()
        }
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
