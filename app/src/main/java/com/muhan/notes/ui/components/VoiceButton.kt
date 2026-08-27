package com.muhan.notes.ui.components

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import com.muhan.notes.R

/** 构造系统语音识别 Intent */
private fun buildRecognizerIntent(context: Context): Intent =
    Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_PROMPT, context.getString(R.string.voice_prompt))
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
    }

/**
 * 麦克风按钮：调用系统语音识别，把识别结果通过 [onText] 回传。
 * - 设备没有语音识别引擎时**不渲染**（隐藏语音输入）
 * - 自动处理 RECORD_AUDIO 运行时权限申请
 */
@Composable
fun VoiceButton(
    onText: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    // 无语音识别引擎时隐藏语音输入按钮
    if (!SpeechRecognizer.isRecognitionAvailable(context)) return

    val speechLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            result.data
                ?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
                ?.firstOrNull()
                ?.let(onText)
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            speechLauncher.launch(buildRecognizerIntent(context))
        }
    }

    AppIconButton(
        icon = Icons.Rounded.Mic,
        contentDescription = context.getString(R.string.voice_input),
        onClick = {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            if (granted) {
                speechLauncher.launch(buildRecognizerIntent(context))
            } else {
                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            }
        },
        modifier = modifier
    )
}
