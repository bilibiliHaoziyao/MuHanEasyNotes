package com.muhan.notes.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Key
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text

/**
 * 通用密码输入弹窗。输入框为掩码显示。
 */
@Composable
fun PasswordDialog(
    title: String,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    confirmText: String = "确定",
    errorText: String? = null
) {
    var password by remember { mutableStateOf("") }

    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colors.surface)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Rounded.Key,
                contentDescription = null,
                tint = MaterialTheme.colors.primary
            )
            Text(
                text = title,
                style = MaterialTheme.typography.title3,
                color = MaterialTheme.colors.onSurface,
                modifier = Modifier.padding(top = 6.dp)
            )
            BasicTextField(
                value = password,
                onValueChange = { password = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colors.background)
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                textStyle = MaterialTheme.typography.body1.copy(
                    color = MaterialTheme.colors.onSurface,
                    textAlign = TextAlign.Center
                ),
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                cursorBrush = SolidColor(MaterialTheme.colors.primary),
                decorationBox = { inner ->
                    if (password.isEmpty()) {
                        Text(
                            text = "请输入密码",
                            style = MaterialTheme.typography.body1,
                            color = MaterialTheme.colors.onSurfaceVariant
                        )
                    }
                    inner()
                }
            )
            if (errorText != null) {
                Text(
                    text = errorText,
                    style = MaterialTheme.typography.caption1,
                    color = MaterialTheme.colors.error,
                    modifier = Modifier.padding(top = 6.dp)
                )
            }
            Row(modifier = Modifier.padding(top = 12.dp)) {
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("取消", style = MaterialTheme.typography.button)
                }
                Button(
                    onClick = { onConfirm(password) },
                    modifier = Modifier
                        .weight(1f)
                        .padding(start = 8.dp)
                ) {
                    Text(confirmText, style = MaterialTheme.typography.button)
                }
            }
        }
    }
}
