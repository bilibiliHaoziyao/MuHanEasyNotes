package com.muhan.notes.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme

/**
 * 圆形图标按钮。用 Box + Icon 显式绘制，确保图标始终可见，
 * 规避 Wear Button 在 BoxScope 中多子元素堆叠/裁剪问题。
 * 支持可选的长按回调（用于设置键长按进入隐私中心）。
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppIconButton(
    icon: ImageVector,
    contentDescription: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.secondary,
    tint: Color = MaterialTheme.colors.onSecondary,
    enabled: Boolean = true,
    onLongClick: (() -> Unit)? = null
) {
    val clickableModifier = if (onLongClick != null) {
        Modifier.combinedClickable(
            enabled = enabled,
            onClick = onClick,
            onLongClick = onLongClick
        )
    } else {
        Modifier.clickable(enabled = enabled, onClick = onClick)
    }
    Box(
        modifier = modifier
            .size(48.dp)
            .clip(CircleShape)
            .background(backgroundColor)
            .then(clickableModifier),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = contentDescription,
            tint = tint,
            modifier = Modifier.size(24.dp)
        )
    }
}
