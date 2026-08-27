package com.muhan.notes.data

import kotlinx.coroutines.flow.Flow

/**
 * 笔记仓库，屏蔽数据源细节。
 */
class NoteRepository(
    private val dao: NoteDao,
    private val attachmentDao: AttachmentDao
) {

    val notes: Flow<List<Note>> = dao.observeAll()

    suspend fun getNote(id: Long): Note? = dao.getById(id)

    suspend fun addNote(note: Note): Long = dao.insert(note)

    suspend fun updateNote(note: Note) = dao.update(note)

    suspend fun deleteNote(id: Long) = dao.deleteById(id)

    suspend fun getAttachments(noteId: Long): List<Attachment> = attachmentDao.getByNote(noteId)

    suspend fun getAttachment(id: Long): Attachment? = attachmentDao.getById(id)

    suspend fun addAttachment(attachment: Attachment): Long = attachmentDao.insert(attachment)

    suspend fun deleteAttachment(id: Long) = attachmentDao.deleteById(id)
}
