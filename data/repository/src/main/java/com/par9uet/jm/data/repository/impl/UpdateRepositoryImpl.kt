package com.par9uet.jm.data.repository.impl

import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.par9uet.jm.core.common.InitManager
import com.par9uet.jm.core.common.normalizeVersion
import com.par9uet.jm.core.model.GithubRelease
import com.par9uet.jm.data.network.model.NetWorkResult
import com.par9uet.jm.data.repository.BaseRepository
import com.par9uet.jm.data.repository.UpdateRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class UpdateRepositoryImpl(
    initManager: InitManager,
) : BaseRepository(initManager), UpdateRepository {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(12, TimeUnit.SECONDS)
        .callTimeout(20, TimeUnit.SECONDS)
        .build()

    override suspend fun getLatestRelease(): NetWorkResult<GithubRelease> = withContext(Dispatchers.IO) {
        try {
            val request = Request.Builder()
                .url(UpdateRepository.RELEASE_API)
                .header("Accept", "application/vnd.github+json")
                .header("User-Agent", "jmcomic-next-android")
                .build()
            client.newCall(request).execute().use { response ->
                if (response.code == 403) {
                    return@withContext getLatestReleaseFromWeb()
                }
                if (!response.isSuccessful) return@withContext NetWorkResult.Error("GitHub 返回 ${response.code}")
                val body = response.body?.string().orEmpty()
                if (body.isBlank()) return@withContext NetWorkResult.Error("GitHub 返回空响应")
                parseRelease(JsonParser.parseString(body).asJsonObject)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            NetWorkResult.Error(e.message ?: "检查更新失败")
        }
    }

    /** API 被匿名限流时，使用同一仓库的公开发布页读取版本和 APK 资产。 */
    private fun getLatestReleaseFromWeb(): NetWorkResult<GithubRelease> {
        val pageRequest = Request.Builder()
            .url(UpdateRepository.RELEASES_URL + "/latest")
            .header("User-Agent", "jmcomic-next-android")
            .build()
        return client.newCall(pageRequest).execute().use { pageResponse ->
            if (!pageResponse.isSuccessful) {
                return@use NetWorkResult.Error(
                    "GitHub API 返回 403，发布页也无法访问（HTTP ${pageResponse.code}）"
                )
            }
            val releaseUrl = pageResponse.request.url.toString()
            val tagName = pageResponse.request.url.pathSegments.lastOrNull().orEmpty()
            val version = normalizeVersion(tagName)
            if (version.isBlank()) return@use NetWorkResult.Error("GitHub 发布页未读取到版本号")

            val asset = getWebApkAsset(tagName, version)
            NetWorkResult.Success(
                GithubRelease(
                    version = version,
                    name = version,
                    url = releaseUrl,
                    body = "",
                    downloadUrl = asset?.downloadUrl.orEmpty(),
                    fileName = asset?.name.orEmpty(),
                )
            )
        }
    }

    private fun getWebApkAsset(tagName: String, version: String): ReleaseAsset? {
        val request = Request.Builder()
            .url("${UpdateRepository.RELEASES_URL}/expanded_assets/$tagName")
            .header("User-Agent", "jmcomic-next-android")
            .build()
        return client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) return@use null
            parseWebApkAssets(response.body?.string().orEmpty())
                .let { selectApkAsset(it, version) }
        }
    }

    internal fun parseRelease(json: JsonObject): NetWorkResult<GithubRelease> {
        val tagName = json.stringOrEmpty("tag_name")
        val name = json.stringOrEmpty("name")
        val version = normalizeVersion(tagName.ifBlank { name })
        if (version.isBlank()) return NetWorkResult.Error("未读取到 Release 版本号")
        val asset = selectApkAsset(json.getAsJsonArray("assets"), version)
        return NetWorkResult.Success(
            GithubRelease(
                version = version,
                name = name,
                url = json.stringOrEmpty("html_url").ifBlank { "${UpdateRepository.RELEASES_URL}/tag/$tagName" },
                body = json.stringOrEmpty("body"),
                downloadUrl = asset?.downloadUrl.orEmpty(),
                fileName = asset?.name.orEmpty(),
            )
        )
    }

    internal data class ReleaseAsset(val name: String, val downloadUrl: String)

    internal fun selectApkAsset(assets: JsonArray?, version: String): ReleaseAsset? {
        val apkAssets = assets?.mapNotNull { item ->
            val obj = item.asJsonObject
            val name = obj.stringOrEmpty("name")
            val url = obj.stringOrEmpty("browser_download_url")
            if (name.endsWith(".apk", ignoreCase = true) && url.isNotBlank()) ReleaseAsset(name, url) else null
        }.orEmpty()
        return selectApkAsset(apkAssets, version)
    }

    internal fun parseWebApkAssets(html: String): List<ReleaseAsset> {
        val assetPattern = Regex(
            """href="(/li1679/JMNext/releases/download/[^"]+/([^"/]+\.apk))"""",
            RegexOption.IGNORE_CASE,
        )
        return assetPattern.findAll(html).mapNotNull { match ->
            val path = match.groupValues[1]
            val encodedName = match.groupValues[2]
            val name = URLDecoder.decode(encodedName, StandardCharsets.UTF_8.name())
            ReleaseAsset(name, "https://github.com$path")
        }.toList()
    }

    private fun selectApkAsset(assets: List<ReleaseAsset>, version: String): ReleaseAsset? {
        return assets.firstOrNull { it.name.equals("jmnext_v${version}_release.apk", ignoreCase = true) }
            ?: assets.firstOrNull { it.name.equals("jmnext_v${version}_debug.apk", ignoreCase = true) }
            ?: assets.firstOrNull { it.name.startsWith("jmnext_v$version", ignoreCase = true) }
            ?: assets.firstOrNull()
    }

    private fun JsonObject.stringOrEmpty(key: String): String =
        get(key)?.takeIf { !it.isJsonNull }?.asString.orEmpty()
}
