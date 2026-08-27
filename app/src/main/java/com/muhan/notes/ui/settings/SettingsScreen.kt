package com.muhan.notes.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.AutoFixHigh
import androidx.compose.material.icons.rounded.Backup
import androidx.compose.material.icons.rounded.DeleteSweep
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Sync
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipColors
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
 * 设置页：调整界面缩放大小、字体大小、自动保存开关；
 * 隐私中心 / 回收站 / 备份与恢复 / 多设备同步入口；关于页。
 */
@Composable
fun SettingsScreen(
    uiScale: Float,
    fontScale: Float,
    autoSave: Boolean,
    hasPrivacyPassword: Boolean,
    onUiScaleChange: (Float) -> Unit,
    onFontScaleChange: (Float) -> Unit,
    onAutoSaveChange: (Boolean) -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenTrash: () -> Unit,
    onOpenBackup: () -> Unit,
    onOpenSync: () -> Unit,
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
                SettingChip(
                    label = if (autoSave) "自动保存：开" else "自动保存：关",
                    icon = Icons.Rounded.AutoFixHigh,
                    onClick = { onAutoSaveChange(!autoSave) },
                    iconTint = if (autoSave) MaterialTheme.colors.primary
                    else MaterialTheme.colors.onSurfaceVariant,
                    colors = if (autoSave) ChipDefaults.secondaryChipColors()
                    else ChipDefaults.primaryChipColors()
                )
            }
            item {
                SettingChip(
                    label = if (hasPrivacyPassword) "隐私中心（已设置密码）" else "隐私中心（设置密码）",
                    icon = Icons.Rounded.Lock,
                    onClick = onOpenPrivacy,
                    iconTint = MaterialTheme.colors.onSurfaceVariant,
                    colors = if (hasPrivacyPassword) ChipDefaults.secondaryChipColors()
                    else ChipDefaults.primaryChipColors()
                )
            }
            item {
                SettingChip(
                    label = "回收站",
                    icon = Icons.Rounded.DeleteSweep,
                    onClick = onOpenTrash
                )
            }
            item {
                SettingChip(
                    label = "备份与恢复",
                    icon = Icons.Rounded.Backup,
                    onClick = onOpenBackup
                )
            }
            item {
                SettingChip(
                    label = "多设备同步",
                    icon = Icons.Rounded.Sync,
                    onClick = onOpenSync
                )
            }
            item {
                SettingChip(
                    label = "关于",
                    icon = Icons.Rounded.Info,
                    onClick = onOpenAbout
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

/**
 * 统一样式的设置项：占满整行（与滑块设置项同宽），
 * 文字单行省略，避免窄屏上文字与图标重叠。
 */
@Composable
private fun SettingChip(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    iconTint: Color = MaterialTheme.colors.onSurfaceVariant,
    colors: ChipColors = ChipDefaults.primaryChipColors()
) {
    Chip(
        onClick = onClick,
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.body1,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                softWrap = false,
                modifier = Modifier.fillMaxWidth()
            )
        },
        icon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(ChipDefaults.IconSize)
            )
        },
        colors = colors,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}
