package com.example.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class DanbooruPostDto(
    val id: Long,
    @Json(name = "created_at") val createdAt: String? = null,
    val score: Int? = 0,
    val source: String? = null,
    val rating: String? = null,
    @Json(name = "image_width") val imageWidth: Int? = null,
    @Json(name = "image_height") val imageHeight: Int? = null,
    @Json(name = "tag_string") val tagString: String? = null,
    @Json(name = "tag_string_general") val tagStringGeneral: String? = null,
    @Json(name = "tag_string_character") val tagStringCharacter: String? = null,
    @Json(name = "tag_string_copyright") val tagStringCopyright: String? = null,
    @Json(name = "tag_string_artist") val tagStringArtist: String? = null,
    @Json(name = "tag_string_meta") val tagStringMeta: String? = null,
    @Json(name = "file_url") val fileUrl: String? = null,
    @Json(name = "large_file_url") val largeFileUrl: String? = null,
    @Json(name = "preview_file_url") val previewFileUrl: String? = null,
    @Json(name = "file_ext") val fileExt: String? = null,
    @Json(name = "file_size") val fileSize: Long? = null,
    val md5: String? = null
)

@JsonClass(generateAdapter = true)
data class DanbooruTagDto(
    val id: Long? = null,
    val name: String,
    @Json(name = "post_count") val postCount: Long? = null,
    val category: Int? = null
)

@JsonClass(generateAdapter = true)
data class GelbooruResponseDto(
    @Json(name = "post") val post: List<GelbooruPostDto>? = null
)

@JsonClass(generateAdapter = true)
data class GelbooruPostDto(
    val id: Long,
    @Json(name = "created_at") val createdAt: String? = null,
    val score: Int? = 0,
    val width: Int? = null,
    val height: Int? = null,
    val md5: String? = null,
    val directory: String? = null,
    val image: String? = null,
    val rating: String? = null,
    val source: String? = null,
    val tags: String? = null,
    @Json(name = "file_url") val fileUrl: String? = null,
    @Json(name = "preview_url") val previewUrl: String? = null,
    @Json(name = "sample_url") val sampleUrl: String? = null
)

@JsonClass(generateAdapter = true)
data class GelbooruTagDto(
    val id: Long? = null,
    val tag: String? = null,
    val count: Long? = null,
    val type: String? = null
)

@JsonClass(generateAdapter = true)
data class GelbooruTagResponseDto(
    val tag: List<GelbooruTagDto>? = null
)

@JsonClass(generateAdapter = true)
data class YanderePostDto(
    val id: Long,
    val tags: String? = null,
    @Json(name = "created_at") val createdAt: Long? = null,
    val author: String? = null,
    val source: String? = null,
    val score: Int? = 0,
    val md5: String? = null,
    @Json(name = "file_size") val fileSize: Long? = null,
    @Json(name = "file_ext") val fileExt: String? = null,
    @Json(name = "file_url") val fileUrl: String? = null,
    @Json(name = "preview_url") val previewUrl: String? = null,
    @Json(name = "sample_url") val sampleUrl: String? = null,
    @Json(name = "jpeg_url") val jpegUrl: String? = null,
    val rating: String? = null,
    val width: Int? = null,
    val height: Int? = null
)

@JsonClass(generateAdapter = true)
data class YandereTagDto(
    val id: Long? = null,
    val name: String,
    val count: Long? = null,
    val type: Int? = null
)
