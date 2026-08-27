package com.muhan.notes.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * 笔记附件（图片 / 视频 / 音频）实体。
 *
 * 文件存放在应用私有目录（filesDir/media/…），这里只保存文件路径。
 * 通过外键 + CASCADE 实现「删除笔记时级联删除附件」。
 */
@Entity(
    tableName = "attachments",
    foreignKeys = [
        ForeignKey(
            entity = Note::class,
            parentColumns = ["id"],
            childColumns = ["noteId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("noteId")]
)
data class Attachment(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val noteId: Long,
    /** 附件类型：image / video / audio */
    val type: String,
    /** 附件在应用私有目录中的绝对路径 */
    val filePath: String,
    val createdAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val TYPE_IMAGE = "image"
        const val TYPE_VIDEO = "video"
        const val TYPE_AUDIO = "audio"
    }
}
