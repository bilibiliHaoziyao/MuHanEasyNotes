package com.muhan.notes.ui.backup

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.CloudDownload
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.material.icons.rounded.Save
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.muhan.notes.ui.components.AppIconButton
import java.io.File

/**
 * 备份与恢复：
 * - 本地备份（应用内备份 + SAF 导出到文件）
 * - 本地恢复（历史备份列表 + SAF 从文件恢复）
 * - WebDAV 云端备份 / 恢复
 */
@Composable
fun BackupScreen(
    webdavUrl: String,
    webdavUser: String,
    webdavPass: String,
    onSaveWebdav: (String, String, String) -> Unit,
    onExport: (onDone: (File?) -> Unit) -> Unit,
    onListLocal: () -> List<File>,
    onRestoreLocal: (File, (Int) -> Unit) -> Unit,
    onExportToUri: (Uri, (Boolean) -> Unit) -> Unit,
    onRestoreUri: (Uri, (Int) -> Unit) -> Unit,
    onWebdavUpload: (String, String, String, (String) -> Unit) -> Unit,
    onWebdavDownload: (String, String, String, (String) -> Unit) -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    var message by remember { mutableStateOf<String?>(null) }
    var backups by remember { mutableStateOf(onListLocal()) }

    var url by remember { mutableStateOf(webdavUrl) }
    var user by remember { mutableStateOf(webdavUser) }
    var pass by remember { mutableStateOf(webdavPass) }

    fun refresh() {
        backups = onListLocal()
    }

    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/zip")
    ) { uri ->
        uri?.let {
            onExportToUri(it) { ok ->
                message = if (ok) "已导出到文件" else "导出失败"
            }
        }
    }

    val openLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            onRestoreUri(it) { added ->
                message = "恢复完成，新增笔记 $added 条"
            }
        }
    }

    Scaffold {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
            item {
                Header(onBack = onBack)
            }
            message?.let { msg ->
                item {
                    Text(
                        text = msg,
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.primary,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    )
                }
            }

            item { SectionTitle("本地备份") }
            item {
                ActionChip(
                    icon = Icons.Rounded.Save,
                    label = "立即备份",
                    onClick = {
                        onExport { file ->
                            message = if (file != null) "备份成功：${file.name}" else "备份失败"
                            refresh()
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            item {
                ActionChip(
                    icon = Icons.Rounded.FileUpload,
                    label = "导出到文件…",
                    onClick = { exportLauncher.launch("muhan_notes_backup.zip") },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            item {
                ActionChip(
                    icon = Icons.Rounded.FileDownload,
                    label = "从文件恢复…",
                    onClick = { openLauncher.launch(arrayOf("application/zip", "application/octet-stream", "*/*")) },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            item { SectionTitle("历史备份") }
            if (backups.isEmpty()) {
                item {
                    Text(
                        text = "暂无历史备份",
                        style = MaterialTheme.typography.caption1,
                        color = MaterialTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp)
                    )
                }
            }
            items(backups, key = { it.absolutePath }) { file ->
                BackupRow(
                    file = file,
                    onRestore = {
                        onRestoreLocal(file) { added ->
                            message = "恢复完成，新增笔记 $added 条"
                        }
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            item { SectionTitle("WebDAV 云端") }
            item {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    FormField(
                        value = url,
                        onValueChange = { url = it },
                        placeholder = "WebDAV 地址（如 https://…/dav）",
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    FormField(
                        value = user,
                        onValueChange = { user = it },
                        placeholder = "账号",
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    FormField(
                        value = pass,
                        onValueChange = { pass = it },
                        placeholder = "密码",
                        modifier = Modifier.padding(bottom = 4.dp)
                    )
                    Button(
                        onClick = { onSaveWebdav(url, user, pass); message = "配置已保存" },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("保存配置") }
                }
            }
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { onWebdavUpload(url, user, pass) { message = it } },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Rounded.CloudUpload, contentDescription = null)
                        Text(text = "上传", style = MaterialTheme.typography.button,
                            modifier = Modifier.padding(start = 4.dp))
                    }
                    Button(
                        onClick = { onWebdavDownload(url, user, pass) { message = it } },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(imageVector = Icons.Rounded.CloudDownload, contentDescription = null)
                        Text(text = "下载", style = MaterialTheme.typography.button,
                            modifier = Modifier.padding(start = 4.dp))
                    }
                }
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun Header(onBack: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconButton(
            icon = Icons.AutoMirrored.Rounded.ArrowBack,
            contentDescription = "返回",
            onClick = onBack
        )
        Text(
            text = "备份与恢复",
            style = MaterialTheme.typography.title3,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.caption2,
        color = MaterialTheme.colors.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
private fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Chip(
        onClick = onClick,
        label = { Text(label) },
        icon = {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colors.primary)
        },
        modifier = modifier
    )
}

@Composable
private fun BackupRow(file: File, onRestore: () -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = file.name,
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface,
                maxLines = 1
            )
            Text(
                text = "${file.length() / 1024} KB",
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurfaceVariant
            )
        }
        Button(onClick = onRestore) {
            Icon(imageVector = Icons.Rounded.Restore, contentDescription = null)
            Text(text = "恢复", style = MaterialTheme.typography.button,
                modifier = Modifier.padding(start = 4.dp))
        }
    }
}

@Composable
private fun FormField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        textStyle = MaterialTheme.typography.body2.copy(
            color = MaterialTheme.colors.onSurface
        ),
        singleLine = true,
        cursorBrush = SolidColor(MaterialTheme.colors.primary),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurfaceVariant
                )
            }
            inner()
        }
    )
}
