package com.muhan.notes.ui.edit

import android.media.MediaPlayer
import android.widget.VideoView
import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Image
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Stop
import androidx.compose.material.icons.rounded.Videocam
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import coil.compose.AsyncImage
import com.muhan.notes.data.Attachment
import com.muhan.notes.ui.components.AppIconButton
import java.io.File

/**
 * 编辑页媒体区：添加图片 / 视频 / 录音按钮 + 附件列表。
 * 图片点击放大预览，视频/音频点击弹窗内播放，右侧 X 删除。
 */
@Composable
fun MediaSection(
    attachments: List<Attachment>,
    isRecording: Boolean,
    recordingSeconds: Int,
    onAddImage: () -> Unit,
    onAddVideo: () -> Unit,
    onRecordAudio: () -> Unit,
    onPlay: (Attachment) -> Unit,
    onDelete: (Attachment) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp)) {
        Text(
            text = "媒体",
            style = MaterialTheme.typography.caption2,
            color = MaterialTheme.colors.onSurfaceVariant
        )
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MediaAddButton(
                icon = Icons.Rounded.Image,
                label = "图片",
                onClick = onAddImage,
                modifier = Modifier.weight(1f)
            )
            MediaAddButton(
                icon = Icons.Rounded.Videocam,
                label = "视频",
                onClick = onAddVideo,
                modifier = Modifier.weight(1f)
            )
            MediaAddButton(
                icon = if (isRecording) Icons.Rounded.Stop else Icons.Rounded.Mic,
                label = if (isRecording) "停止" else "录音",
                onClick = onRecordAudio,
                modifier = Modifier.weight(1f),
                highlight = isRecording
            )
        }
        if (isRecording) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                RecordingDot()
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "录音中 ${formatSeconds(recordingSeconds)}",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.error
                )
            }
        }
        attachments.forEach { attachment ->
            AttachmentRow(
                attachment = attachment,
                onClick = { onPlay(attachment) },
                onDelete = { onDelete(attachment) },
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }
    }
}

/** 录音红点动画占位（静态红点） */
@Composable
private fun RecordingDot() {
    Box(
        modifier = Modifier
            .size(10.dp)
            .clip(RoundedCornerShape(50))
            .background(MaterialTheme.colors.error)
    )
}

private fun formatSeconds(total: Int): String {
    val m = total / 60
    val s = total % 60
    return "%02d:%02d".format(m, s)
}

@Composable
private fun MediaAddButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    highlight: Boolean = false
) {
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(if (highlight) MaterialTheme.colors.error else MaterialTheme.colors.surface)
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = if (highlight) MaterialTheme.colors.onError else MaterialTheme.colors.primary,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.caption2,
            color = if (highlight) MaterialTheme.colors.onError else MaterialTheme.colors.onSurface
        )
    }
}

@Composable
private fun AttachmentRow(
    attachment: Attachment,
    onClick: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colors.surface)
            .clickable(onClick = onClick)
            .padding(horizontal = 8.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AttachmentThumb(attachment, modifier = Modifier.size(40.dp))
        Spacer(modifier = Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = when (attachment.type) {
                    Attachment.TYPE_IMAGE -> "图片"
                    Attachment.TYPE_VIDEO -> "视频"
                    else -> "录音"
                },
                style = MaterialTheme.typography.body2,
                color = MaterialTheme.colors.onSurface
            )
            Text(
                text = File(attachment.filePath).name,
                style = MaterialTheme.typography.caption2,
                color = MaterialTheme.colors.onSurfaceVariant,
                maxLines = 1
            )
        }
        AppIconButton(
            icon = Icons.Rounded.Close,
            contentDescription = "删除附件",
            onClick = onDelete,
            modifier = Modifier.size(40.dp),
            backgroundColor = MaterialTheme.colors.surface,
            tint = MaterialTheme.colors.onSurfaceVariant
        )
    }
}

@Composable
private fun AttachmentThumb(attachment: Attachment, modifier: Modifier = Modifier) {
    val bg = MaterialTheme.colors.primaryVariant
    val fg = MaterialTheme.colors.onPrimary
    when (attachment.type) {
        Attachment.TYPE_IMAGE -> {
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colors.surface),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = File(attachment.filePath),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Attachment.TYPE_VIDEO -> {
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.PlayArrow,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
        else -> {
            Box(
                modifier = modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(bg),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.MusicNote,
                    contentDescription = null,
                    tint = fg,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

/**
 * 附件预览弹窗：图片全屏显示，视频/音频在弹窗内播放。
 */
@Composable
fun MediaPreviewDialog(attachment: Attachment, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colors.surface)
                .padding(12.dp),
            contentAlignment = Alignment.Center
        ) {
            when (attachment.type) {
                Attachment.TYPE_IMAGE -> {
                    AsyncImage(
                        model = File(attachment.filePath),
                        contentDescription = "图片预览",
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(RoundedCornerShape(8.dp))
                    )
                }
                Attachment.TYPE_VIDEO -> VideoPlayer(
                    path = attachment.filePath,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(8.dp))
                )
                else -> AudioPlayer(path = attachment.filePath)
            }
            // 右上角关闭
            AppIconButton(
                icon = Icons.Rounded.Close,
                contentDescription = "关闭",
                onClick = onDismiss,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .size(40.dp),
                backgroundColor = MaterialTheme.colors.surface,
                tint = MaterialTheme.colors.onSurfaceVariant
            )
        }
    }
}

/** 内嵌 VideoView 播放本地视频 */
@Composable
private fun VideoPlayer(path: String, modifier: Modifier = Modifier) {
    AndroidView(
        factory = { ctx ->
            VideoView(ctx).apply {
                setVideoPath(path)
                setOnPreparedListener { mp -> mp.start() }
                requestFocus()
            }
        },
        modifier = modifier
    )
}

/** MediaPlayer 播放本地音频 */
@Composable
private fun AudioPlayer(path: String) {
    val player = remember {
        MediaPlayer().apply {
            runCatching {
                setDataSource(path)
                prepare()
                start()
            }
        }
    }
    DisposableEffect(Unit) {
        onDispose {
            runCatching { player.stop() }
            runCatching { player.release() }
        }
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(
            imageVector = Icons.Rounded.MusicNote,
            contentDescription = null,
            tint = MaterialTheme.colors.primary,
            modifier = Modifier.size(40.dp)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "正在播放…",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface
        )
    }
}
