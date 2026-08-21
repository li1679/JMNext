package com.par9uet.jm.ui.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Book
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Circle
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.par9uet.jm.core.model.APP_LOCK_TYPE_PASSWORD
import com.par9uet.jm.core.model.APP_LOCK_TYPE_PATTERN
import com.par9uet.jm.core.model.Comic
import com.par9uet.jm.core.model.ComicChapter
import com.par9uet.jm.data.database.dao.DownloadComicDao
import com.par9uet.jm.domain.store.BACKUP_PROTECTION_BOTH
import com.par9uet.jm.domain.store.BACKUP_PROTECTION_NONE
import com.par9uet.jm.domain.store.BACKUP_PROTECTION_PASSWORD
import com.par9uet.jm.domain.store.BACKUP_PROTECTION_PATTERN
import com.par9uet.jm.domain.store.BackupContentOptions
import com.par9uet.jm.domain.store.BackupFile
import com.par9uet.jm.domain.store.BackupManager
import com.par9uet.jm.domain.store.ComicCacheBackup
import com.par9uet.jm.domain.store.ComicGroupBackup
import com.par9uet.jm.domain.store.DownloadManager
import com.par9uet.jm.data.storage.LocalSettingManager
import com.par9uet.jm.domain.store.RemoteSettingManager
import com.par9uet.jm.core.common.ToastManager
import com.par9uet.jm.ui.component.CommonScaffold
import com.par9uet.jm.core.designsystem.component.SelectDialog
import com.par9uet.jm.core.designsystem.component.SelectOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.koin.compose.getKoin
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class BackupStep {
    None, SelectContent, SelectProtection, SetPassword, SetPattern
}

private enum class RestoreStep {
    None, VerifyPassword, VerifyPattern, SelectContent, SelectComicCache
}

private val protectionOptionList = listOf(
    SelectOption("无保护", BACKUP_PROTECTION_NONE),
    SelectOption("仅密码", BACKUP_PROTECTION_PASSWORD),
    SelectOption("仅图案", BACKUP_PROTECTION_PATTERN),
    SelectOption("密码 + 图案", BACKUP_PROTECTION_BOTH),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupRestoreScreen(
    localSettingManager: LocalSettingManager = getKoin().get(),
    downloadComicDao: DownloadComicDao = getKoin().get(),
    downloadManager: DownloadManager = getKoin().get(),
    remoteSettingManager: RemoteSettingManager = getKoin().get(),
    toastManager: ToastManager = getKoin().get(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val localSetting by localSettingManager.localSettingState.collectAsStateWithLifecycle()
    val remoteSetting by remoteSettingManager.remoteSettingState.collectAsStateWithLifecycle()
    val backupManager = remember { BackupManager() }

    var backupStep by remember { mutableStateOf(BackupStep.None) }
    var contentOptions by remember { mutableStateOf(BackupContentOptions()) }
    var pendingProtectionType by remember { mutableStateOf(BACKUP_PROTECTION_NONE) }
    var pendingPassword by remember { mutableStateOf<String?>(null) }
    var pendingPattern by remember { mutableStateOf<String?>(null) }
    var pendingCreateDocument by remember { mutableStateOf(false) }
    // 缓存备份在内存中暂存，等待写入文件时一起打包
    var pendingComicCacheBackup by remember { mutableStateOf<ComicCacheBackup?>(null) }

    var restoreBackup by remember { mutableStateOf<BackupFile?>(null) }
    var restoreStep by remember { mutableStateOf(RestoreStep.None) }
    // 恢复时用户选择的内容选项
    var restoreContentOptions by remember { mutableStateOf(BackupContentOptions()) }

    fun resetBackupState() {
        backupStep = BackupStep.None
        contentOptions = BackupContentOptions()
        pendingProtectionType = BACKUP_PROTECTION_NONE
        pendingPassword = null
        pendingPattern = null
        pendingComicCacheBackup = null
    }

    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri == null) {
            toastManager.showAsync("未选择保存位置")
            resetBackupState()
            return@rememberLauncherForActivityResult
        }
        val pwd = pendingPassword
        val pat = pendingPattern
        val prot = pendingProtectionType
        val opts = contentOptions
        val cacheBackup = pendingComicCacheBackup
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val json = backupManager.createBackup(
                        localSetting = if (opts.includeLocalSetting) localSetting else null,
                        comicCache = if (opts.includeComicCache) cacheBackup else null,
                        options = opts,
                        protectionType = prot,
                        password = pwd,
                        pattern = pat
                    )
                    if (!backupManager.writeToUri(context, uri, json)) {
                        error("写入备份文件失败")
                    }
                }
            }.onSuccess {
                toastManager.showAsync("备份成功")
            }.onFailure {
                toastManager.showAsync("备份失败：${it.message ?: "未知错误"}")
            }
            resetBackupState()
        }
    }

    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri == null) {
            toastManager.showAsync("未选择备份文件")
            return@rememberLauncherForActivityResult
        }
        scope.launch {
            runCatching {
                withContext(Dispatchers.IO) {
                    val json = backupManager.readFromUri(context, uri)
                        ?: error("无法读取备份文件")
                    backupManager.parseBackup(json).getOrThrow()
                }
            }.onSuccess { backup ->
                restoreBackup = backup
                restoreStep = when {
                    backupManager.needsPassword(backup) -> RestoreStep.VerifyPassword
                    backupManager.needsPattern(backup) -> RestoreStep.VerifyPattern
                    else -> RestoreStep.SelectContent
                }
            }.onFailure {
                toastManager.showAsync("恢复失败：${it.message ?: "未知错误"}")
            }
        }
    }

    fun onPasswordVerified() {
        val backup = restoreBackup ?: return
        restoreStep = if (backupManager.needsPattern(backup)) {
            RestoreStep.VerifyPattern
        } else {
            RestoreStep.SelectContent
        }
    }

    fun onPatternVerified() {
        restoreStep = RestoreStep.SelectContent
    }

    fun applyRestore(backup: BackupFile, options: BackupContentOptions) {
        runCatching {
            val applied = mutableListOf<String>()
            if (options.includeLocalSetting) {
                val setting = backupManager.extractLocalSetting(backup)
                if (setting != null) {
                    localSettingManager.applyLocalSetting(setting)
                    applied += "本地设置"
                }
            }
            // 缓存目录在 applyComicCacheRestore 中单独处理，这里不处理
            if (applied.isEmpty()) "未找到可恢复的内容" else "已恢复：${applied.joinToString("、")}"
        }.onSuccess { msg ->
            toastManager.showAsync(msg)
        }.onFailure {
            toastManager.showAsync("恢复失败：${it.message ?: "未知错误"}")
        }
        restoreBackup = null
        restoreStep = RestoreStep.None
    }

    /**
     * 恢复缓存目录：将用户选中的漫画组重新加入下载队列。
     */
    fun applyComicCacheRestore(selectedGroups: List<ComicGroupBackup>) {
        if (selectedGroups.isEmpty()) {
            toastManager.showAsync("未选择需要恢复缓存的漫画")
            restoreBackup = null
            restoreStep = RestoreStep.None
            return
        }
        scope.launch {
            var totalChapters = 0
            selectedGroups.forEach { group ->
                val parentComic = Comic.create(
                    id = group.id,
                    name = group.name,
                    authorList = group.authors,
                )
                val chapters = group.chapters.sortedBy { it.sortOrder }
                    .map { ChapterBackup_to_ComicChapter(it) }
                if (chapters.size == 1 && chapters.first().name.isBlank()) {
                    // 单篇漫画：走 downloadComic
                    downloadManager.downloadComic(parentComic)
                } else {
                    downloadManager.downloadChapters(parentComic, chapters)
                }
                totalChapters += chapters.size
            }
            toastManager.showAsync("已创建 ${selectedGroups.size} 部漫画的缓存任务（共 $totalChapters 章）")
            restoreBackup = null
            restoreStep = RestoreStep.None
        }
    }

    fun cancelRestore() {
        restoreBackup = null
        restoreStep = RestoreStep.None
        toastManager.showAsync("已取消恢复")
    }

    LaunchedEffect(pendingCreateDocument) {
        if (pendingCreateDocument) {
            pendingCreateDocument = false
            createDocumentLauncher.launch(generateBackupFileName())
        }
    }

    CommonScaffold(title = "数据备份与恢复") {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item { InfoCard() }
            item {
                ActionCard(
                    icon = Icons.Rounded.CloudUpload,
                    title = "备份数据",
                    description = "选择需要备份的内容（本地设置 / 缓存目录），再选择是否设置密码/图案保护",
                    onClick = {
                        resetBackupState()
                        backupStep = BackupStep.SelectContent
                    }
                )
            }
            item {
                ActionCard(
                    icon = Icons.Rounded.CloudDownload,
                    title = "恢复数据",
                    description = "从备份文件恢复，可选择需要恢复的内容（不会覆盖当前设备的应用锁状态）",
                    onClick = {
                        openDocumentLauncher.launch(arrayOf("application/json"))
                    }
                )
            }
        }


        // 步骤 1：选择备份内容
        if (backupStep == BackupStep.SelectContent) {
            BackupContentPickerDialog(
                options = contentOptions,
                onChange = { contentOptions = it },
                onConfirm = {
                    if (contentOptions.isEmpty) {
                        toastManager.showAsync("请至少选择一项备份内容")
                    } else {
                        // 如果选中了缓存目录，先异步读取 DAO 数据
                        if (contentOptions.includeComicCache) {
                            scope.launch {
                                runCatching {
                                    withContext(Dispatchers.IO) {
                                        val all = downloadComicDao.getAll()
                                        backupManager.buildComicCacheBackup(all)
                                    }
                                }.onSuccess { cache ->
                                    if (cache.groups.isEmpty()) {
                                        toastManager.showAsync("当前没有缓存记录，已自动取消勾选缓存目录")
                                        contentOptions = contentOptions.copy(includeComicCache = false)
                                    } else {
                                        pendingComicCacheBackup = cache
                                    }
                                }.onFailure {
                                    toastManager.showAsync("读取缓存列表失败：${it.message ?: "未知错误"}")
                                    contentOptions = contentOptions.copy(includeComicCache = false)
                                }
                                backupStep = BackupStep.SelectProtection
                            }
                        } else {
                            backupStep = BackupStep.SelectProtection
                        }
                    }
                },
                onDismiss = { resetBackupState() }
            )
        }

        // 步骤 2：选择保护方式
        if (backupStep == BackupStep.SelectProtection) {
            SelectDialog(
                title = "选择保护方式",
                value = null,
                selectOptionList = protectionOptionList,
                onSelect = { type ->
                    pendingProtectionType = type
                    when (type) {
                        BACKUP_PROTECTION_NONE -> {
                            pendingCreateDocument = true
                            backupStep = BackupStep.None
                        }
                        BACKUP_PROTECTION_PASSWORD -> {
                            backupStep = BackupStep.SetPassword
                        }
                        BACKUP_PROTECTION_PATTERN -> {
                            backupStep = BackupStep.SetPattern
                        }
                        BACKUP_PROTECTION_BOTH -> {
                            backupStep = BackupStep.SetPassword
                        }
                    }
                },
                onDismissRequest = { resetBackupState() }
            )
        }

        // 步骤 3a：设置密码
        if (backupStep == BackupStep.SetPassword) {
            SetAppLockPasswordDialog(
                lockType = APP_LOCK_TYPE_PASSWORD,
                passwordLength = 4,
                onConfirm = { pwd ->
                    pendingPassword = pwd
                    backupStep = if (pendingProtectionType == BACKUP_PROTECTION_BOTH) {
                        BackupStep.SetPattern
                    } else {
                        pendingCreateDocument = true
                        BackupStep.None
                    }
                },
                onDismiss = { resetBackupState() }
            )
        }

        // 步骤 3b：设置图案
        if (backupStep == BackupStep.SetPattern) {
            SetAppLockPasswordDialog(
                lockType = APP_LOCK_TYPE_PATTERN,
                onConfirm = { pattern ->
                    pendingPattern = pattern
                    pendingCreateDocument = true
                    backupStep = BackupStep.None
                },
                onDismiss = { resetBackupState() }
            )
        }


        if (restoreStep == RestoreStep.VerifyPassword) {
            val backup = restoreBackup
            if (backup != null) {
                VerifyPasswordDialog(
                    passwordLength = 4,
                    onVerify = { pwd ->
                        if (backupManager.verifyPassword(backup, pwd)) {
                            onPasswordVerified()
                            true
                        } else {
                            false
                        }
                    },
                    onDismiss = { cancelRestore() }
                )
            }
        }

        if (restoreStep == RestoreStep.VerifyPattern) {
            val backup = restoreBackup
            if (backup != null) {
                VerifyPatternDialog(
                    onVerify = { pattern ->
                        if (backupManager.verifyPattern(backup, pattern)) {
                            onPatternVerified()
                            true
                        } else {
                            false
                        }
                    },
                    onDismiss = { cancelRestore() }
                )
            }
        }

        // 选择恢复内容
        if (restoreStep == RestoreStep.SelectContent) {
            val backup = restoreBackup
            if (backup != null) {
                RestoreContentPickerDialog(
                    backup = backup,
                    onConfirm = { options ->
                        restoreContentOptions = options
                        // 如果包含缓存目录，先进入漫画选择步骤
                        if (options.includeComicCache && backup.meta.includeComicCache) {
                            restoreStep = RestoreStep.SelectComicCache
                        } else {
                            applyRestore(backup, options)
                        }
                    },
                    onDismiss = { cancelRestore() }
                )
            }
        }

        // 选择要恢复缓存的漫画
        if (restoreStep == RestoreStep.SelectComicCache) {
            val backup = restoreBackup
            if (backup != null) {
                val cache = backupManager.extractComicCache(backup)
                ComicCacheRestoreDialog(
                    groups = cache.groups,
                    imgHost = remoteSetting.imgHost,
                    onConfirm = { selected ->
                        // 先应用其他选项（localSetting），再恢复缓存
                        val opts = restoreContentOptions
                        if (opts.includeLocalSetting) {
                            applyRestore(backup, opts.copy(includeComicCache = false))
                        } else {
                            restoreBackup = null
                            restoreStep = RestoreStep.None
                        }
                        applyComicCacheRestore(selected)
                    },
                    onSkip = {
                        // 用户选择不恢复缓存，只恢复其他内容
                        applyRestore(backup, restoreContentOptions.copy(includeComicCache = false))
                    },
                    onDismiss = { cancelRestore() }
                )
            }
        }
    }
}

private fun generateBackupFileName(): String {
    val sdf = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.CHINESE)
    return "jm-mobile-backup-${sdf.format(Date())}.json"
}

