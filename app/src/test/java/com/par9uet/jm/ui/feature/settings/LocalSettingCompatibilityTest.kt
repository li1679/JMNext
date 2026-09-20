package com.par9uet.jm.ui.feature.settings

import com.google.gson.Gson
import com.par9uet.jm.core.model.LocalSetting
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class LocalSettingCompatibilityTest {
    @Test
    fun oldGlobalExclusionsAreIgnoredWhileOtherSettingsSurvive() {
        val gson = Gson()
        val setting = gson.fromJson(
            """{"globalExcludedTags":["excluded"],"theme":"dark","readMode":"scroll","blockedTagTemplateList":[{"name":"search","tagList":["search-only"]}]}""",
            LocalSetting::class.java,
        )

        assertEquals("dark", setting.theme)
        assertEquals("scroll", setting.readMode)
        assertEquals(listOf("search-only"), setting.blockedTagTemplateList.single().tagList)
        assertFalse(gson.toJsonTree(setting).asJsonObject.has("globalExcludedTags"))
    }
}
