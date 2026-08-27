package com.muhan.notes.data

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/**
 * 附件文件管理：把选中的 / 录制的媒体文件复制到应用私有目录，
 * 应用卸载或清数据时自动清理，且不受源文件被删除影响。
 */
object AttachmentStorage {

    private val extForType = mapOf(
        Attachment.TYPE_IMAGE to "img",
        Attachment.TYPE_VIDEO to "vid",
        Attachment.TYPE_AUDIO to "aud"
    )

    /** 生成指定类型的存储目录（不存在则创建） */
    fun mediaDir(context: Context, type: String): File =
        File(context.filesDir, "media/${extForType[type] ?: "file"}").apply { mkdirs() }

    /**
     * 把 [uri] 指向的媒体内容复制进应用私有目录，返回复制后的绝对路径。
     * 文件名带随机后缀，避免重名覆盖。
     */
    fun copyFromUri(context: Context, type: String, uri: Uri): String? {
        return try {
            val resolver: ContentResolver = context.contentResolver
            val dir = mediaDir(context, type)
            val ext = typeExtension(type, resolver, uri)
            val target = File(dir, "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}$ext")
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(target).use { output -> input.copyTo(output) }
            } ?: return null
            target.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    /** 创建一条空的待写入文件（用于录音/录像） */
    fun createNewFile(context: Context, type: String): File {
        val dir = mediaDir(context, type)
        val ext = if (type == Attachment.TYPE_AUDIO) ".m4a" else ".mp4"
        val file = File(dir, "${System.currentTimeMillis()}_${UUID.randomUUID().toString().take(8)}$ext")
        file.createNewFile()
        return file
    }

    /** 删除附件文件（忽略不存在/删除失败） */
    fun deleteFile(path: String?) {
        if (path.isNullOrBlank()) return
        runCatching { File(path).delete() }
    }

    private fun typeExtension(type: String, resolver: ContentResolver, uri: Uri): String {
        val displayName = queryDisplayName(resolver, uri)
        val fromName = displayName?.substringAfterLast('.', "")?.takeIf { it.isNotBlank() }
        if (fromName != null) return ".$fromName"
        val mime = resolver.getType(uri).orEmpty()
        return when {
            mime.startsWith("image") -> ".jpg"
            mime.startsWith("video") -> ".mp4"
            mime.startsWith("audio") -> ".m4a"
            else -> ".bin"
        }
    }

    private fun queryDisplayName(resolver: ContentResolver, uri: Uri): String? =
        runCatching {
            resolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)
                ?.use { c ->
                    if (c.moveToFirst()) c.getString(0) else null
                }
        }.getOrNull()
}
