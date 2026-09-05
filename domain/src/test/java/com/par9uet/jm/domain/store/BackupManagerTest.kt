package com.par9uet.jm.domain.store

import com.par9uet.jm.core.model.LocalSetting
import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupManagerTest {
    @Test
    fun encryptedBackupRoundTripsWithPassword() {
        val manager = BackupManager()
        val json = manager.createBackup(
            localSetting = LocalSetting(),
            options = BackupContentOptions(includeLocalSetting = true),
            protectionType = BACKUP_PROTECTION_PASSWORD,
            password = "1234",
        )
        assertFalse(json.contains("appLockPassword"))
        val header = manager.parseBackup(json).getOrThrow()
        val decrypted = manager.decryptBackup(header, password = "1234")
        assertEquals(LocalSetting(), manager.extractLocalSetting(decrypted))
    }
}
