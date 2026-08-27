package com.muhan.notes

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.muhan.notes.data.Note
import com.muhan.notes.data.NoteDatabase
import com.muhan.notes.data.NoteRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class NoteViewModel(private val repository: NoteRepository) : ViewModel() {

    /** 全部笔记：置顶优先、按更新时间倒序 */
    val notes: StateFlow<List<Note>> = repository.notes.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = emptyList()
    )

    suspend fun getNote(id: Long): Note? = repository.getNote(id)

    /**
     * 统一的新建/更新：已存在则更新，否则插入。
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
        return if (existing == null) {
            repository.addNote(
                Note(title = t, content = c, color = color, isPinned = isPinned)
            )
        } else {
            val updated = existing.copy(
                title = t,
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
        viewModelScope.launch { repository.deleteNote(id) }
    }

    fun togglePin(note: Note) {
        viewModelScope.launch {
            repository.updateNote(note.copy(isPinned = !note.isPinned, updatedAt = System.currentTimeMillis()))
        }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val app = this[APPLICATION_KEY]!!
                NoteViewModel(NoteRepository(NoteDatabase.getInstance(app).noteDao()))
            }
        }
    }
}
