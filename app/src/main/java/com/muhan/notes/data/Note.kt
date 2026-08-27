package com.muhan.notes.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * 笔记实体，对应数据库中 notes 表。
 *
 * @property color 笔记的强调色（ARGB 值），用于列表展示与编辑页配色
 * @property isPinned 是否置顶（小米便签中的“收藏/置顶”）
 */
@Entity(tableName = "notes")
data class Note(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val title: String = "",
    val content: String = "",
    val color: Long = DEFAULT_COLOR,
    val isPinned: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        /** 默认笔记颜色：暖黄色 */
        const val DEFAULT_COLOR = 0xFFFFC107L
    }
}
