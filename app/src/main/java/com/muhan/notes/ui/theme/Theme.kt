package com.muhan.notes.ui.theme

import androidx.compose.runtime.Composable
import androidx.wear.compose.material.MaterialTheme

/**
 * 应用主题：统一品牌配色（深色 + 青绿主色）。
 */
@Composable
fun MuHanTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = MuHanColors,
        content = content
    )
}
