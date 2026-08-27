package com.muhan.notes.data

import kotlinx.coroutines.flow.Flow

/**
 * 笔记仓库，屏蔽数据源细节。
 */
class NoteRepository(
    private val dao: NoteDao,
    private val attachmentDao: AttachmentDao
) {

    /** 主列表：正常且非隐私的笔记 */
    val notes: Flow<List<Note>> = dao.observeAll()

    /** 隐私中心：正常且隐私的笔记 */
    val privateNotes: Flow<List<Note>> = dao.observePrivate()

    /** 回收站：已删除的笔记 */
    val trashedNotes: Flow<List<Note>> = dao.observeTrashed()

    suspend fun getNote(id: Long): Note? = dao.getById(id)

    /** 全部笔记（含隐私与回收站），用于备份 */
    suspend fun getAllNotes(): List<Note> = dao.getAll()

    suspend fun addNote(note: Note): Long = dao.insert(note)

    suspend fun updateNote(note: Note) = dao.update(note)

    /** 彻底删除（回收站内使用） */
    suspend fun deleteNote(id: Long) = dao.deleteById(id)

    suspend fun moveToTrash(id: Long, now: Long = System.currentTimeMillis()) = dao.moveToTrash(id, now)

    suspend fun restoreFromTrash(id: Long) = dao.restoreById(id)

    suspend fun setPrivate(id: Long, isPrivate: Boolean) = dao.setPrivate(id, isPrivate)

    suspend fun findDuplicate(title: String, content: String): Long? = dao.findDuplicate(title, content)

    suspend fun getTrashedById(id: Long): Note? = dao.getTrashedById(id)

    suspend fun getAttachments(noteId: Long): List<Attachment> = attachmentDao.getByNote(noteId)

    suspend fun getAllAttachments(): List<Attachment> = attachmentDao.getAll()

    suspend fun getAttachment(id: Long): Attachment? = attachmentDao.getById(id)

    suspend fun addAttachment(attachment: Attachment): Long = attachmentDao.insert(attachment)

    suspend fun deleteAttachment(id: Long) = attachmentDao.deleteById(id)
}
