package com.muhan.notes.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors

/** 品牌主色：青绿 */
val MuHanTeal = Color(0xFF26A69A)

/**
 * 统一配色方案：以品牌青绿色为主色，深色背景，
 * 全应用（按钮、芯片、图标、强调色）保持一致。
 */
val MuHanColors = Colors(
    primary = MuHanTeal,
    primaryVariant = Color(0xFF00897B),
    secondary = Color(0xFF4DB6AC),
    secondaryVariant = MuHanTeal,
    background = Color(0xFF101418),
    surface = Color(0xFF1C2228),
    error = Color(0xFFEF5350),
    onPrimary = Color.White,
    onSecondary = Color(0xFF001F1B),
    onBackground = Color(0xFFE0E6EC),
    onSurface = Color(0xFFE0E6EC),
    onSurfaceVariant = Color(0xFF9AA7B4),
    onError = Color.White
)

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
