package com.example.data.model

data class BooruTag(
    val name: String,
    val category: TagCategory = TagCategory.GENERAL,
    val source: BooruSource,
    val count: Long? = null,
    val isCustom: Boolean = false
)

data class BooruPost(
    val id: Long,
    val source: BooruSource,
    val postUrl: String,
    val previewUrl: String,
    val sampleUrl: String?,
    val fileUrl: String,
    val fileExtension: String,
    val width: Int?,
    val height: Int?,
    val fileSize: Long?,
    val rating: BooruRating,
    val score: Int?,
    val tags: List<BooruTag>,
    val artist: String?,
    val sourceUrl: String?,
    val createdAt: String?,
    val isDownloaded: Boolean = false,
    val localUri: String? = null,
    val isFavorite: Boolean = false
) {
    val uniqueKey: String
        get() = "${source.name}_$id"

    val bestDisplayUrl: String
        get() = localUri ?: sampleUrl ?: fileUrl ?: previewUrl

    val highResUrl: String
        get() = fileUrl.ifEmpty { sampleUrl ?: previewUrl }
}
