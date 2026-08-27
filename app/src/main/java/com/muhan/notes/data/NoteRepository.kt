package com.muhan.notes.data

import kotlinx.coroutines.flow.Flow

/**
 * 笔记仓库，屏蔽数据源细节。
 */
class NoteRepository(private val dao: NoteDao) {

    val notes: Flow<List<Note>> = dao.observeAll()

    suspend fun getNote(id: Long): Note? = dao.getById(id)

    suspend fun addNote(note: Note): Long = dao.insert(note)

    suspend fun updateNote(note: Note) = dao.update(note)

    suspend fun deleteNote(id: Long) = dao.deleteById(id)
}
