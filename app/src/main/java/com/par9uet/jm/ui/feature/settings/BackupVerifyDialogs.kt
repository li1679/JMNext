package com.par9uet.jm.ui.feature.settings

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** 恢复备份前的密码 / 图案校验对话框。 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VerifyPasswordDialog(
    passwordLength: Int,
    onVerify: (String) -> Boolean,
    onDismiss: () -> Unit,
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.widthIn(max = 400.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "请输入备份密码",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                PasswordLockInput(
                    title = "",
                    correctPassword = null,
                    onUnlock = {},
                    passwordLength = passwordLength,
                    onInputComplete = { pwd ->
                        if (!onVerify(pwd)) {
                            errorMessage = "密码错误，请重试"
                        } else {
                            errorMessage = null
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun VerifyPatternDialog(
    onVerify: (String) -> Boolean,
    onDismiss: () -> Unit,
) {
    var errorMessage by remember { mutableStateOf<String?>(null) }

    BasicAlertDialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.widthIn(max = 400.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
            ),
            shape = MaterialTheme.shapes.extraLarge
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "请绘制备份图案",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
                errorMessage?.let {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
                PatternLockInput(
                    title = "",
                    correctPassword = null,
                    onUnlock = {},
                    onInputComplete = { pattern ->
                        if (!onVerify(pattern)) {
                            errorMessage = "图案错误，请重试"
                        } else {
                            errorMessage = null
                        }
                    }
                )
                Spacer(modifier = Modifier.height(16.dp))
                TextButton(onClick = onDismiss) {
                    Text("取消")
                }
            }
        }
    }
}

