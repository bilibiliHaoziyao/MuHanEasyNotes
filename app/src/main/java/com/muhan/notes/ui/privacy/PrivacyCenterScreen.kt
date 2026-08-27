package com.muhan.notes.ui.privacy

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.LockOpen
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.muhan.notes.data.Note
import com.muhan.notes.ui.components.AppIconButton
import com.muhan.notes.ui.components.NoteCard
import kotlinx.coroutines.launch

/**
 * 隐私中心：
 * - 未设置密码 -> 先设置密码；已设置 -> 输入密码解锁
 * - 解锁后展示隐私笔记；长按可移出隐私中心或删除
 * - 支持修改 / 清除密码
 */
@Composable
fun PrivacyCenterScreen(
    privateNotes: List<Note>,
    hasPassword: Boolean,
    onVerify: suspend (String) -> Boolean,
    onSetPassword: (String) -> Unit,
    onClearPassword: () -> Unit,
    onOpenNote: (Long) -> Unit,
    onSetPrivate: (Long, Boolean) -> Unit,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit
) {
    var unlocked by remember { mutableStateOf(false) }
    var showSetup by remember { mutableStateOf(!hasPassword) }
    var error by remember { mutableStateOf<String?>(null) }

    var manageMenu by remember { mutableStateOf(false) }
    var noteMenuTarget by remember { mutableStateOf<Note?>(null) }
    var changePassword by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val listState = rememberScalingLazyListState()

    fun unlockWith(input: String) {
        scope.launch {
            if (onVerify(input)) {
                unlocked = true
                error = null
            } else {
                error = "密码错误"
            }
        }
    }

    Scaffold {
        if (!unlocked) {
            // 锁定 / 设置密码界面
            Column(
                modifier = Modifier.fillMaxSize().padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                AppIconButton(
                    icon = Icons.AutoMirrored.Rounded.ArrowBack,
                    contentDescription = "返回",
                    onClick = onBack,
                    modifier = Modifier.align(Alignment.Start)
                )
                Icon(
                    imageVector = if (showSetup) Icons.Rounded.Lock else Icons.Rounded.LockOpen,
                    contentDescription = null,
                    tint = MaterialTheme.colors.primary,
                    modifier = Modifier.padding(top = 8.dp)
                )
                Text(
                    text = if (showSetup) "设置隐私密码" else "输入密码",
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(top = 6.dp)
                )
                Text(
                    text = "长按设置键即可进入隐私中心",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 2.dp)
                )
                if (showSetup) {
                    SetupPasswordForm(
                        onConfirm = { pass ->
                            if (pass.isBlank()) error = "密码不能为空"
                            else {
                                onSetPassword(pass)
                                unlocked = true
                            }
                        },
                        onSkip = { unlocked = true },
                        error = error
                    )
                } else {
                    PasswordEntryForm(
                        onConfirm = { unlockWith(it) },
                        error = error
                    )
                }
            }
        } else {
            // 已解锁：隐私笔记列表
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState
            ) {
                item {
                    PrivacyHeader(onBack = onBack, onManage = { manageMenu = true })
                }
                if (privateNotes.isEmpty()) {
                    item {
                        Text(
                            text = "还没有隐私笔记\n在主列表长按笔记可加入隐私中心",
                            style = MaterialTheme.typography.body2,
                            color = MaterialTheme.colors.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 24.dp, horizontal = 20.dp)
                        )
                    }
                }
                items(privateNotes, key = { it.id }) { note ->
                    NoteCard(
                        note = note,
                        onClick = { onOpenNote(note.id) },
                        onLongClick = { noteMenuTarget = note },
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                item { Spacer(modifier = Modifier.height(12.dp)) }
            }
        }
    }

    // 管理菜单：修改 / 清除密码
    if (manageMenu) {
        Dialog(onDismissRequest = { manageMenu = false }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colors.surface)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "隐私设置",
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Button(
                    onClick = {
                        manageMenu = false
                        changePassword = true
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) { Text("修改密码") }
                Button(
                    onClick = {
                        manageMenu = false
                        onClearPassword()
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) { Text("清除密码") }
                Button(
                    onClick = { manageMenu = false },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) { Text("取消") }
            }
        }
    }

    // 修改密码
    if (changePassword) {
        Dialog(onDismissRequest = { changePassword = false }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colors.surface)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "设置新密码",
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                var newPass by remember { mutableStateOf("") }
                BasicTextField(
                    value = newPass,
                    onValueChange = { newPass = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colors.background)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    textStyle = MaterialTheme.typography.body1.copy(
                        color = MaterialTheme.colors.onSurface,
                        textAlign = TextAlign.Center
                    ),
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    cursorBrush = SolidColor(MaterialTheme.colors.primary),
                    decorationBox = { inner ->
                        if (newPass.isEmpty()) {
                            Text("请输入新密码", style = MaterialTheme.typography.body1,
                                color = MaterialTheme.colors.onSurfaceVariant)
                        }
                        inner()
                    }
                )
                Button(
                    onClick = {
                        if (newPass.isNotBlank()) {
                            onSetPassword(newPass)
                            changePassword = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("确定") }
            }
        }
    }

    // 长按隐私笔记菜单
    noteMenuTarget?.let { note ->
        Dialog(onDismissRequest = { noteMenuTarget = null }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colors.surface)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "「${note.title.ifBlank { "无标题" }}」",
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp),
                    textAlign = TextAlign.Center
                )
                Button(
                    onClick = {
                        onSetPrivate(note.id, false)
                        noteMenuTarget = null
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) { Text("移出隐私中心") }
                Button(
                    onClick = {
                        onDelete(note.id)
                        noteMenuTarget = null
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) { Text("删除（到回收站）") }
                Button(
                    onClick = { noteMenuTarget = null },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) { Text("取消") }
            }
        }
    }
}

@Composable
private fun PrivacyHeader(onBack: () -> Unit, onManage: () -> Unit) {
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
        Icon(
            imageVector = Icons.Rounded.VisibilityOff,
            contentDescription = null,
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.padding(start = 8.dp)
        )
        Text(
            text = "隐私中心",
            style = MaterialTheme.typography.title3,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp)
        )
        AppIconButton(
            icon = Icons.Rounded.Settings,
            contentDescription = "隐私设置",
            onClick = onManage,
            modifier = Modifier.size(44.dp)
        )
    }
}

@Composable
private fun PasswordEntryForm(onConfirm: (String) -> Unit, error: String?) {
    var password by remember { mutableStateOf("") }
    PasswordField(
        value = password,
        onValueChange = { password = it },
        placeholder = "请输入密码",
        modifier = Modifier.padding(top = 10.dp)
    )
    if (error != null) {
        Text(
            text = error,
            style = MaterialTheme.typography.caption1,
            color = MaterialTheme.colors.error,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
    Button(
        onClick = { onConfirm(password) },
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
    ) { Text("解锁") }
}

@Composable
private fun SetupPasswordForm(
    onConfirm: (String) -> Unit,
    onSkip: () -> Unit,
    error: String?
) {
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    PasswordField(
        value = password,
        onValueChange = { password = it },
        placeholder = "设置密码",
        modifier = Modifier.padding(top = 10.dp)
    )
    PasswordField(
        value = confirm,
        onValueChange = { confirm = it },
        placeholder = "确认密码",
        modifier = Modifier.padding(top = 6.dp)
    )
    val shownError = error ?: localError
    if (shownError != null) {
        Text(
            text = shownError,
            style = MaterialTheme.typography.caption1,
            color = MaterialTheme.colors.error,
            modifier = Modifier.padding(top = 6.dp)
        )
    }
    Button(
        onClick = {
            when {
                password.isBlank() -> localError = "密码不能为空"
                password != confirm -> localError = "两次输入不一致"
                else -> onConfirm(password)
            }
        },
        modifier = Modifier.fillMaxWidth().padding(top = 10.dp)
    ) { Text("确定") }
    Text(
        text = "暂时跳过",
        style = MaterialTheme.typography.body2,
        color = MaterialTheme.colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp)
            .clickable(onClick = onSkip)
    )
}

@Composable
private fun PasswordField(
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
            .background(MaterialTheme.colors.background)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        textStyle = MaterialTheme.typography.body1.copy(
            color = MaterialTheme.colors.onSurface,
            textAlign = TextAlign.Center
        ),
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        cursorBrush = SolidColor(MaterialTheme.colors.primary),
        decorationBox = { inner ->
            if (value.isEmpty()) {
                Text(
                    text = placeholder,
                    style = MaterialTheme.typography.body1,
                    color = MaterialTheme.colors.onSurfaceVariant
                )
            }
            inner()
        }
    )
}
