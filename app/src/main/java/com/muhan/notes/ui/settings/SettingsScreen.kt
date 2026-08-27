package com.muhan.notes.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Info
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.InlineSlider
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.muhan.notes.SettingsViewModel
import com.muhan.notes.ui.components.AppIconButton
import kotlin.math.roundToInt

/**
 * 设置页：调整界面缩放大小、字体大小、自动保存开关，以及关于页入口。
 */
@Composable
fun SettingsScreen(
    uiScale: Float,
    fontScale: Float,
    autoSave: Boolean,
    onUiScaleChange: (Float) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onAutoSaveChange: (Boolean) -> Unit,
    onOpenAbout: () -> Unit,
    onBack: () -> Unit
) {
    val listState = rememberScalingLazyListState()

    Scaffold {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState
        ) {
            item {
                SettingsHeader(onBack = onBack)
            }
            item {
                SettingSlider(
                    label = "缩放大小",
                    value = uiScale,
                    onValueChange = onUiScaleChange,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            item {
                SettingSlider(
                    label = "字体大小",
                    value = fontScale,
                    onValueChange = onFontScaleChange,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            item {
                Chip(
                    onClick = { onAutoSaveChange(!autoSave) },
                    label = {
                        Text(text = if (autoSave) "自动保存：开" else "自动保存：关")
                    },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.AutoFixHigh,
                            contentDescription = null,
                            tint = if (autoSave) MaterialTheme.colors.primary
                            else MaterialTheme.colors.onSurfaceVariant
                        )
                    },
                    colors = if (autoSave) ChipDefaults.secondaryChipColors()
                    else ChipDefaults.primaryChipColors(),
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            item {
                Chip(
                    onClick = onOpenAbout,
                    label = { Text(text = "关于") },
                    icon = {
                        Icon(
                            imageVector = Icons.Rounded.Info,
                            contentDescription = null,
                            tint = MaterialTheme.colors.onSurfaceVariant
                        )
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
        }
    }
}

@Composable
private fun SettingsHeader(onBack: () -> Unit) {
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
            text = "设置",
            style = MaterialTheme.typography.title3,
            color = MaterialTheme.colors.onSurface,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/** 带百分比显示的滑块设置项 */
@Composable
private fun SettingSlider(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "$label  ${(value * 100).roundToInt()}%",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onSurface
        )
        InlineSlider(
            value = value,
            onValueChange = onValueChange,
            steps = 6,
            decreaseIcon = {},
            increaseIcon = {},
            valueRange = SettingsViewModel.MIN_SCALE..SettingsViewModel.MAX_SCALE
        )
    }
}
