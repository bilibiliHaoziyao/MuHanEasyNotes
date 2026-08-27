package com.muhan.notes.ui.about

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.muhan.notes.BuildConfig
import com.muhan.notes.ui.components.AppIconButton

/**
 * 关于页：展示应用名称、版本、简介、开源协议与仓库链接。
 */
@Composable
fun AboutScreen(onBack: () -> Unit) {
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
                    Text(
                        text = "关于",
                        style = MaterialTheme.typography.title3,
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
            item {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colors.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.EditNote,
                        contentDescription = null,
                        tint = MaterialTheme.colors.onPrimary,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }
            item {
                Text(
                    text = "慕寒轻松记",
                    style = MaterialTheme.typography.title2,
                    color = MaterialTheme.colors.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                )
            }
            item {
                Text(
                    text = "版本 ${BuildConfig.VERSION_NAME}",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp)
                )
            }
            item {
                Text(
                    text = "该软件由 慕寒_Official 开发",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                )
            }
            item {
                Text(
                    text = "专为 Wear OS 智能手表设计的记事本\n支持语音输入、多彩笔记与置顶",
                    style = MaterialTheme.typography.body2,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
            item {
                Text(
                    text = "开源协议：MIT License\n仓库：github.com/bilibiliHaoziyao/MuHanEasyNotes",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 12.dp)
                )
            }
        }
    }
}
