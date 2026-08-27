package com.muhan.notes.data

import android.content.Context
import android.media.MediaRecorder
import java.io.File

/**
 * 软件内录音：基于 MediaRecorder 的简单封装。
 * 支持开始 / 停止，录音文件写入应用私有目录。
 */
class AudioRecorder(private val context: Context) {

    private var recorder: MediaRecorder? = null

    /** 正在录音的目标文件 */
    var outputFile: File? = null
        private set

    val isRecording: Boolean
        get() = recorder != null

    fun start(): Boolean {
        stopQuietly()
        return try {
            val file = AttachmentStorage.createNewFile(context, Attachment.TYPE_AUDIO)
            val r = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }
            r.setAudioSource(MediaRecorder.AudioSource.MIC)
            r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            r.setAudioEncodingBitRate(64_000)
            r.setAudioSamplingRate(44_100)
            r.setOutputFile(file.absolutePath)
            r.prepare()
            r.start()
            outputFile = file
            recorder = r
            true
        } catch (e: Exception) {
            recorder = null
            outputFile = null
            false
        }
    }

    /** 停止录音，返回录音文件路径；失败返回 null（同时清理空文件） */
    fun stop(): String? {
        val file = outputFile
        val r = recorder
        recorder = null
        outputFile = null
        if (r == null) return null
        return try {
            r.stop()
            r.release()
            file?.takeIf { it.exists() && it.length() > 0 }?.absolutePath
        } catch (e: Exception) {
            runCatching { r.release() }
            if (file?.exists() == true) file.delete()
            null
        }
    }

    /** 放弃当前录音（删除临时文件） */
    fun cancel() {
        val file = outputFile
        val r = recorder
        recorder = null
        outputFile = null
        if (r != null) {
            runCatching { r.stop() }
            runCatching { r.release() }
        }
        if (file?.exists() == true) file.delete()
    }

    private fun stopQuietly() {
        val r = recorder
        if (r != null) {
            runCatching { r.stop() }
            runCatching { r.release() }
        }
        recorder = null
    }
}
