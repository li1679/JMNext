package com.par9uet.jm.domain.store

import android.content.Context
import android.net.Uri
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import java.util.Base64
import com.par9uet.jm.core.model.LocalSetting
import com.par9uet.jm.core.common.logError
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.AEADBadTagException

const val BACKUP_PROTECTION_NONE = "none"
const val BACKUP_PROTECTION_PASSWORD = "password"
const val BACKUP_PROTECTION_PATTERN = "pattern"
const val BACKUP_PROTECTION_BOTH = "both"

const val BACKUP_FORMAT_VERSION = 2

/** 用户选择要备份的内容类型 */
data class BackupContentOptions(
    val includeLocalSetting: Boolean = true,
    val includeComicCache: Boolean = false,
) {
    val isEmpty: Boolean get() = !includeLocalSetting && !includeComicCache
}

/** 备份元信息：版本、时间戳、保护方式与内容标记 */
data class BackupMeta(
    val version: Int = BACKUP_FORMAT_VERSION,
    val timestamp: Long = System.currentTimeMillis(),
    val protectionType: String = BACKUP_PROTECTION_NONE,
    val includeLocalSetting: Boolean = true,
    val includeComicCache: Boolean = false,
    val comicCacheCount: Int = 0,
    val encryptionSalt: String? = null,
)

/** 缓存目录备份：单章信息，不含图片文件 */
data class ChapterBackup(
    val id: Int,
    val name: String,
    val sortOrder: Long,
)

/** 缓存目录备份：一组漫画。单篇漫画的 chapters 只有一个元素（id 与 groupId 相同） */
data class ComicGroupBackup(
    val id: Int,
    val name: String,
    val authors: List<String>,
    val tags: List<String>,
    val chapters: List<ChapterBackup>,
) {
    val chapterCount: Int get() = chapters.size
}

/**
 * 缓存目录备份整体结构。
 */
data class ComicCacheBackup(
    val groups: List<ComicGroupBackup> = emptyList(),
)

/**
 * 新应用备份文件结构：元数据公开，内容使用密码/图案派生的 AES-GCM 密钥加密。
 */
data class BackupFile(
    val meta: BackupMeta,
    val data: JsonObject,
    val encryptedData: String? = null,
)

class BackupManager {
    private val random = SecureRandom()
    private val gson: Gson = GsonBuilder()
        .disableHtmlEscaping()
        .setPrettyPrinting()
        .create()

    /**
     * 创建备份 JSON 字符串。
     */
    fun createBackup(
        localSetting: LocalSetting?,
        comicCache: ComicCacheBackup? = null,
        options: BackupContentOptions,
        protectionType: String = BACKUP_PROTECTION_NONE,
        password: String? = null,
        pattern: String? = null,
    ): String {
        require(!options.isEmpty) { "至少需要选择一项备份内容" }
        require(protectionType != BACKUP_PROTECTION_NONE) { "新备份必须设置密码或图案保护" }
        val secret = protectionSecret(protectionType, password, pattern)
        if (protectionType != BACKUP_PROTECTION_PATTERN) {
            require(requireNotNull(password).length >= 8) { "新备份密码至少需要 8 个字符" }
        }
        val salt = ByteArray(16).also(random::nextBytes)
        val meta = BackupMeta(
            version = BACKUP_FORMAT_VERSION,
            timestamp = System.currentTimeMillis(),
            protectionType = protectionType,
            includeLocalSetting = options.includeLocalSetting,
            includeComicCache = options.includeComicCache && comicCache != null,
            comicCacheCount = comicCache?.groups?.size ?: 0,
            encryptionSalt = Base64.getEncoder().withoutPadding().encodeToString(salt),
        )

        val data = JsonObject()
        if (options.includeLocalSetting && localSetting != null) {
            val sanitized = localSetting.copy(
                appLockPassword = "",
                appLockPattern = "",
            )
            data.add("localSetting", gson.toJsonTree(sanitized))
        }
        if (options.includeComicCache && comicCache != null) {
            data.add("comicCache", gson.toJsonTree(comicCache))
        }

        val encryptedData = encryptData(gson.toJson(data), secret, salt)
        val backup = BackupFile(meta = meta, data = JsonObject(), encryptedData = encryptedData)
        return gson.toJson(backup)
    }

    /**
     * 解析新应用备份元数据；内容在凭据校验后解密。
     */
    fun parseBackup(json: String): Result<BackupFile> = runCatching {
        val obj = JsonParser.parseString(json).asJsonObject
        // Version 1 hashes are intentionally ignored: the encrypted payload authenticates credentials.
        val meta = gson.fromJson(obj.getAsJsonObject("meta"), BackupMeta::class.java)
            ?: error("备份文件缺少 meta 字段")
        require(meta.version in 1..BACKUP_FORMAT_VERSION) { "不支持的备份版本：${meta.version}" }
        require(meta.protectionType in setOf(BACKUP_PROTECTION_PASSWORD, BACKUP_PROTECTION_PATTERN, BACKUP_PROTECTION_BOTH)) {
            "备份保护方式无效"
        }
        val encryptedData = obj.get("encryptedData")?.takeUnless { it.isJsonNull }?.asString
            ?: error("备份文件不是新加密格式")
        BackupFile(meta = meta, data = JsonObject(), encryptedData = encryptedData)
    }

    fun decryptBackup(backup: BackupFile, password: String? = null, pattern: String? = null): BackupFile {
        require(backup.meta.version in 1..BACKUP_FORMAT_VERSION) { "不支持的备份版本：${backup.meta.version}" }
        val encrypted = backup.encryptedData ?: error("备份文件缺少加密数据")
        val salt = backup.meta.encryptionSalt?.let { decodeBase64(it) }
            ?: error("备份文件缺少加密参数")
        require(salt.size == 16) { "备份文件加密参数无效" }
        val secret = protectionSecret(backup.meta.protectionType, password, pattern)
        val json = try {
            decryptData(encrypted, secret, salt)
        } catch (error: AEADBadTagException) {
            throw IllegalArgumentException("密码或图案错误，或备份文件已损坏", error)
        }
        return backup.copy(data = JsonParser.parseString(json).asJsonObject)
    }

    private fun protectionSecret(type: String, password: String?, pattern: String?): String {
        fun credential(value: String?) = requireNotNull(value?.takeIf { it.isNotEmpty() }) {
            "备份保护凭据不能为空"
        }
        return when (type) {
            BACKUP_PROTECTION_PASSWORD -> credential(password)
            BACKUP_PROTECTION_PATTERN -> credential(pattern)
            BACKUP_PROTECTION_BOTH -> credential(password) + "\u0000" + credential(pattern)
            else -> error("备份保护方式无效")
        }
    }

    private fun deriveKey(secret: String, salt: ByteArray) =
        SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec(secret.toCharArray(), salt, 120_000, 256))
            .encoded

    private fun encryptData(data: String, secret: String, salt: ByteArray): String {
        val iv = ByteArray(12).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, javax.crypto.spec.SecretKeySpec(deriveKey(secret, salt), "AES"), GCMParameterSpec(128, iv))
        return Base64.getEncoder().withoutPadding().encodeToString(iv + cipher.doFinal(data.toByteArray(Charsets.UTF_8)))
    }

    private fun decryptData(value: String, secret: String, salt: ByteArray): String {
        val bytes = decodeBase64(value)
        require(bytes.size >= 28) { "备份文件加密数据无效" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, javax.crypto.spec.SecretKeySpec(deriveKey(secret, salt), "AES"), GCMParameterSpec(128, bytes.copyOfRange(0, 12)))
        return cipher.doFinal(bytes.copyOfRange(12, bytes.size)).toString(Charsets.UTF_8)
    }

    private fun decodeBase64(value: String): ByteArray = try {
        Base64.getDecoder().decode(value)
    } catch (error: IllegalArgumentException) {
        throw IllegalArgumentException("备份文件加密数据格式无效", error)
    }

    /**
     * 从已解密的新应用备份中提取 [LocalSetting]。
     */
    fun extractLocalSetting(backup: BackupFile): LocalSetting? {
        val obj = backup.data.getAsJsonObject("localSetting")
        if (obj != null) return gson.fromJson(obj, LocalSetting::class.java)
        return null
    }

    /**
     * 从已解密的新应用备份中提取缓存目录信息。
     */
    fun extractComicCache(backup: BackupFile): ComicCacheBackup {
        val obj = backup.data.getAsJsonObject("comicCache") ?: return ComicCacheBackup()
        return runCatching {
            gson.fromJson(obj, ComicCacheBackup::class.java) ?: ComicCacheBackup()
        }.getOrDefault(ComicCacheBackup())
    }

    fun needsPassword(backup: BackupFile): Boolean {
        return backup.meta.protectionType == BACKUP_PROTECTION_PASSWORD ||
            backup.meta.protectionType == BACKUP_PROTECTION_BOTH
    }

    fun needsPattern(backup: BackupFile): Boolean {
        return backup.meta.protectionType == BACKUP_PROTECTION_PATTERN ||
            backup.meta.protectionType == BACKUP_PROTECTION_BOTH
    }

    fun readFromUri(context: Context, uri: Uri): String? {
        return runCatching {
            context.contentResolver.openInputStream(uri)?.use { input ->
                input.readBytes().toString(Charsets.UTF_8)
            }
        }.getOrElse {
            logError("BackupManager", "读取备份文件失败: ${it.message}")
            null
        }
    }

    /**
     * 从 DownloadComicDao 的数据生成缓存目录备份。
     * 只备份漫画编号与章节信息，不备份图片文件本身。
     */
    suspend fun buildComicCacheBackup(
        allDownloads: List<com.par9uet.jm.data.database.model.DownloadComic>
    ): ComicCacheBackup {
        // 按 groupId 聚合（单篇漫画 groupId=0，以自身 id 作为组 ID）
        val grouped = allDownloads.groupBy { it.groupId.takeIf { g -> g != 0 } ?: it.id }
        val groups = grouped.map { (groupId, items) ->
            val first = items.first()
            val chapters = items
                .sortedBy { it.createTime }
                .map { item ->
                    ChapterBackup(
                        id = item.id,
                        name = item.chapterName,
                        sortOrder = item.createTime,
                    )
                }
            ComicGroupBackup(
                id = groupId,
                name = first.groupName.ifBlank { first.name },
                authors = first.authorList,
                tags = first.tagList,
                chapters = chapters,
            )
        }.sortedBy { it.id }
        return ComicCacheBackup(groups = groups)
    }

    fun writeToUri(context: Context, uri: Uri, content: String): Boolean {
        return runCatching {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(content.toByteArray(Charsets.UTF_8))
                true
            } ?: false
        }.getOrElse {
            logError("BackupManager", "写入备份文件失败: ${it.message}")
            false
        }
    }

}
