package com.example

import com.example.data.model.BooruRating
import com.example.data.model.BooruSource
import com.example.data.model.TagCategory
import com.example.data.remote.dto.DanbooruPostDto
import com.example.data.remote.dto.GelbooruPostDto
import com.example.data.remote.dto.YanderePostDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class BooruParsingUnitTest {

    private lateinit var moshi: Moshi

    @Before
    fun setup() {
        moshi = Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Test
    fun testRatingMapping() {
        assertEquals(BooruRating.SAFE, BooruRating.fromString("s"))
        assertEquals(BooruRating.SAFE, BooruRating.fromString("safe"))
        assertEquals(BooruRating.SAFE, BooruRating.fromString("g"))
        assertEquals(BooruRating.SAFE, BooruRating.fromString("general"))

        assertEquals(BooruRating.QUESTIONABLE, BooruRating.fromString("q"))
        assertEquals(BooruRating.QUESTIONABLE, BooruRating.fromString("questionable"))
        assertEquals(BooruRating.QUESTIONABLE, BooruRating.fromString("sensitive"))

        assertEquals(BooruRating.EXPLICIT, BooruRating.fromString("e"))
        assertEquals(BooruRating.EXPLICIT, BooruRating.fromString("explicit"))

        assertEquals(BooruRating.UNKNOWN, BooruRating.fromString(null))
        assertEquals(BooruRating.UNKNOWN, BooruRating.fromString(""))
        assertEquals(BooruRating.UNKNOWN, BooruRating.fromString("random_value"))
    }

    @Test
    fun testDanbooruParsing() {
        val json = """
            {
                "id": 551234,
                "created_at": "2024-03-12T10:15:30.000Z",
                "score": 42,
                "source": "https://twitter.com/artist/status/123",
                "rating": "s",
                "image_width": 1920,
                "image_height": 1080,
                "tag_string": "1girl solo scenery",
                "tag_string_artist": "great_artist",
                "tag_string_character": "hatsune_miku",
                "tag_string_general": "1girl solo scenery",
                "file_url": "https://danbooru.donmai.us/data/original/123.png",
                "large_file_url": "https://danbooru.donmai.us/data/sample/123.jpg",
                "preview_file_url": "https://danbooru.donmai.us/data/preview/123.jpg",
                "file_ext": "png",
                "file_size": 2048500
            }
        """.trimIndent()

        val adapter = moshi.adapter(DanbooruPostDto::class.java)
        val dto = adapter.fromJson(json)

        assertNotNull(dto)
        assertEquals(551234L, dto?.id)
        assertEquals(42, dto?.score)
        assertEquals("s", dto?.rating)
        assertEquals(1920, dto?.imageWidth)
        assertEquals(1080, dto?.imageHeight)
        assertEquals("great_artist", dto?.tagStringArtist)
        assertEquals("hatsune_miku", dto?.tagStringCharacter)
        assertEquals("png", dto?.fileExt)
    }

    @Test
    fun testGelbooruParsing() {
        val json = """
            {
                "id": 987654,
                "created_at": "Mon Mar 11 08:30:00 -0500 2024",
                "score": 15,
                "width": 1280,
                "height": 720,
                "md5": "abcdef1234567890",
                "rating": "general",
                "source": "https://pixiv.net/artworks/999",
                "tags": "scenery landscape sunset sky cloud",
                "file_url": "https://img3.gelbooru.com/images/ab/cd/abcdef.jpg",
                "preview_url": "https://img3.gelbooru.com/thumbnails/ab/cd/thumbnail_abcdef.jpg",
                "sample_url": "https://img3.gelbooru.com/samples/ab/cd/sample_abcdef.jpg"
            }
        """.trimIndent()

        val adapter = moshi.adapter(GelbooruPostDto::class.java)
        val dto = adapter.fromJson(json)

        assertNotNull(dto)
        assertEquals(987654L, dto?.id)
        assertEquals(15, dto?.score)
        assertEquals("general", dto?.rating)
        assertEquals(1280, dto?.width)
        assertEquals(720, dto?.height)
        assertTrue(dto?.tags?.contains("landscape") == true)
    }

    @Test
    fun testYandereParsing() {
        val json = """
            {
                "id": 112233,
                "tags": "1girl cat_ears smile ribbon",
                "created_at": 1710150000,
                "author": "cute_illustrator",
                "source": "https://twitter.com/illustrator",
                "score": 88,
                "file_size": 4500000,
                "file_ext": "jpg",
                "file_url": "https://files.yande.re/image/original.jpg",
                "preview_url": "https://assets.yande.re/data/preview.jpg",
                "sample_url": "https://files.yande.re/sample/sample.jpg",
                "jpeg_url": "https://files.yande.re/jpeg/jpeg.jpg",
                "rating": "q",
                "width": 3840,
                "height": 2160
            }
        """.trimIndent()

        val adapter = moshi.adapter(YanderePostDto::class.java)
        val dto = adapter.fromJson(json)

        assertNotNull(dto)
        assertEquals(112233L, dto?.id)
        assertEquals("q", dto?.rating)
        assertEquals("cute_illustrator", dto?.author)
        assertEquals(3840, dto?.width)
        assertEquals(2160, dto?.height)
        assertEquals("jpg", dto?.fileExt)
    }

    @Test
    fun testTagCategoryMapping() {
        assertEquals(TagCategory.ARTIST, TagCategory.fromInt(1))
        assertEquals(TagCategory.COPYRIGHT, TagCategory.fromInt(3))
        assertEquals(TagCategory.CHARACTER, TagCategory.fromInt(4))
        assertEquals(TagCategory.META, TagCategory.fromInt(5))
        assertEquals(TagCategory.GENERAL, TagCategory.fromInt(0))
        assertEquals(TagCategory.GENERAL, TagCategory.fromInt(99))
    }
}
