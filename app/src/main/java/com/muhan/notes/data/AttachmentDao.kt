package com.muhan.notes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query

@Dao
interface AttachmentDao {

    /** 某条笔记的全部附件，按添加时间正序 */
    @Query("SELECT * FROM attachments WHERE noteId = :noteId ORDER BY createdAt ASC, id ASC")
    suspend fun getByNote(noteId: Long): List<Attachment>

    @Query("SELECT * FROM attachments WHERE id = :id")
    suspend fun getById(id: Long): Attachment?

    /** 全部附件（用于备份导出） */
    @Query("SELECT * FROM attachments ORDER BY id ASC")
    suspend fun getAll(): List<Attachment>

    @Insert
    suspend fun insert(attachment: Attachment): Long

    @Query("DELETE FROM attachments WHERE id = :id")
    suspend fun deleteById(id: Long)
}
