package com.example.rubix.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

enum class NodeType {
    FOLDER,
    IMAGE,
    NOTE,
    PDF
}

@Entity(tableName = "nodes")
data class NodeEntity(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val parentId: String? = null,
    val type: NodeType,
    val title: String,
    val content: String? = null,
    val creationDate: Long = System.currentTimeMillis(),
    val aspectRatio: Float = 1.0f,
    val sortOrder: Long = System.currentTimeMillis(),

    // Performance Columns
    val thumbnailPath: String? = null,
    val previewPath: String? = null,
    val originalPath: String? = null,
    val dominantColor: Int? = null,
    
    // Lifecycle Columns
    val isArchived: Boolean = false,
    val isTrashed: Boolean = false,
    val isPinned: Boolean = false
)

