package com.example.data.local.dao

import androidx.room.*
import com.example.data.local.entity.*
import kotlinx.coroutines.flow.Flow

data class PostWithTags(
    @Embedded val post: PostEntity,
    @Relation(
        parentColumn = "key",
        entityColumn = "tagKey",
        associateBy = Junction(
            value = PostTagCrossRef::class,
            parentColumn = "postKey",
            entityColumn = "tagKey"
        )
    )
    val tags: List<TagEntity>
)

data class CollectionWithPosts(
    @Embedded val collection: CollectionEntity,
    @Relation(
        parentColumn = "collectionId",
        entityColumn = "key",
        associateBy = Junction(
            value = CollectionPostCrossRef::class,
            parentColumn = "collectionId",
            entityColumn = "postKey"
        )
    )
    val posts: List<PostEntity>
)

@Dao
interface PostDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPost(post: PostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosts(posts: List<PostEntity>)

    @Query("SELECT * FROM posts WHERE `key` = :key LIMIT 1")
    suspend fun getPostByKey(key: String): PostEntity?

    @Transaction
    @Query("SELECT * FROM posts WHERE `key` = :key LIMIT 1")
    fun getPostWithTags(key: String): Flow<PostWithTags?>

    @Transaction
    @Query("SELECT * FROM posts WHERE isDownloaded = 1 ORDER BY savedAtTimestamp DESC")
    fun getDownloadedPosts(): Flow<List<PostWithTags>>

    @Transaction
    @Query("SELECT * FROM posts WHERE isFavorite = 1 ORDER BY savedAtTimestamp DESC")
    fun getFavoritePosts(): Flow<List<PostWithTags>>

    @Query("UPDATE posts SET isFavorite = :isFavorite WHERE `key` = :key")
    suspend fun setFavorite(key: String, isFavorite: Boolean)

    @Query("UPDATE posts SET isDownloaded = :isDownloaded, localUri = :localUri WHERE `key` = :key")
    suspend fun setDownloaded(key: String, isDownloaded: Boolean, localUri: String?)

    @Query("DELETE FROM posts WHERE `key` = :key")
    suspend fun deletePost(key: String)

    @Transaction
    @Query("""
        SELECT p.* FROM posts p
        INNER JOIN post_tags pt ON p.`key` = pt.postKey
        INNER JOIN tags t ON pt.tagKey = t.tagKey
        WHERE t.name LIKE '%' || :tagName || '%'
        ORDER BY p.savedAtTimestamp DESC
    """)
    fun searchDownloadedPostsByTag(tagName: String): Flow<List<PostEntity>>
}

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTags(tags: List<TagEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPostTagCrossRefs(crossRefs: List<PostTagCrossRef>)

    @Query("SELECT * FROM tags WHERE name LIKE :query || '%' ORDER BY count DESC LIMIT 20")
    suspend fun getAutocompleteTags(query: String): List<TagEntity>

    @Query("DELETE FROM post_tags WHERE postKey = :postKey")
    suspend fun clearTagsForPost(postKey: String)
}

@Dao
interface CollectionDao {
    @Query("SELECT * FROM collections ORDER BY createdAt DESC")
    fun getAllCollections(): Flow<List<CollectionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCollection(collection: CollectionEntity): Long

    @Query("DELETE FROM collections WHERE collectionId = :id")
    suspend fun deleteCollection(id: Long)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addPostToCollection(crossRef: CollectionPostCrossRef)

    @Query("DELETE FROM collection_posts WHERE collectionId = :collectionId AND postKey = :postKey")
    suspend fun removePostFromCollection(collectionId: Long, postKey: String)

    @Transaction
    @Query("SELECT * FROM collections WHERE collectionId = :id LIMIT 1")
    fun getCollectionWithPosts(id: Long): Flow<CollectionWithPosts?>

    @Query("SELECT COUNT(*) FROM collection_posts WHERE collectionId = :collectionId")
    suspend fun getPostCount(collectionId: Long): Int
}

@Dao
interface DownloadDao {
    @Query("SELECT * FROM download_tasks ORDER BY createdAt DESC")
    fun getAllDownloadTasks(): Flow<List<DownloadTaskEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: DownloadTaskEntity)

    @Update
    suspend fun updateTask(task: DownloadTaskEntity)

    @Query("UPDATE download_tasks SET status = :status, progressPercent = :progress, bytesDownloaded = :downloaded, totalBytes = :total WHERE downloadId = :id")
    suspend fun updateProgress(id: String, status: String, progress: Int, downloaded: Long, total: Long)

    @Query("UPDATE download_tasks SET status = :status, targetUri = :uri WHERE downloadId = :id")
    suspend fun setCompleted(id: String, status: String, uri: String)

    @Query("UPDATE download_tasks SET status = :status, errorMessage = :error WHERE downloadId = :id")
    suspend fun setFailed(id: String, status: String, error: String)

    @Query("DELETE FROM download_tasks WHERE downloadId = :id")
    suspend fun deleteTask(id: String)

    @Query("DELETE FROM download_tasks WHERE status = 'COMPLETED'")
    suspend fun clearCompletedTasks()
}

@Dao
interface SearchHistoryDao {
    @Query("SELECT * FROM search_history ORDER BY timestamp DESC LIMIT 30")
    fun getSearchHistory(): Flow<List<SearchHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSearch(entry: SearchHistoryEntity)

    @Query("DELETE FROM search_history WHERE `query` = :query")
    suspend fun deleteSearch(query: String)

    @Query("DELETE FROM search_history")
    suspend fun clearSearchHistory()
}
