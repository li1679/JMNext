package com.par9uet.jm.data.repository.impl

import com.google.gson.JsonParser
import com.par9uet.jm.core.common.InitManager
import com.par9uet.jm.data.network.model.NetWorkResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateRepositoryImplTest {
    private val repository = UpdateRepositoryImpl(InitManager())

    @Test
    fun parsesReleaseAndPrefersMatchingJmnextApk() {
        val result = repository.parseRelease(JsonParser.parseString("""
            {
              "tag_name": "v1.1.0",
              "name": "JMNext 1.1.0",
              "html_url": "https://github.com/li1679/JMNext/releases/tag/v1.1.0",
              "body": "notes",
              "assets": [
                {"name": "other.apk", "browser_download_url": "https://example.test/other.apk"},
                {"name": "jmnext_v1.1.0_debug.apk", "browser_download_url": "https://example.test/debug.apk"},
                {"name": "jmnext_v1.1.0_release.apk", "browser_download_url": "https://example.test/release.apk"}
              ]
            }
        """).asJsonObject)

        assertTrue(result is NetWorkResult.Success)
        val release = (result as NetWorkResult.Success).data
        assertEquals("1.1.0", release.version)
        assertEquals("jmnext_v1.1.0_release.apk", release.fileName)
        assertEquals("https://example.test/release.apk", release.downloadUrl)
    }

    @Test
    fun rejectsReleaseWithoutVersion() {
        val result = repository.parseRelease(JsonParser.parseString("""
            {"tag_name":"", "name":"", "assets":[]}
        """).asJsonObject)

        assertTrue(result is NetWorkResult.Error)
    }

    @Test
    fun parsesApkLinksFromGitHubExpandedAssetsPage() {
        val assets = repository.parseWebApkAssets(
            """
                <a href="/li1679/JMNext/releases/download/v1.1.0/jmnext_v1.1.0_abc.apk">
                    jmnext_v1.1.0_abc.apk
                </a>
            """.trimIndent(),
        )

        assertEquals(1, assets.size)
        assertEquals("jmnext_v1.1.0_abc.apk", assets.single().name)
        assertEquals(
            "https://github.com/li1679/JMNext/releases/download/v1.1.0/jmnext_v1.1.0_abc.apk",
            assets.single().downloadUrl,
        )
    }
}
