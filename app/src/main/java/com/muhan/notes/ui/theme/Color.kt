package com.muhan.notes.ui.theme

import androidx.compose.ui.graphics.Color

/** 小米便签风格的颜色盘：颜色名 -> ARGB 值 */
val NOTE_COLOR_PALETTE: List<Pair<String, Long>> = listOf(
    "黄" to 0xFFFFC107L,
    "橙" to 0xFFFF7043L,
    "红" to 0xFFF44336L,
    "绿" to 0xFF4CAF50L,
    "蓝" to 0xFF2196F3L,
    "紫" to 0xFF9C27B0L,
    "灰" to 0xFF607D8BL
)

/** 在暗色背景下，笔记颜色条的最小可读宽度 */
fun Long.asColor(): Color = Color(this)
