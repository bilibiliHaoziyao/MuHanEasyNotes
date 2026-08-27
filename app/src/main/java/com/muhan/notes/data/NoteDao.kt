package com.muhan.notes.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface NoteDao {

    /** 主列表：正常且非隐私的笔记，置顶优先，其次按更新时间倒序 */
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND isPrivate = 0 ORDER BY isPinned DESC, updatedAt DESC")
    fun observeAll(): Flow<List<Note>>

    /** 隐私中心：正常且隐私的笔记 */
    @Query("SELECT * FROM notes WHERE deletedAt IS NULL AND isPrivate = 1 ORDER BY updatedAt DESC")
    fun observePrivate(): Flow<List<Note>>

    /** 回收站：已删除（deletedAt 非空）的笔记 */
    @Query("SELECT * FROM notes WHERE deletedAt IS NOT NULL ORDER BY deletedAt DESC")
    fun observeTrashed(): Flow<List<Note>>

    /** 供恢复去重：按标题+正文精确匹配 */
    @Query("SELECT id FROM notes WHERE title = :title AND content = :content LIMIT 1")
    suspend fun findDuplicate(title: String, content: String): Long?

    @Query("SELECT * FROM notes WHERE id = :id")
    suspend fun getById(id: Long): Note?

    /** 全部笔记（含隐私与回收站），用于备份导出 */
    @Query("SELECT * FROM notes ORDER BY updatedAt DESC")
    suspend fun getAll(): List<Note>

    @Query("SELECT * FROM notes WHERE id = :id AND deletedAt IS NOT NULL")
    suspend fun getTrashedById(id: Long): Note?

    @Query("UPDATE notes SET deletedAt = :now WHERE id = :id")
    suspend fun moveToTrash(id: Long, now: Long)

    @Query("UPDATE notes SET deletedAt = NULL WHERE id = :id")
    suspend fun restoreById(id: Long)

    @Query("UPDATE notes SET isPrivate = :isPrivate WHERE id = :id")
    suspend fun setPrivate(id: Long, isPrivate: Boolean)

    @Insert
    suspend fun insert(note: Note): Long

    @Update
    suspend fun update(note: Note)

    /** 彻底删除（回收站内） */
    @Query("DELETE FROM notes WHERE id = :id")
    suspend fun deleteById(id: Long)
}
