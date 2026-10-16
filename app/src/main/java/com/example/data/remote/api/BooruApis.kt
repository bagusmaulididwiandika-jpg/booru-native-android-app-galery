package com.example.data.remote.api

import com.example.data.remote.dto.*
import retrofit2.http.GET
import retrofit2.http.Query

interface DanbooruApi {
    @GET("posts.json")
    suspend fun getPosts(
        @Query("tags") tags: String?,
        @Query("page") page: Int,
        @Query("limit") limit: Int = 20
    ): List<DanbooruPostDto>

    @GET("tags.json")
    suspend fun getTags(
        @Query("search[name_matches]") query: String,
        @Query("search[order]") order: String = "count",
        @Query("limit") limit: Int = 15
    ): List<DanbooruTagDto>
}

interface GelbooruApi {
    @GET("index.php?page=dapi&s=post&q=index&json=1")
    suspend fun getPosts(
        @Query("tags") tags: String?,
        @Query("pid") page: Int,
        @Query("limit") limit: Int = 20
    ): GelbooruResponseDto

    @GET("index.php?page=dapi&s=tag&q=index&json=1")
    suspend fun getTags(
        @Query("name") name: String,
        @Query("limit") limit: Int = 15
    ): GelbooruTagResponseDto
}

interface YandereApi {
    @GET("post.json")
    suspend fun getPosts(
        @Query("tags") tags: String?,
        @Query("page") page: Int,
        @Query("limit") limit: Int = 20
    ): List<YanderePostDto>

    @GET("tag.json?order=count")
    suspend fun getTags(
        @Query("name") name: String,
        @Query("limit") limit: Int = 15
    ): List<YandereTagDto>
}
