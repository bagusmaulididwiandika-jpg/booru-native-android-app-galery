package com.example.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "posts",
    indices = [
        Index(value = ["source", "postId"], unique = true),
        Index(value = ["isFavorite"]),
        Index(value = ["isDownloaded"])
    ]
)
data class PostEntity(
    @PrimaryKey val key: String,
    val postId: Long,
    val source: String,
    val postUrl: String,
    val previewUrl: String,
    val sampleUrl: String?,
    val fileUrl: String,
    val fileExtension: String,
    val width: Int?,
    val height: Int?,
    val fileSize: Long?,
    val rating: String,
    val score: Int?,
    val artist: String?,
    val sourceUrl: String?,
    val createdAt: String?,
    val localUri: String? = null,
    val isDownloaded: Boolean = false,
    val isFavorite: Boolean = false,
    val savedAtTimestamp: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "tags",
    indices = [
        Index(value = ["name"]),
        Index(value = ["source", "name"], unique = true)
    ]
)
data class TagEntity(
    @PrimaryKey val tagKey: String,
    val name: String,
    val category: String,
    val source: String,
    val count: Long?,
    val isCustom: Boolean = false
)

@Entity(
    tableName = "post_tags",
    primaryKeys = ["postKey", "tagKey"],
    indices = [
        Index(value = ["tagKey"]),
        Index(value = ["postKey"])
    ]
)
data class PostTagCrossRef(
    val postKey: String,
    val tagKey: String
)

@Entity(
    tableName = "collections",
    indices = [Index(value = ["name"], unique = true)]
)
data class CollectionEntity(
    @PrimaryKey(autoGenerate = true) val collectionId: Long = 0,
    val name: String,
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val coverUri: String? = null
)

@Entity(
    tableName = "collection_posts",
    primaryKeys = ["collectionId", "postKey"],
    indices = [Index(value = ["postKey"])]
)
data class CollectionPostCrossRef(
    val collectionId: Long,
    val postKey: String
)

@Entity(
    tableName = "download_tasks",
    indices = [Index(value = ["status"])]
)
data class DownloadTaskEntity(
    @PrimaryKey val downloadId: String,
    val postKey: String,
    val postId: Long,
    val source: String,
    val previewUrl: String,
    val downloadUrl: String,
    val fileName: String,
    val status: String,
    val progressPercent: Int = 0,
    val bytesDownloaded: Long = 0,
    val totalBytes: Long = 0,
    val targetUri: String? = null,
    val errorMessage: String? = null,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "search_history")
data class SearchHistoryEntity(
    @PrimaryKey val query: String,
    val timestamp: Long = System.currentTimeMillis()
)
