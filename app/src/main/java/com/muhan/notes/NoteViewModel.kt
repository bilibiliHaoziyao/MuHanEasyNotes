package com.muhan.notes

import android.app.Application
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.muhan.notes.data.Attachment
import com.muhan.notes.data.AttachmentStorage
import com.muhan.notes.data.AudioRecorder
import com.muhan.notes.data.BackupManager
import com.muhan.notes.data.Note
import com.muhan.notes.data.NoteDatabase
import com.muhan.notes.data.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

class NoteViewModel(
    private val context: Application,
    private val repository: NoteRepository
) : ViewModel() {

    /** 主列表：正常且非隐私的笔记，置顶优先、按更新时间倒序 */
    val notes: StateFlow<List<Note>> = repository.notes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    /** 隐私中心：正常且隐私的笔记 */
    val privateNotes: StateFlow<List<Note>> = repository.privateNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    /** 回收站：已删除的笔记 */
    val trashedNotes: StateFlow<List<Note>> = repository.trashedNotes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    // ------------------------------------------------------------------
    // 软件内录音
    // ------------------------------------------------------------------

    private var recorder: AudioRecorder? = null

    fun audioRecorder(): AudioRecorder = recorder ?: AudioRecorder(context).also { recorder = it }

    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    fun startRecording() {
        _isRecording.value = audioRecorder().start()
    }

    fun stopRecording(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val path = audioRecorder().stop()
            _isRecording.value = false
            onResult(path)
        }
    }

    fun cancelRecording() {
        audioRecorder().cancel()
        _isRecording.value = false
    }

    // ------------------------------------------------------------------
    // 笔记基础操作
    // ------------------------------------------------------------------

    suspend fun getNote(id: Long): Note? = repository.getNote(id)

    suspend fun getAttachments(noteId: Long): List<Attachment> = repository.getAttachments(noteId)

    suspend fun saveNote(
        existing: Note?,
        title: String,
        content: String,
        color: Long,
        isPinned: Boolean
    ): Long {
        val t = title.trim()
        val c = content.trim()
        // 无标题时以正文第一行（截断 60 字）作为标题
        val effectiveTitle = t.ifBlank {
            c.lineSequence().firstOrNull()?.trim()?.take(60) ?: ""
        }
        return if (existing == null) {
            repository.addNote(
                Note(title = effectiveTitle, content = c, color = color, isPinned = isPinned)
            )
        } else {
            val updated = existing.copy(
                title = effectiveTitle,
                content = c,
                color = color,
                isPinned = isPinned,
                updatedAt = System.currentTimeMillis()
            )
            repository.updateNote(updated)
            updated.id
        }
    }

    /** 删除笔记：移入回收站（软删除，附件保留） */
    fun deleteNote(id: Long) {
        viewModelScope.launch { repository.moveToTrash(id) }
    }

    /** 彻底删除（回收站内）：先删附件文件，再删记录（attachments 行级联删除） */
    fun purgeNote(id: Long) {
        viewModelScope.launch {
            repository.getAttachments(id).forEach { AttachmentStorage.deleteFile(it.filePath) }
            repository.deleteNote(id)
        }
    }

    /** 从回收站恢复 */
    fun restoreFromTrash(id: Long) {
        viewModelScope.launch { repository.restoreFromTrash(id) }
    }

    /** 加入 / 移出隐私中心 */
    fun setPrivate(id: Long, isPrivate: Boolean) {
        viewModelScope.launch { repository.setPrivate(id, isPrivate) }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }

    fun touchNote(id: Long) {
        viewModelScope.launch {
            val note = repository.getNote(id) ?: return@launch
            repository.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    fun addAttachment(noteId: Long, type: String, uri: Uri) {
        viewModelScope.launch {
            val path = AttachmentStorage.copyFromUri(context, type, uri) ?: return@launch
            repository.addAttachment(
                Attachment(noteId = noteId, type = type, filePath = path)
            )
        }
    }

    fun addLocalAttachment(noteId: Long, type: String, filePath: String) {
        viewModelScope.launch {
            if (filePath.isBlank()) return@launch
            repository.addAttachment(
                Attachment(noteId = noteId, type = type, filePath = filePath)
            )
        }
    }

    fun deleteAttachment(id: Long) {
        viewModelScope.launch {
            val attachment = repository.getAttachment(id) ?: return@launch
            AttachmentStorage.deleteFile(attachment.filePath)
            repository.deleteAttachment(id)
        }
    }

    // ------------------------------------------------------------------
    // 本地备份 / 恢复
    // ------------------------------------------------------------------

    /** 执行本地备份，完成后回传 zip 文件（失败为 null） */
    fun exportBackup(onDone: (File?) -> Unit) {
        viewModelScope.launch { onDone(BackupManager.export(context, repository)) }
    }

    /** 列出应用 backups 目录下的历史备份 */
    fun listLocalBackups(): List<File> =
        File(context.filesDir, "backups")
            .listFiles { f -> f.isFile && f.name.endsWith(".zip") }
            ?.sortedByDescending { it.lastModified() }
            ?: emptyList()

    fun restoreLocalBackup(file: File, onDone: (Int) -> Unit) {
        viewModelScope.launch {
            onDone(BackupManager.restoreFromFile(context, repository, file))
        }
    }

    fun restoreFromUri(uri: Uri, onDone: (Int) -> Unit) {
        viewModelScope.launch {
            onDone(BackupManager.restoreFromUri(context, repository, context.contentResolver, uri))
        }
    }

    fun writeExportZipTo(out: java.io.OutputStream, onDone: (Boolean) -> Unit) {
        viewModelScope.launch {
            onDone(BackupManager.writeExportZip(context, repository, out))
        }
    }

    // ------------------------------------------------------------------
    // WebDAV 备份 / 恢复
    // ------------------------------------------------------------------

    fun webdavUpload(url: String, user: String, pass: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val zip = BackupManager.export(context, repository)
            if (zip == null) {
                onDone("备份生成失败")
                return@launch
            }
            onDone(
                if (BackupManager.webdavUpload(url, user, pass, zip)) "上传成功"
                else "上传失败，请检查地址与账号"
            )
        }
    }

    fun webdavDownload(url: String, user: String, pass: String, onDone: (String) -> Unit) {
        viewModelScope.launch {
            val dest = File(context.cacheDir, "webdav_backup.zip")
            if (!BackupManager.webdavDownload(url, user, pass, dest)) {
                onDone("下载失败，请检查地址与账号")
                return@launch
            }
            val added = BackupManager.restoreFromFile(context, repository, dest)
            dest.delete()
            onDone("恢复完成，新增笔记 $added 条")
        }
    }

    // ------------------------------------------------------------------
    // 多设备同步（局域网 / 蓝牙）
    // ------------------------------------------------------------------

    private val _syncStatus = MutableStateFlow("")
    val syncStatus: StateFlow<String> = _syncStatus.asStateFlow()

    fun lanHost() {
        viewModelScope.launch {
            _syncStatus.value = "正在启动…"
            BackupManager.lanHost(context, repository) { _syncStatus.value = it }
        }
    }

    fun lanConnect(hostIp: String) {
        viewModelScope.launch {
            _syncStatus.value = "正在启动…"
            BackupManager.lanConnect(context, repository, hostIp) { _syncStatus.value = it }
        }
    }

    fun bluetoothHost() {
        viewModelScope.launch {
            _syncStatus.value = "正在启动…"
            BackupManager.bluetoothHost(context, repository) { _syncStatus.value = it }
        }
    }

    fun bluetoothConnect(address: String) {
        viewModelScope.launch {
            _syncStatus.value = "正在启动…"
            BackupManager.bluetoothConnect(context, repository, address) { _syncStatus.value = it }
        }
    }

    fun resetSyncStatus() {
        _syncStatus.value = ""
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                val db = NoteDatabase.getInstance(app)
                NoteViewModel(
                    context = app,
                    repository = NoteRepository(db.noteDao(), db.attachmentDao())
                )
            }
        }
    }
}
