package com.example.data.repository

import com.example.data.local.AppDatabase
import com.example.data.local.dao.PostWithTags
import com.example.data.local.entity.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class LocalGalleryRepository(private val db: AppDatabase) {
    private val postDao = db.postDao()
    private val tagDao = db.tagDao()
    private val collectionDao = db.collectionDao()
    private val searchHistoryDao = db.searchHistoryDao()

    fun getDownloadedPosts(): Flow<List<BooruPost>> {
        return postDao.getDownloadedPosts().map { list ->
            list.map { it.toBooruPost() }
        }
    }

    fun getFavoritePosts(): Flow<List<BooruPost>> {
        return postDao.getFavoritePosts().map { list ->
            list.map { it.toBooruPost() }
        }
    }

    fun getPostWithTags(key: String): Flow<BooruPost?> {
        return postDao.getPostWithTags(key).map { it?.toBooruPost() }
    }

    suspend fun getPostByKey(key: String): BooruPost? {
        val entity = postDao.getPostByKey(key) ?: return null
        return entity.toBooruPost(emptyList())
    }

    suspend fun toggleFavorite(key: String, isFavorite: Boolean) {
        postDao.setFavorite(key, isFavorite)
    }

    suspend fun deletePost(key: String) {
        postDao.deletePost(key)
    }

    suspend fun importTagsForPost(post: BooruPost) {
        val postKey = post.uniqueKey
        val postEntity = post.toEntity()
        postDao.insertPost(postEntity)

        if (post.tags.isNotEmpty()) {
            val tagEntities = post.tags.map { tag ->
                TagEntity(
                    tagKey = "${tag.source.name}_${tag.name}",
                    name = tag.name,
                    category = tag.category.name,
                    source = tag.source.name,
                    count = tag.count,
                    isCustom = tag.isCustom
                )
            }
            val crossRefs = post.tags.map { tag ->
                PostTagCrossRef(
                    postKey = postKey,
                    tagKey = "${tag.source.name}_${tag.name}"
                )
            }
            tagDao.insertTags(tagEntities)
            tagDao.insertPostTagCrossRefs(crossRefs)
        }
    }

    suspend fun addCustomTagToPost(postKey: String, source: BooruSource, tagName: String) {
        val cleanName = tagName.trim().lowercase()
        if (cleanName.isBlank()) return
        val tagKey = "${source.name}_$cleanName"
        val tagEntity = TagEntity(
            tagKey = tagKey,
            name = cleanName,
            category = TagCategory.USER_CUSTOM.name,
            source = source.name,
            count = 1,
            isCustom = true
        )
        tagDao.insertTags(listOf(tagEntity))
        tagDao.insertPostTagCrossRefs(listOf(PostTagCrossRef(postKey = postKey, tagKey = tagKey)))
    }

    // Collections
    fun getAllCollections(): Flow<List<CollectionEntity>> = collectionDao.getAllCollections()

    suspend fun createCollection(name: String, description: String = ""): Long {
        return collectionDao.insertCollection(
            CollectionEntity(name = name.trim(), description = description.trim())
        )
    }

    suspend fun deleteCollection(collectionId: Long) {
        collectionDao.deleteCollection(collectionId)
    }

    suspend fun addPostToCollection(collectionId: Long, postKey: String) {
        collectionDao.addPostToCollection(CollectionPostCrossRef(collectionId, postKey))
    }

    suspend fun removePostFromCollection(collectionId: Long, postKey: String) {
        collectionDao.removePostFromCollection(collectionId, postKey)
    }

    fun getCollectionWithPosts(collectionId: Long): Flow<List<BooruPost>> {
        return collectionDao.getCollectionWithPosts(collectionId).map { collectionWithPosts ->
            collectionWithPosts?.posts?.map { it.toBooruPost(emptyList()) } ?: emptyList()
        }
    }

    // Search History
    fun getSearchHistory(): Flow<List<SearchHistoryEntity>> = searchHistoryDao.getSearchHistory()

    suspend fun recordSearch(query: String) {
        if (query.isNotBlank()) {
            searchHistoryDao.insertSearch(SearchHistoryEntity(query.trim()))
        }
    }

    suspend fun deleteSearchHistoryItem(query: String) {
        searchHistoryDao.deleteSearch(query)
    }

    suspend fun clearSearchHistory() {
        searchHistoryDao.clearSearchHistory()
    }

    private fun PostWithTags.toBooruPost(): BooruPost {
        val tagList = tags.map { tagEntity ->
            BooruTag(
                name = tagEntity.name,
                category = runCatching { TagCategory.valueOf(tagEntity.category) }.getOrDefault(TagCategory.GENERAL),
                source = runCatching { BooruSource.valueOf(tagEntity.source) }.getOrDefault(BooruSource.DANBOORU),
                count = tagEntity.count,
                isCustom = tagEntity.isCustom
            )
        }
        return post.toBooruPost(tagList)
    }

    private fun PostEntity.toBooruPost(tagList: List<BooruTag>): BooruPost {
        val src = runCatching { BooruSource.valueOf(source) }.getOrDefault(BooruSource.DANBOORU)
        return BooruPost(
            id = postId,
            source = src,
            postUrl = postUrl,
            previewUrl = previewUrl,
            sampleUrl = sampleUrl,
            fileUrl = fileUrl,
            fileExtension = fileExtension,
            width = width,
            height = height,
            fileSize = fileSize,
            rating = BooruRating.fromString(rating),
            score = score,
            tags = tagList,
            artist = artist,
            sourceUrl = sourceUrl,
            createdAt = createdAt,
            isDownloaded = isDownloaded,
            localUri = localUri,
            isFavorite = isFavorite
        )
    }

    private fun BooruPost.toEntity(): PostEntity {
        return PostEntity(
            key = uniqueKey,
            postId = id,
            source = source.name,
            postUrl = postUrl,
            previewUrl = previewUrl,
            sampleUrl = sampleUrl,
            fileUrl = fileUrl,
            fileExtension = fileExtension,
            width = width,
            height = height,
            fileSize = fileSize,
            rating = rating.name,
            score = score,
            artist = artist,
            sourceUrl = sourceUrl,
            createdAt = createdAt,
            localUri = localUri,
            isDownloaded = isDownloaded,
            isFavorite = isFavorite
        )
    }
}
