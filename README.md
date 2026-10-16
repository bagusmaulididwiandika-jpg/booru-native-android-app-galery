# BooruVault — Native Android Image Downloader & Gallery

BooruVault is a modern, high-performance native Android application engineered in Kotlin with Jetpack Compose and Material 3 for browsing, searching, downloading, tagging, and organizing anime artwork from **Danbooru**, **Gelbooru**, and **Yande.re** with offline local gallery persistence.

---

## Features

1. **Multi-Source REST API Integration**:
   - **Danbooru**: Direct queries to `https://danbooru.donmai.us/posts.json` with tag category breakdowns (Artist, Character, Copyright, Meta, General).
   - **Gelbooru**: Native queries to `https://gelbooru.com/index.php?page=dapi&s=post&q=index&json=1`.
   - **Yande.re**: High-resolution queries to `https://yande.re/post.json`.
   - Multi-source search with asynchronous coroutines and deduplication.

2. **Tag Importer & Local Taxonomy**:
   - Imports tags directly from API responses into local **Room Database**.
   - Preserves original server tags, categories, and post-tag relations.
   - Allows users to add custom local tags without modifying server metadata.
   - Single-tap tag copy, "Copy All Tags", and "Export Metadata (JSON)".

3. **Background Image Downloader**:
   - Asynchronous download queue with live byte-level progress reporting and percentage indicators.
   - Saves files to **`Pictures/BooruVault/`** via Android `MediaStore` (Scoped Storage compliant, immediately indexed by Android's photo gallery).
   - Quality options: Original full resolution (with fallback to sample) or Sample medium resolution.
   - Integrity validation: validates HTTP responses, non-empty byte streams, and image file headers.
   - Cancel and Retry mechanisms.

4. **Offline Local Gallery & Viewer**:
   - Offline gallery displays all downloaded artworks and favorites without internet access.
   - Search downloaded posts by tags or artist.
   - Virtual collections system (e.g., Favorites, Wallpapers, Characters) stored in Room.
   - Multi-select actions: Add to Collection, Share, Delete.
   - Fullscreen **Image Viewer** with pinch-to-zoom (up to 5x), pan gestures, slideshow playback mode with auto-advance, and collapsible metadata inspector.

5. **Content Control & Privacy**:
   - Safe, Questionable, and Explicit content filters.
   - Sensitive thumbnail blur preview mode with quick-reveal toggle button.

6. **Customizable Settings**:
   - Dark Mode, Light Mode, and System Theme.
   - Dynamic grid columns (2 to 5 columns).
   - Temporary cache cleaner (safely clears cache without deleting downloaded files).
   - Search history manager.

---

## API Endpoints Used

| Source | Search Endpoint | Autocomplete Endpoint |
| :--- | :--- | :--- |
| **Danbooru** | `GET /posts.json?tags={tags}&page={page}&limit={limit}` | `GET /tags.json?search[name_matches]={query}` |
| **Gelbooru** | `GET /index.php?page=dapi&s=post&q=index&json=1&tags={tags}&pid={page}` | `GET /index.php?page=dapi&s=tag&q=index&json=1&name={query}` |
| **Yande.re** | `GET /post.json?tags={tags}&page={page}&limit={limit}` | `GET /tag.json?name={query}&order=count` |

---

## Android Permissions

- `android.permission.INTERNET`: Required to communicate with Danbooru, Gelbooru, and Yande.re HTTPS endpoints and download artwork files.
- `android.permission.ACCESS_NETWORK_STATE`: Monitors network availability for graceful offline mode transitions.
- *Storage Permissions*: On Android 10+ (API 29+), BooruVault utilizes the zero-permission **`MediaStore` API** to write directly into `Pictures/BooruVault/`, ensuring full Google Play policy compliance without requesting sensitive storage permissions.

---

## Build & Run Instructions

### Build Debug APK
```bash
gradle assembleDebug
```
The resulting APK is generated at:
`app/build/outputs/apk/debug/app-debug.apk`

### Run Local Unit Tests
```bash
gradle :app:testDebugUnitTest
```
Tests verify API JSON parsing, model normalizations, tag categories, and rating translations.
