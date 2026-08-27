package com.muhan.notes.ui.sync

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Bluetooth
import androidx.compose.material.icons.rounded.Cast
import androidx.compose.material.icons.rounded.CastConnected
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import com.muhan.notes.ui.components.AppIconButton

/**
 * 多设备同步：
 * - 局域网：一台设备作为主机等待连接，另一台输入主机 IP 连接
 * - 蓝牙：一台设备作为主机等待，另一台选择已配对设备连接
 * 连接后双方互相交换备份并自动合并。
 */
@Composable
fun SyncScreen(
    syncStatus: String,
    onResetStatus: () -> Unit,
    onLanHost: () -> Unit,
    onLanConnect: (String) -> Unit,
    onBtHost: () -> Unit,
    onBtConnect: (String) -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val listState = rememberScalingLazyListState()
    var hostIp by remember { mutableStateOf("") }
    var showBondedDialog by remember { mutableStateOf(false) }

    val btPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) onBtHost()
    }

    fun requestBtHost() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) onBtHost()
        else btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
    }

    // 读取已配对设备（需要 BLUETOOTH_CONNECT 权限）
    val bondedDevices = remember {
        runCatching {
            BluetoothAdapter.getDefaultAdapter()?.bondedDevices?.toList() ?: emptyList()
        }.getOrDefault(emptyList())
    }

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
                        imageVector = Icons.Rounded.Cast,
                        contentDescription = null,
                        tint = MaterialTheme.colors.primary,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                    Text(
                        text = "多设备同步",
                        style = MaterialTheme.typography.title3,
                        color = MaterialTheme.colors.onSurface,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }
            }
            item {
                Text(
                    text = "两台设备安装本应用后，一台做主机、另一台连接，\n会自动互相交换并合并笔记。",
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            item { SectionTitle("局域网") }
            item {
                ActionChip(
                    icon = Icons.Rounded.CastConnected,
                    label = "作为主机等待连接",
                    onClick = onLanHost,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            item {
                Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)) {
                    BasicTextField(
                        value = hostIp,
                        onValueChange = { hostIp = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colors.surface)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        textStyle = MaterialTheme.typography.body2.copy(
                            color = MaterialTheme.colors.onSurface
                        ),
                        singleLine = true,
                        cursorBrush = SolidColor(MaterialTheme.colors.primary),
                        decorationBox = { inner ->
                            if (hostIp.isEmpty()) {
                                Text(
                                    text = "输入主机 IP（如 192.168.1.5）",
                                    style = MaterialTheme.typography.body2,
                                    color = MaterialTheme.colors.onSurfaceVariant
                                )
                            }
                            inner()
                        }
                    )
                    Button(
                        onClick = { if (hostIp.isNotBlank()) onLanConnect(hostIp.trim()) },
                        modifier = Modifier.fillMaxWidth().padding(top = 6.dp)
                    ) { Text("连接主机") }
                }
            }

            item { SectionTitle("蓝牙") }
            item {
                ActionChip(
                    icon = Icons.Rounded.Bluetooth,
                    label = "作为主机等待连接",
                    onClick = { requestBtHost() },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }
            item {
                ActionChip(
                    icon = Icons.Rounded.Cast,
                    label = "连接已配对设备",
                    onClick = {
                        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) ==
                            PackageManager.PERMISSION_GRANTED
                        if (granted) showBondedDialog = true
                        else btPermissionLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                    },
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            item {
                Text(
                    text = if (syncStatus.isBlank()) "状态：空闲" else "状态：$syncStatus",
                    style = MaterialTheme.typography.body2,
                    color = if (syncStatus.contains("完成")) MaterialTheme.colors.primary
                    else MaterialTheme.colors.onSurface,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp)
                )
            }
            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showBondedDialog) {
        Dialog(onDismissRequest = { showBondedDialog = false }) {
            Column(
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .background(MaterialTheme.colors.surface)
                    .padding(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "选择已配对设备",
                    style = MaterialTheme.typography.title3,
                    color = MaterialTheme.colors.onSurface,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                if (bondedDevices.isEmpty()) {
                    Text(
                        text = "没有已配对设备\n请先在系统设置中配对",
                        style = MaterialTheme.typography.body2,
                        color = MaterialTheme.colors.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    bondedDevices.forEach { device ->
                        Button(
                            onClick = {
                                showBondedDialog = false
                                onBtConnect(device.address)
                            },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                        ) {
                            Text(device.name ?: device.address)
                        }
                    }
                }
                Button(
                    onClick = { showBondedDialog = false },
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
                ) { Text("取消") }
            }
        }
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.caption2,
        color = MaterialTheme.colors.onSurfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 4.dp)
    )
}

@Composable
private fun ActionChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Chip(
        onClick = onClick,
        label = { Text(label) },
        icon = {
            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colors.primary)
        },
        modifier = modifier
    )
}
