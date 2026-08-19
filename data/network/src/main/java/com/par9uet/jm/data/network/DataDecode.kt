package com.par9uet.jm.data.network

import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonObject
import com.google.gson.Strictness
import com.par9uet.jm.core.common.DEFAULT_SCRAMBLE_ID
import java.nio.charset.Charset
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

// TODO 改为 koin 注入，改为类，并实现 koinComponent
private val g: Gson = Gson().newBuilder().setStrictness(Strictness.LENIENT).create()

// 正则置于顶层：这些函数在每次拉取图片列表时都会调用，避免重复编译
private val RESULT_REGEX = Regex("""const result\s*=\s*(\{[\s\S]*?\});""")
private val CONFIG_REGEX = Regex("""const config\s*=\s*(\{[\s\S]*?\});""")
private val AID_REGEX = Regex("""var aid\s*=\s*(\d+);""")
private val SCRAMBLE_ID_REGEX = Regex("""var scramble_id\s*=\s*(\d+);""")
private val SPEED_REGEX = Regex("""var speed\s*=\s*'(.*)';""")

fun parseHtml(htmlStr: String): List<String> {
    val resultMatch = RESULT_REGEX.find(htmlStr)
    val originPicList = mutableListOf<String>()

    if (resultMatch != null) {
        try {
            val resultJson = resultMatch.groupValues[1]
            val o = g.fromJson(
                resultJson,
                JsonObject::class.java
            )
            val list = o.get("images").asJsonArray
            if (list != null) {
                for (i in 0 until list.size()) {
                    list.get(i).let {
                        if (it.isJsonPrimitive) {
                            originPicList.add(it.asString)
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d("api", "Error parsing result object: ${e.stackTraceToString()}")
        }
    }

    val configMatch = CONFIG_REGEX.find(htmlStr)
    var imgHost: String? = null
    var jmId: String? = null
    var cache: String? = null

    if (configMatch != null) {
        try {
            val resultJson = configMatch.groupValues[1]
            val o = g.fromJson(
                resultJson,
                JsonObject::class.java
            )
            imgHost = o.get("imghost").asString
            jmId = o.get("jmid").asString
            cache = o.get("cache").asString
        } catch (e: Exception) {
            Log.d("api", "Error parsing config object: ${e.stackTraceToString()}")
        }
    }

    if (originPicList.isEmpty() || imgHost == null || jmId == null || cache == null) {
        Log.d("api", "解析漫画 html 页失败")
        return listOf()
    }

    return originPicList.toList().map { item ->
        "$imgHost/media/photos/$jmId/$item$cache"
    }
}

fun parseRange(htmlStr: String): Pair<Int, Int> {
    var left = 0
    // scramble_id 若退化成 0，所有老本子都会被判定为「需要解扰」而出现错版，
    // 因此解析不到时回退到禁漫的默认值而非 0
    var right = DEFAULT_SCRAMBLE_ID
    val rs1 = AID_REGEX.find(htmlStr)
    if (rs1 != null) {
        try {
            val str = rs1.groupValues[1]
            left = str.toInt()
        } catch (e: Exception) {
            Log.d("parse", "Error parse range, result object: ${e.stackTraceToString()}")
        }
    }

    val rs2 = SCRAMBLE_ID_REGEX.find(htmlStr)
    if (rs2 != null) {
        try {
            val str = rs2.groupValues[1]
            right = str.toInt()
        } catch (e: Exception) {
            Log.d("parse", "Error parse range, result object: ${e.stackTraceToString()}")
        }
    } else {
        Log.d("parse", "未解析到 scramble_id，回退到默认值 $DEFAULT_SCRAMBLE_ID")
    }
    return left to right
}

fun parseSpeed(htmlStr: String): String {
    var speed = ""
    val rs1 = SPEED_REGEX.find(htmlStr)
    if (rs1 != null) {
        try {
            speed = rs1.groupValues[1]
        } catch (e: Exception) {
            Log.d("parse", "Error parse speed, result object: ${e.stackTraceToString()}")
        }
    }
    return speed
}

fun decryptData(str: String): String {
    val decryptKey = ApiContext.getDataDecryptKey()
    val secretKey = SecretKeySpec(decryptKey.toByteArray(Charset.forName("UTF-8")), "AES")

    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    cipher.init(Cipher.DECRYPT_MODE, secretKey)

    val encryptedBytes = android.util.Base64.decode(str, android.util.Base64.DEFAULT)
    val decryptedBytes = cipher.doFinal(encryptedBytes)

    return String(decryptedBytes, Charset.forName("UTF-8"))
}