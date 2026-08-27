package com.muhan.notes.ui.edit

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.MediaStore
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.muhan.notes.data.Attachment
import com.muhan.notes.data.Note
import com.muhan.notes.ui.components.AppIconButton
import com.muhan.notes.ui.components.VoiceButton
import com.muhan.notes.ui.theme.NOTE_COLOR_PALETTE
import com.muhan.notes.ui.theme.asColor
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val AUTO_SAVE_DEBOUNCE_MS = 1_200L

/**
 * 新建 / 编辑笔记页。手表端通过语音输入文字，同时保留手动输入能力。
 * 支持添加图片、视频附件，以及软件内录音；开启「自动保存」后停止输入约 1.2 秒即自动保存。
 */
@Composable
fun NoteEditScreen(
    noteId: Long,
    loadNote: suspend (Long) -> Note?,
    autoSave: Boolean,
    onSave: suspend (Note?, String, String, Long, Boolean) -> Long,
    onDelete: (Long) -> Unit,
    onBack: () -> Unit,
    loadAttachments: suspend (Long) -> List<Attachment>,
    isRecording: Boolean,
    onStartRecording: () -> Unit,
    onStopRecording: (onResult: (String?) -> Unit) -> Unit,
    onAddAttachment: (Long, String, Uri) -> Unit,
    onAddLocalAttachment: (Long, String, String) -> Unit,
    onDeleteAttachment: (Long) -> Unit,
    onTouchNote: (Long) -> Unit
) {
    val isEditing = noteId > 0
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // savedNote 表示「最后一次已落库的笔记」；新建笔记在首次保存前为 null
    var savedNote by remember(noteId) { mutableStateOf<Note?>(null) }
    var title by remember(noteId) { mutableStateOf("") }
    var content by remember(noteId) { mutableStateOf("") }
    var color by remember(noteId) { mutableStateOf(Note.DEFAULT_COLOR) }
    var isPinned by remember(noteId) { mutableStateOf(false) }

    var attachments by remember(noteId) { mutableStateOf<List<Attachment>>(emptyList()) }
    var previewAttachment by remember { mutableStateOf<Attachment?>(null) }
    var showVideoOptions by remember { mutableStateOf(false) }
    var recordingSeconds by remember { mutableIntStateOf(0) }

    LaunchedEffect(noteId) {
        if (isEditing) {
            savedNote = loadNote(noteId)
            savedNote?.let { loaded ->
                title = loaded.title
                content = loaded.content
                color = loaded.color
                isPinned = loaded.isPinned
                attachments = loadAttachments(loaded.id)
            }
        }
    }

    /**
     * 确保笔记已落库：新建笔记首次添加附件前先保存，拿到真实 id。
     */
    suspend fun ensureSaved(): Long? {
        savedNote?.let { return it.id }
        val newId = onSave(null, title, content, color, isPinned)
        savedNote = Note(
            id = newId,
            title = title.trim(),
            content = content.trim(),
            color = color,
            isPinned = isPinned
        )
        return newId
    }

    /** 附件写入数据库后稍等片刻再刷新列表 */
    fun reloadAttachmentsDelayed() {
        scope.launch {
            delay(500)
            val id = savedNote?.id ?: return@launch
            attachments = loadAttachments(id)
        }
    }

    // 自动保存：输入防抖，与已保存内容一致时跳过
    LaunchedEffect(title, content, color, isPinned, autoSave) {
        if (!autoSave) return@LaunchedEffect
        val t = title.trim()
        val c = content.trim()
        if (t.isEmpty() && c.isEmpty()) return@LaunchedEffect
        val unchanged = savedNote != null &&
            savedNote!!.title == t &&
            savedNote!!.content == c &&
            savedNote!!.color == color &&
            savedNote!!.isPinned == isPinned
        if (unchanged) return@LaunchedEffect
        delay(AUTO_SAVE_DEBOUNCE_MS)
        val newId = onSave(savedNote, title, content, color, isPinned)
        // 将返回的 id 还原为已保存的 Note，供后续比较与删除使用
        savedNote = if (savedNote == null) {
            Note(
                id = newId,
                title = title.trim(),
                content = content.trim(),
                color = color,
                isPinned = isPinned
            )
        } else {
            savedNote!!.copy(
                title = title.trim(),
                content = content.trim(),
                color = color,
                isPinned = isPinned
            )
        }
    }

    // 录音计时
    LaunchedEffect(isRecording) {
        recordingSeconds = 0
        if (isRecording) {
            while (true) {
                delay(1_000)
                recordingSeconds++
            }
        }
    }

    // 离开编辑页时若仍在录音，则停止并丢弃
    DisposableEffect(Unit) {
        onDispose {
            onStopRecording { }
        }
    }

    val listState = rememberScalingLazyListState()

    // 选择图片
    val imagePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { picked ->
            scope.launch {
                val id = ensureSaved() ?: return@launch
                onAddAttachment(id, Attachment.TYPE_IMAGE, picked)
                onTouchNote(id)
                reloadAttachmentsDelayed()
            }
        }
    }

    // 相册选择视频
    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { picked ->
            scope.launch {
                val id = ensureSaved() ?: return@launch
                onAddAttachment(id, Attachment.TYPE_VIDEO, picked)
                onTouchNote(id)
                reloadAttachmentsDelayed()
            }
        }
    }

    // 系统相机拍摄视频
    val videoCamera = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                scope.launch {
                    val id = ensureSaved() ?: return@launch
                    onAddAttachment(id, Attachment.TYPE_VIDEO, uri)
                    onTouchNote(id)
                    reloadAttachmentsDelayed()
                }
            }
        }
    }

    // 录音权限
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onStartRecording()
    }

    fun pickVideoFromCamera() {
        try {
            videoCamera.launch(Intent(MediaStore.ACTION_VIDEO_CAPTURE))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(context, "设备不支持拍摄视频", Toast.LENGTH_SHORT).show()
        }
    }

    fun onRecordAudio() {
        if (isRecording) {
            onStopRecording { path ->
                if (path != null) {
                    scope.launch {
                        val id = ensureSaved() ?: return@launch
                        onAddLocalAttachment(id, Attachment.TYPE_AUDIO, path)
                        onTouchNote(id)
                        reloadAttachmentsDelayed()
                    }
                }
            }
        } else {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) onStartRecording()
            else audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    Scaffold {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
            item {
                EditHeader(
                    isEditing = isEditing,
                    onBack = {
                        if (isRecording) onStopRecording { }
                        onBack()
                    }
                )
            }
            item {
                FieldCard(
                    value = title,
                    onValueChange = { title = it },
                    placeholder = "标题",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    singleLine = true
                )
            }
            item {
                FieldCard(
                    value = content,
                    onValueChange = { content = it },
                    placeholder = "记录点什么吧…",
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                    singleLine = false,
                    minLines = 4
                )
            }
            item {
                ColorPicker(
                    selected = color,
                    onSelect = { color = it },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            item {
                Chip(
                    onClick = { isPinned = !isPinned },
                    label = { Text(text = if (isPinned) "已置顶" else "置顶") },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.PushPin,
                            contentDescription = null,
                            tint = if (isPinned) MaterialTheme.colors.primary
                            else MaterialTheme.colors.onSurfaceVariant
                        )
                    },
                    colors = if (isPinned) ChipDefaults.secondaryChipColors()
                    else ChipDefaults.primaryChipColors(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            item {
                MediaSection(
                    attachments = attachments,
                    isRecording = isRecording,
                    recordingSeconds = recordingSeconds,
                    onAddImage = { imagePicker.launch("image/*") },
                    onAddVideo = { showVideoOptions = true },
                    onRecordAudio = { onRecordAudio() },
                    onPlay = { previewAttachment = it },
                    onDelete = { attachment ->
                        onDeleteAttachment(attachment.id)
                        attachments = attachments.filterNot { it.id == attachment.id }
                    },
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
            item {
                Button(
                    onClick = {
                        scope.launch {
                            onSave(savedNote, title, content, color, isPinned)
                            onBack()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp)
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = null,
                            modifier = Modifier
                                .width(20.dp)
                                .height(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(text = "保存", style = MaterialTheme.typography.button)
                    }
                }
            }
            if (savedNote != null) {
                item {
                    Button(
                        onClick = { onDelete(savedNote!!.id) },
                        colors = ButtonDefaults.primaryButtonColors(
                            backgroundColor = MaterialTheme.colors.error,
                            contentColor = MaterialTheme.colors.onError
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp)
                            .padding(horizontal = 12.dp, vertical = 4.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Rounded.Delete,
                                contentDescription = null,
                                modifier = Modifier
                                    .width(20.dp)
                                    .height(20.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(text = "删除", style = MaterialTheme.typography.button)
                        }
                    }
                }
            }
        }
    }

    // 视频来源选择（拍摄 / 相册）
    if (showVideoOptions) {
        androidx.compose.ui.window.Dialog(onDismissRequest = { showVideoOptions = false }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colors.surface)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "添加视频",
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Button(
                    onClick = {
                        showVideoOptions = false
                        pickVideoFromCamera()
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text("拍摄视频")
                }
                Button(
                    onClick = {
                        showVideoOptions = false
                        videoPicker.launch("video/*")
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text("从相册选择")
                }
            }
        }
    }

    // 附件预览：图片全屏 / 视频、音频弹窗播放
    previewAttachment?.let { attachment ->
        MediaPreviewDialog(
            attachment = attachment,
            onDismiss = { previewAttachment = null }
        )
    }
}

@Composable
private fun EditHeader(isEditing: Boolean, onBack: () -> Unit) {
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
            text = if (isEditing) "编辑笔记" else "新建笔记",
            style = MaterialTheme.typography.title3,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/** 带语音输入按钮的文本输入卡片 */
@Composable
private fun FieldCard(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    singleLine: Boolean = true,
    minLines: Int = 1
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = MaterialTheme.typography.body1.copy(
                color = MaterialTheme.colors.onSurface
            ),
            singleLine = singleLine,
            minLines = minLines,
            cursorBrush = SolidColor(MaterialTheme.colors.primary),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.body1,
                        color = MaterialTheme.colors.onSurfaceVariant
                    )
                }
                innerTextField()
            }
        )
        Spacer(modifier = Modifier.width(4.dp))
        VoiceButton(
            onText = { spoken ->
                onValueChange(
                    if (value.isBlank()) spoken else "$value $spoken"
                )
            }
        )
    }
}

/** 小米便签风格颜色选择器 */
@Composable
private fun ColorPicker(
    selected: Long,
    onSelect: (Long) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "颜色",
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurfaceVariant
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            NOTE_COLOR_PALETTE.forEach { (_, colorValue) ->
                val isSelected = colorValue == selected
                val diameter = if (isSelected) 34.dp else 28.dp
                Box(
                    modifier = Modifier
                        .size(diameter)
                        .clip(CircleShape)
                        .background(colorValue.asColor())
                        .then(
                            if (isSelected) {
                                Modifier.border(
                                    width = 2.dp,
                                    color = MaterialTheme.colors.onSurface,
                                    shape = CircleShape
                                )
                            } else {
                                Modifier
                            }
                        )
                        .clickable { onSelect(colorValue) }
                )
            }
        }
    }
}
