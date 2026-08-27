package com.muhan.notes.ui.list

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.muhan.notes.data.Note
import com.muhan.notes.ui.components.AppIconButton
import com.muhan.notes.ui.components.NoteCard

/**
 * 笔记列表页：顶部标题 + 全部笔记卡片 + 底部新建按钮。
 * 点击卡片编辑；长按卡片弹出菜单（加入隐私中心 / 删除）；
 * 点击设置键进设置；长按设置键进入隐私中心。
 */
@Composable
fun NoteListScreen(
    notes: List<Note>,
    onOpenNote: (Long) -> Unit,
    onNewNote: () -> Unit,
    onDelete: (Long) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onSetPrivate: (Long, Boolean) -> Unit
) {
    val listState = rememberScalingLazyListState()
    var noteMenuTarget by remember { mutableStateOf<Note?>(null) }

    Scaffold(
        positionIndicator = {
            if (notes.isNotEmpty()) {
                PositionIndicator(listState)
            }
        }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
            item { AppHeader(onOpenSettings = onOpenSettings, onOpenPrivacy = onOpenPrivacy) }
            if (notes.isEmpty()) {
                item { EmptyHint() }
            }
            items(notes, key = { it.id }) { note ->
                NoteCard(
                    note = note,
                    onClick = { onOpenNote(note.id) },
                    onLongClick = { noteMenuTarget = note },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            item {
                NewNoteButton(onClick = onNewNote)
            }
        }
    }

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
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                Button(
                    onClick = {
                        onSetPrivate(note.id, true)
                        noteMenuTarget = null
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Rounded.VisibilityOff,
                            contentDescription = null,
                            modifier = Modifier
                                .width(20.dp)
                                .height(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "加入隐私中心",
                            style = MaterialTheme.typography.button,
                            maxLines = 1
                        )
                    }
                }
                Button(
                    onClick = {
                        onDelete(note.id)
                        noteMenuTarget = null
                    },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = ButtonDefaults.primaryButtonColors(
                        backgroundColor = MaterialTheme.colors.error,
                        contentColor = MaterialTheme.colors.onError
                    )
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
                        Text(
                            text = "删除（到回收站）",
                            style = MaterialTheme.typography.button,
                            maxLines = 1
                        )
                    }
                }
                Button(
                    onClick = { noteMenuTarget = null },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(text = "取消", style = MaterialTheme.typography.button)
                }
            }
        }
    }
}

@Composable
private fun AppHeader(onOpenSettings: () -> Unit, onOpenPrivacy: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Rounded.EditNote,
            contentDescription = null,
            tint = MaterialTheme.colors.primary
        )
        Text(
            text = "慕寒轻松记",
            style = MaterialTheme.typography.title2,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier
                .weight(1f)
                .padding(start = 6.dp)
        )
        AppIconButton(
            icon = Icons.Rounded.Settings,
            contentDescription = "设置（长按进入隐私中心）",
            onClick = onOpenSettings,
            onLongClick = onOpenPrivacy
        )
    }
}

@Composable
private fun EmptyHint() {
    Text(
        text = "还没有笔记\n点击下方按钮，用语音快速记录",
        style = MaterialTheme.typography.body2,
        color = MaterialTheme.colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp, horizontal = 20.dp)
    )
}

@Composable
private fun NewNoteButton(onClick: () -> Unit) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(64.dp)
            .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector = Icons.Rounded.Add,
                contentDescription = null,
                modifier = Modifier
                    .width(20.dp)
                    .height(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "新建笔记",
                style = MaterialTheme.typography.button
            )
        }
    }
}
