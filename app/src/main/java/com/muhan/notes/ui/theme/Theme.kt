package com.muhan.notes.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

/**
 * 应用主题。Wear OS 手表通常为深色环境，直接使用 Wear Material 默认配色。
 */
@Composable
fun MuHanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        content = content
    )
}
