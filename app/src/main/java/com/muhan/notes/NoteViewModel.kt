package com.muhan.notes

import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.muhan.notes.data.Attachment
import com.muhan.notes.data.AttachmentStorage
import com.muhan.notes.data.AudioRecorder
import com.muhan.notes.data.Note
import com.muhan.notes.data.NoteDatabase
import com.muhan.notes.data.NoteRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteViewModel(
    private val context: Application,
    private val repository: NoteRepository
) : ViewModel() {

    /** 全部笔记：置顶优先、按更新时间倒序 */
    val notes: StateFlow<List<Note>> = repository.notes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    /** 软件内录音器（按需创建，进程存活期间可复用） */
    private var recorder: AudioRecorder? = null

    fun audioRecorder(): AudioRecorder = recorder ?: AudioRecorder(context).also { recorder = it }

    /** 是否正在录音（供界面显示录音中状态） */
    private val _isRecording = MutableStateFlow(false)
    val isRecording: StateFlow<Boolean> = _isRecording.asStateFlow()

    /** 开始软件内录音 */
    fun startRecording() {
        _isRecording.value = audioRecorder().start()
    }

    /**
     * 停止录音。录音文件写入应用私有目录后，通过 [onResult] 回传路径（失败为 null），
     * 由界面决定是否挂到当前笔记上。
     */
    fun stopRecording(onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val path = audioRecorder().stop()
            _isRecording.value = false
            onResult(path)
        }
    }

    /** 放弃当前录音（删除临时文件） */
    fun cancelRecording() {
        audioRecorder().cancel()
        _isRecording.value = false
    }

    suspend fun getNote(id: Long): Note? = repository.getNote(id)

    suspend fun getAttachments(noteId: Long): List<Attachment> = repository.getAttachments(noteId)

    /**
     * 统一的新建/更新：已存在则更新，否则插入。
     * 标题可选：未填写标题时，自动取正文第一句话作为标题。
     * 返回保存后笔记的 id。
     */
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

    fun deleteNote(id: Long) {
        viewModelScope.launch {
            // 先删物理文件，再删数据库记录（attachments 行由外键级联删除）
            repository.getAttachments(id).forEach { AttachmentStorage.deleteFile(it.filePath) }
            repository.deleteNote(id)
        }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }

    /** 触碰笔记：仅刷新 updatedAt（添加附件后让笔记在列表中置前） */
    fun touchNote(id: Long) {
        viewModelScope.launch {
            val note = repository.getNote(id) ?: return@launch
            repository.updateNote(note.copy(updatedAt = System.currentTimeMillis()))
        }
    }

    /** 复制外部 Uri 媒体到应用私有目录并写入附件记录 */
    fun addAttachment(noteId: Long, type: String, uri: Uri) {
        viewModelScope.launch {
            val path = AttachmentStorage.copyFromUri(context, type, uri) ?: return@launch
            repository.addAttachment(
                Attachment(noteId = noteId, type = type, filePath = path)
            )
        }
    }

    /** 新增一条已经录好/保存好的本地媒体文件 */
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
