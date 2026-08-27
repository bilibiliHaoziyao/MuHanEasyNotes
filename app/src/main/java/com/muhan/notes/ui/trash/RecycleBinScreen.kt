package com.muhan.notes.ui.trash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Restore
import androidx.compose.runtime.Composable
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
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.muhan.notes.data.Note
import com.muhan.notes.ui.components.AppIconButton
import com.muhan.notes.ui.formatTime

/**
 * 回收站：列出已删除笔记，可恢复或彻底删除。
 */
@Composable
fun RecycleBinScreen(
    trashedNotes: List<Note>,
    onRestore: (Long) -> Unit,
    onPurge: (Long) -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    Scaffold {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
            item {
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
                        imageVector = Icons.Rounded.DeleteForever,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    Text(
                        text = "回收站",
                        style = MaterialTheme.typography.title3,
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
            if (trashedNotes.isEmpty()) {
                item {
                    Text(
                        text = "回收站是空的",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                    )
                }
            }
            items(trashedNotes, key = { it.id }) { note ->
                TrashedRow(
                    note = note,
                    onRestore = { onRestore(note.id) },
                    onPurge = { onPurge(note.id) },
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            item { Spacer(modifier = Modifier.padding(top = 12.dp)) }
        }
    }
}

@Composable
private fun TrashedRow(
    note: Note,
    onRestore: () -> Unit,
    onPurge: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .padding(horizontal = 10.dp, vertical = 8.dp)
    ) {
        Text(
            text = note.title.ifBlank { "无标题" },
            style = MaterialTheme.typography.title3,
            color = MaterialTheme.colors.onSurface,
            maxLines = 1
        )
        Text(
            text = "删除于 ${formatTime(note.deletedAt ?: note.updatedAt)}",
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = onRestore,
                modifier = Modifier.weight(1f)
            ) {
                Icon(
                    imageVector = Icons.Rounded.Restore,
                    contentDescription = null
                )
                Text(
                    text = "恢复",
                    style = MaterialTheme.typography.button,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
            Button(
                onClick = onPurge,
                modifier = Modifier.weight(1f),
                colors = androidx.wear.compose.material.ButtonDefaults.primaryButtonColors(
                    backgroundColor = MaterialTheme.colors.error,
                    contentColor = MaterialTheme.colors.onError
                )
            ) {
                Icon(
                    imageVector = Icons.Rounded.DeleteForever,
                    contentDescription = null
                )
                Text(
                    text = "彻底删除",
                    style = MaterialTheme.typography.button,
                    modifier = Modifier.padding(start = 4.dp)
                )
            }
        }
    }
}
