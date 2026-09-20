package com.par9uet.jm.ui.feature.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

@Composable
internal fun SetBackupPasswordDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit) {
    var password by remember { mutableStateOf("") }
    var confirmation by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("设置备份密码") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                BackupPasswordField(password, { password = it }, "密码（至少 8 个字符）")
                BackupPasswordField(confirmation, { confirmation = it }, "再次输入密码")
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(password) }, enabled = password.length >= 8 && password == confirmation) {
                Text("确定")
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun BackupPasswordField(value: String, onChange: (String) -> Unit, label: String, enabled: Boolean = true) {
    OutlinedTextField(
        value = value,
        onValueChange = onChange,
        modifier = Modifier.fillMaxWidth(),
        label = { Text(label) },
        enabled = enabled,
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
    )
}

/** 凭据仅存活于当前对话框，AES-GCM 认证成功后才进入恢复内容选择。 */
@Composable
internal fun VerifyBackupDialog(
    needsPassword: Boolean,
    needsPattern: Boolean,
    onVerify: suspend (String?, String?) -> Unit,
    onDismiss: () -> Unit,
) {
    var password by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf<String?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = { if (!busy) onDismiss() },
        title = { Text("验证备份") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                if (needsPassword) BackupPasswordField(password, { password = it }, "备份密码", !busy)
                if (needsPattern && !busy) {
                    PatternLockInput(
                        title = "请绘制备份图案",
                        correctPassword = null,
                        onUnlock = {},
                        onInputComplete = { pattern = it },
                    )
                    if (pattern != null) Text("图案已输入")
                }
                if (busy) CircularProgressIndicator()
                errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            }
        },
        confirmButton = {
            TextButton(
                enabled = !busy && (!needsPassword || password.isNotEmpty()) && (!needsPattern || pattern != null),
                onClick = {
                    busy = true
                    errorMessage = null
                    scope.launch {
                        try {
                            onVerify(password.takeIf { needsPassword }, pattern.takeIf { needsPattern })
                        } catch (cancelled: CancellationException) {
                            throw cancelled
                        } catch (error: Exception) {
                            errorMessage = error.message ?: "备份验证失败"
                        } finally {
                            busy = false
                        }
                    }
                },
            ) { Text("验证") }
        },
        dismissButton = { TextButton(onClick = onDismiss, enabled = !busy) { Text("取消") } },
    )
}
