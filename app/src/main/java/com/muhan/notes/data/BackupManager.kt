package com.muhan.notes.data

import android.bluetooth.BluetoothAdapter
import android.content.Context
import android.net.Uri
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import java.net.HttpURLConnection
import java.net.ServerSocket
import java.net.Socket
import java.net.URL
import java.util.UUID
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * 备份 / 恢复 / 同步核心服务。
 *
 * 备份为 zip 压缩包，内含：
 * - notes.json      全部笔记（含隐私与回收站）
 * - attachments.json 附件元数据
 * - attachments/<id>/<文件名> 附件实体文件
 *
 * 恢复采用「合并」策略：标题+正文完全一致的笔记跳过，其余新增，避免重复。
 */
object BackupManager {

    private const val PORT = 18888
    private const val BT_SERVICE_NAME = "MuHanEasyNotes"
    private val BT_UUID: UUID = UUID.fromString("e0d0c3f4-9c5a-4b2f-8d1a-2f3c4d5e6f70")
    private const val REMOTE_BACKUP_NAME = "muhan_notes_backup.zip"
    private const val JSON_ENTRY = "notes.json"
    private const val ATTACH_JSON_ENTRY = "attachments.json"
    private const val ATTACH_DIR = "attachments"
    private const val MAX_TRANSFER_BYTES = 200L * 1024 * 1024

    // ------------------------------------------------------------------
    // 导出
    // ------------------------------------------------------------------

    /** 导出到应用私有 backups 目录，返回生成的 zip 文件；失败返回 null */
    suspend fun export(context: Context, repo: NoteRepository): File? = withContext(Dispatchers.IO) {
        runCatching {
            val dir = File(context.filesDir, "backups").apply { mkdirs() }
            val file = File(dir, "backup_${System.currentTimeMillis()}.zip")
            FileOutputStream(file).use { writeExportZip(context, repo, it) }
            file
        }.getOrNull()
    }

    /** 把备份 zip 写入任意输出流（如 SAF 的 ContentResolver） */
    suspend fun writeExportZip(context: Context, repo: NoteRepository, out: OutputStream): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val notes = repo.getAllNotes()
                val attachments = repo.getAllAttachments()
                val notesArr = JSONArray()
                notes.forEach { n ->
                    notesArr.put(
                        JSONObject().apply {
                            put("id", n.id)
                            put("title", n.title)
                            put("content", n.content)
                            put("color", n.color)
                            put("isPinned", n.isPinned)
                            put("isPrivate", n.isPrivate)
                            put("deletedAt", n.deletedAt ?: JSONObject.NULL)
                            put("createdAt", n.createdAt)
                            put("updatedAt", n.updatedAt)
                        }
                    )
                }
                val attArr = JSONArray()
                attachments.forEach { a ->
                    attArr.put(
                        JSONObject().apply {
                            put("id", a.id)
                            put("noteId", a.noteId)
                            put("type", a.type)
                            put("fileName", File(a.filePath).name)
                            put("createdAt", a.createdAt)
                        }
                    )
                }

                ZipOutputStream(out).use { zip ->
                    zip.putNextEntry(ZipEntry(JSON_ENTRY))
                    zip.write(notesArr.toString().toByteArray())
                    zip.closeEntry()

                    zip.putNextEntry(ZipEntry(ATTACH_JSON_ENTRY))
                    zip.write(attArr.toString().toByteArray())
                    zip.closeEntry()

                    attachments.forEach { a ->
                        val f = File(a.filePath)
                        if (f.exists()) {
                            zip.putNextEntry(ZipEntry("$ATTACH_DIR/${a.id}/${f.name}"))
                            f.inputStream().use { it.copyTo(zip) }
                            zip.closeEntry()
                        }
                    }
                }
                true
            }.getOrDefault(false)
        }

    // ------------------------------------------------------------------
    // 恢复（合并）
    // ------------------------------------------------------------------

    suspend fun restoreFromUri(context: Context, repo: NoteRepository, resolver: android.content.ContentResolver, uri: Uri): Int {
        return withContext(Dispatchers.IO) {
            runCatching {
                resolver.openInputStream(uri)?.use { restoreFromStream(context, repo, it) } ?: 0
            }.getOrDefault(0)
        }
    }

    suspend fun restoreFromFile(context: Context, repo: NoteRepository, file: File): Int {
        return withContext(Dispatchers.IO) {
            runCatching { file.inputStream().use { restoreFromStream(context, repo, it) } }.getOrDefault(0)
        }
    }

    /** 从 zip 流恢复，返回新增笔记数量 */
    suspend fun restoreFromStream(context: Context, repo: NoteRepository, input: InputStream): Int =
        withContext(Dispatchers.IO) {
            val entries = readAllEntries(input)
            val notesJson = entries[JSON_ENTRY]?.toString(Charsets.UTF_8) ?: return@withContext 0
            val now = System.currentTimeMillis()
            val oldToNew = mutableMapOf<Long, Long>()
            var added = 0
            runCatching {
                val notesArr = JSONArray(notesJson)
                for (i in 0 until notesArr.length()) {
                    val o = notesArr.getJSONObject(i)
                    val title = o.optString("title")
                    val content = o.optString("content")
                    val dup = repo.findDuplicate(title, content)
                    if (dup != null) {
                        oldToNew[o.optLong("id")] = dup
                        continue
                    }
                    val deletedAt = if (o.isNull("deletedAt")) null else o.optLong("deletedAt", -1).takeIf { it > 0 }
                    val newId = repo.addNote(
                        Note(
                            title = title,
                            content = content,
                            color = o.optLong("color", Note.DEFAULT_COLOR),
                            isPinned = o.optBoolean("isPinned", false),
                            isPrivate = o.optBoolean("isPrivate", false),
                            deletedAt = deletedAt,
                            createdAt = o.optLong("createdAt", now),
                            updatedAt = o.optLong("updatedAt", now)
                        )
                    )
                    oldToNew[o.optLong("id")] = newId
                    added++
                }

                val attJson = entries[ATTACH_JSON_ENTRY]?.toString(Charsets.UTF_8)
                if (attJson != null) {
                    val attArr = JSONArray(attJson)
                    for (i in 0 until attArr.length()) {
                        val o = attArr.getJSONObject(i)
                        val newNoteId = oldToNew[o.optLong("noteId")] ?: continue
                        val type = o.optString("type", Attachment.TYPE_IMAGE)
                        val fileName = o.optString("fileName")
                        if (fileName.isBlank()) continue
                        val bytes = entries["$ATTACH_DIR/${o.optLong("id")}/$fileName"] ?: continue
                        val dir = AttachmentStorage.mediaDir(context, type)
                        val target = File(dir, "${System.currentTimeMillis()}_$fileName")
                        target.writeBytes(bytes)
                        repo.addAttachment(
                            Attachment(noteId = newNoteId, type = type, filePath = target.absolutePath)
                        )
                    }
                }
            }
            added
        }

    private fun readAllEntries(input: InputStream): Map<String, ByteArray> {
        val map = mutableMapOf<String, ByteArray>()
        ZipInputStream(BufferedInputStream(input)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (!entry.isDirectory) {
                    map[entry.name] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        return map
    }

    // ------------------------------------------------------------------
    // WebDAV
    // ------------------------------------------------------------------

    suspend fun webdavUpload(baseUrl: String, user: String, pass: String, zipFile: File): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL(resolveRemoteUrl(baseUrl)) as HttpURLConnection).apply {
                    requestMethod = "PUT"
                    doOutput = true
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    setRequestProperty("Content-Type", "application/zip")
                    setRequestProperty("Authorization", basicAuth(user, pass))
                }
                conn.outputStream.use { out -> zipFile.inputStream().use { it.copyTo(out) } }
                val code = conn.responseCode
                conn.disconnect()
                code in 200..299
            }.getOrDefault(false)
        }

    suspend fun webdavDownload(baseUrl: String, user: String, pass: String, destFile: File): Boolean =
        withContext(Dispatchers.IO) {
            runCatching {
                val conn = (URL(resolveRemoteUrl(baseUrl)) as HttpURLConnection).apply {
                    requestMethod = "GET"
                    connectTimeout = 15_000
                    readTimeout = 30_000
                    setRequestProperty("Authorization", basicAuth(user, pass))
                }
                val code = conn.responseCode
                if (code in 200..299) {
                    conn.inputStream.use { input -> destFile.outputStream().use { input.copyTo(it) } }
                    conn.disconnect()
                    true
                } else {
                    conn.disconnect()
                    false
                }
            }.getOrDefault(false)
        }

    private fun resolveRemoteUrl(baseUrl: String): String {
        val trimmed = baseUrl.trim().trimEnd('/')
        return if (trimmed.endsWith(".zip", ignoreCase = true)) trimmed
        else "$trimmed/$REMOTE_BACKUP_NAME"
    }

    private fun basicAuth(user: String, pass: String): String {
        val raw = "$user:$pass".toByteArray()
        return "Basic " + Base64.encodeToString(raw, Base64.NO_WRAP)
    }

    // ------------------------------------------------------------------
    // 多设备同步（局域网 / 蓝牙）：对称式交换
    // 双方各自 发送自己的备份 -> 接收对方的备份 -> 合并恢复
    // ------------------------------------------------------------------

    /** 局域网主机：监听端口，等待设备连接 */
    suspend fun lanHost(
        context: Context,
        repo: NoteRepository,
        onStatus: (String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        try {
            onStatus("等待局域网设备连接…")
            ServerSocket(PORT).use { server ->
                server.accept().use { socket ->
                    onStatus("设备已连接，正在同步…")
                    doExchange(context, repo, socket.getInputStream(), socket.getOutputStream(), onStatus)
                }
            }
        } catch (e: Exception) {
            onStatus("同步失败：${e.message ?: "未知错误"}")
            -1
        }
    }

    /** 局域网客户端：连接指定 IP 的主机 */
    suspend fun lanConnect(
        context: Context,
        repo: NoteRepository,
        hostIp: String,
        onStatus: (String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        try {
            onStatus("正在连接 $hostIp …")
            Socket(hostIp, PORT).use { socket ->
                onStatus("已连接，正在同步…")
                doExchange(context, repo, socket.getInputStream(), socket.getOutputStream(), onStatus)
            }
        } catch (e: Exception) {
            onStatus("连接失败：${e.message ?: "未知错误"}")
            -1
        }
    }

    /** 蓝牙主机：等待已配对设备连接 */
    suspend fun bluetoothHost(
        context: Context,
        repo: NoteRepository,
        onStatus: (String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: run {
                onStatus("设备不支持蓝牙")
                return@withContext -1
            }
            onStatus("等待蓝牙设备连接…")
            val server = adapter.listenUsingInsecureRfcommWithServiceRecord(
                BT_SERVICE_NAME, BT_UUID
            )
            try {
                server.accept().use { socket ->
                    onStatus("设备已连接，正在同步…")
                    doExchange(context, repo, socket.inputStream, socket.outputStream, onStatus)
                }
            } finally {
                runCatching { server.close() }
            }
        } catch (e: Exception) {
            onStatus("蓝牙同步失败：${e.message ?: "未知错误"}")
            -1
        }
    }

    /** 蓝牙客户端：连接指定地址的已配对设备 */
    suspend fun bluetoothConnect(
        context: Context,
        repo: NoteRepository,
        deviceAddress: String,
        onStatus: (String) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        try {
            val adapter = BluetoothAdapter.getDefaultAdapter() ?: return@withContext -1
            val device = adapter.getRemoteDevice(deviceAddress)
            onStatus("正在连接 ${device.name ?: deviceAddress} …")
            val socket = device.createInsecureRfcommSocketToServiceRecord(BT_UUID)
            socket.use {
                it.connect()
                onStatus("已连接，正在同步…")
                doExchange(context, repo, it.inputStream, it.outputStream, onStatus)
            }
        } catch (e: Exception) {
            onStatus("连接失败：${e.message ?: "未知错误"}")
            -1
        }
    }

    /** 对称交换：发送本机备份 -> 接收对方备份 -> 合并恢复 */
    private suspend fun doExchange(
        context: Context,
        repo: NoteRepository,
        input: InputStream,
        output: OutputStream,
        onStatus: (String) -> Unit
    ): Int {
        val ownZip = export(context, repo)
            ?: run { onStatus("本机备份生成失败"); return -1 }
        val tmpZip = File(context.cacheDir, "sync_in_${System.currentTimeMillis()}.zip")
        return try {
            sendZip(output, ownZip)
            onStatus("已发送本机数据，正在接收对方数据…")
            val ok = receiveZip(input, tmpZip)
            if (!ok) {
                onStatus("接收失败")
                -1
            } else {
                onStatus("正在合并恢复…")
                val added = restoreFromFile(context, repo, tmpZip)
                onStatus("同步完成，新增笔记 $added 条")
                added
            }
        } catch (e: Exception) {
            onStatus("同步中断：${e.message ?: "未知错误"}")
            -1
        } finally {
            runCatching { tmpZip.delete() }
        }
    }

    private fun sendZip(output: OutputStream, zip: File) {
        val data = zip.readBytes()
        val out = BufferedOutputStream(output)
        out.write(intToBytes(data.size))
        out.write(data)
        out.flush()
    }

    private fun receiveZip(input: InputStream, dest: File): Boolean {
        val buffered = BufferedInputStream(input)
        val len = readInt(buffered)
        if (len <= 0 || len > MAX_TRANSFER_BYTES) return false
        val bytes = ByteArray(len)
        var read = 0
        while (read < len) {
            val r = buffered.read(bytes, read, len - read)
            if (r < 0) break
            read += r
        }
        if (read < len) return false
        dest.writeBytes(bytes)
        return true
    }

    private fun intToBytes(value: Int): ByteArray =
        byteArrayOf(
            (value ushr 24).toByte(),
            (value ushr 16).toByte(),
            (value ushr 8).toByte(),
            value.toByte()
        )

    private fun readInt(input: InputStream): Int {
        val b = ByteArray(4)
        var read = 0
        while (read < 4) {
            val r = input.read(b, read, 4 - read)
            if (r < 0) break
            read += r
        }
        if (read < 4) return -1
        return ((b[0].toInt() and 0xFF) shl 24) or
            ((b[1].toInt() and 0xFF) shl 16) or
            ((b[2].toInt() and 0xFF) shl 8) or
            (b[3].toInt() and 0xFF)
    }
}
