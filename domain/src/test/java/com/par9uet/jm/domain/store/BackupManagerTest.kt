package com.par9uet.jm.domain.store

import com.par9uet.jm.core.model.LocalSetting
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.assertThrows
import com.google.gson.JsonParser
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import org.junit.Test

class BackupManagerTest {
    @Test
    fun encryptedBackupRoundTripsWithPassword() {
        val manager = BackupManager()
        val json = manager.createBackup(
            localSetting = LocalSetting(),
            options = BackupContentOptions(includeLocalSetting = true),
            protectionType = BACKUP_PROTECTION_PASSWORD,
            password = "test-password",
        )
        assertFalse(json.contains("appLockPassword"))
        val header = manager.parseBackup(json).getOrThrow()
        assertFalse(json.contains("passwordHash"))
        assertFalse(json.contains("patternHash"))
        assertEquals(2, header.meta.version)
        val decrypted = manager.decryptBackup(header, password = "test-password")
        assertEquals(LocalSetting(), manager.extractLocalSetting(decrypted))
    }

    @Test
    fun patternRestoreDoesNotUsePasswordFromPreviousRestore() {
        val manager = BackupManager()
        val backup = manager.parseBackup(manager.createBackup(
            LocalSetting(), options = BackupContentOptions(),
            protectionType = BACKUP_PROTECTION_PATTERN, pattern = "0,1,2,5",
        )).getOrThrow()
        assertEquals(LocalSetting(), manager.extractLocalSetting(
            manager.decryptBackup(backup, password = "stale-password", pattern = "0,1,2,5")
        ))
    }

    @Test
    fun combinedProtectionRequiresBothCredentialsAndRejectsCorruption() {
        val manager = BackupManager()
        val backup = manager.parseBackup(manager.createBackup(
            LocalSetting(), options = BackupContentOptions(),
            protectionType = BACKUP_PROTECTION_BOTH, password = "test-password", pattern = "0,1,2,5",
        )).getOrThrow()
        assertThrows(IllegalArgumentException::class.java) { manager.decryptBackup(backup, "test-password") }
        assertThrows(IllegalArgumentException::class.java) { manager.decryptBackup(backup, "wrong-password", "0,1,2,5") }
        val bytes = Base64.getDecoder().decode(backup.encryptedData)
        bytes[bytes.lastIndex] = (bytes.last().toInt() xor 1).toByte()
        val corrupt = backup.copy(encryptedData = Base64.getEncoder().encodeToString(bytes))
        val error = assertThrows(IllegalArgumentException::class.java) {
            manager.decryptBackup(corrupt, "test-password", "0,1,2,5")
        }
        assertTrue(error.message!!.contains("损坏"))
        assertEquals(LocalSetting(), manager.extractLocalSetting(manager.decryptBackup(backup, "test-password", "0,1,2,5")))
    }

    @Test
    fun legacyVersionOneWithFourDigitPasswordStillRestores() {
        val salt = ByteArray(16) { it.toByte() }
        val iv = ByteArray(12) { it.toByte() }
        val key = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            .generateSecret(PBEKeySpec("1234".toCharArray(), salt, 120_000, 256)).encoded
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val encrypted = Base64.getEncoder().encodeToString(iv + cipher.doFinal("{\"localSetting\":{}}".toByteArray()))
        val json = """{"meta":{"version":1,"protectionType":"password","passwordHash":"legacy-unused-hash","encryptionSalt":"${Base64.getEncoder().encodeToString(salt)}"},"encryptedData":"$encrypted"}"""
        val manager = BackupManager()
        val backup = manager.parseBackup(json).getOrThrow()
        assertEquals(LocalSetting(), manager.extractLocalSetting(manager.decryptBackup(backup, "1234")))
    }

    @Test
    fun unknownVersionAndWeakNewPasswordAreRejected() {
        val manager = BackupManager()
        assertThrows(IllegalArgumentException::class.java) {
            manager.createBackup(LocalSetting(), options = BackupContentOptions(), protectionType = BACKUP_PROTECTION_PASSWORD, password = "1234")
        }
        val json = JsonParser.parseString(manager.createBackup(
            LocalSetting(), options = BackupContentOptions(), protectionType = BACKUP_PROTECTION_PASSWORD, password = "test-password",
        )).asJsonObject
        json.getAsJsonObject("meta").addProperty("version", 999)
        assertTrue(manager.parseBackup(json.toString()).isFailure)
    }
}
