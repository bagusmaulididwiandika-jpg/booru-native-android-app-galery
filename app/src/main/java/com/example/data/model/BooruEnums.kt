package com.example.data.model

enum class BooruSource(val displayName: String, val baseUrl: String) {
    DANBOORU("Danbooru", "https://danbooru.donmai.us"),
    GELBOORU("Gelbooru", "https://gelbooru.com"),
    YANDERE("Yande.re", "https://yande.re")
}

enum class BooruRating(val label: String, val isSensitive: Boolean) {
    SAFE("Safe", false),
    QUESTIONABLE("Questionable", true),
    EXPLICIT("Explicit", true),
    UNKNOWN("Unknown", false);

    companion object {
        fun fromString(ratingStr: String?): BooruRating {
            if (ratingStr.isNullOrBlank()) return UNKNOWN
            return when (ratingStr.trim().lowercase()) {
                "s", "safe", "g", "general" -> SAFE
                "q", "questionable", "sensitive" -> QUESTIONABLE
                "e", "explicit" -> EXPLICIT
                else -> UNKNOWN
            }
        }
    }
}

enum class TagCategory(val label: String) {
    GENERAL("General"),
    ARTIST("Artist"),
    CHARACTER("Character"),
    COPYRIGHT("Copyright"),
    META("Meta"),
    USER_CUSTOM("Custom");

    companion object {
        fun fromInt(type: Int?): TagCategory {
            return when (type) {
                1 -> ARTIST
                3 -> COPYRIGHT
                4 -> CHARACTER
                5 -> META
                else -> GENERAL
            }
        }
    }
}
