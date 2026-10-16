# BooruVault — Native Android Image Downloader & Gallery

BooruVault is a full-featured, high-performance native Android application engineered with Kotlin and Jetpack Compose for searching, browsing, downloading, tagging, and organizing images from Danbooru, Gelbooru, and Yande.re with offline local gallery persistence.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural parameters have been confirmed based on your preferences:

- **Confirmed Storage Location**: Downloaded images are stored in the public device directory `Pictures/BooruVault/` using Android's modern `MediaStore` API (scoped storage compliant, accessible to system galleries and other apps).
- **Confirmed Content Filtering**: All content ratings (Safe, Questionable, Explicit) are enabled and queryable, with smart blur filters applied to sensitive thumbnail previews that can be tapped to reveal.
- **Confirmed Image Quality Strategy**: Downloader targets highest-fidelity original file URLs, automatically falling back to sample images if original files are restricted or unavailable.
- **Offline & Local Tag Importer**: Room database stores full tag taxonomies, post metadata, user collections, search history, and download tasks for full offline browsing.

---

## 1. Overview & Core Concept

- **What It Does**: BooruVault connects directly to Danbooru, Gelbooru, and Yande.re REST APIs, normalizes diverse data schemas into a unified domain model, provides multi-tag searching with autocomplete and rating filters, manages an asynchronous background download queue via Kotlin Coroutines and OkHttp streaming, and delivers a native offline gallery with full pinch-to-zoom viewer, tag importer, collections, and EXIF/metadata inspection.
- **Target Audience**: Anime art enthusiasts, digital artists, collectors, and researchers who require reliable image archiving, metadata preservation, and responsive tagging on Android devices.
- **Key Value**: A fast, privacy-focused, native Android client without webview wrappers, featuring robust error handling, rate limiting compliance, and non-destructive local tag management.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Explore & Search Flow**:
   - User opens the app to the **Explore** tab featuring a Material 3 Search Bar, source selector chips (*All Sources*, *Danbooru*, *Gelbooru*, *Yande.re*), and active rating filter chips.
   - User types tags (e.g. `scenery 1girl`); search history suggestions and autocompletions appear.
   - Infinite-scrolling staggered grid renders thumbnails with smooth crossfade loading, rating badges, resolution tags, and quick-action buttons (favorite, download).
   - Sensitive images feature a translucent blur layer with a quick-tap unblur toggle.

2. **Image Inspection & Tag Importer Flow**:
   - Tapping an item opens the **Post Detail Screen** with high-resolution preview, pinch-to-zoom, pan, and full metadata breakdown (dimensions, file size, rating, artist, score, source URL).
   - The **Tag Importer** card lists all post tags classified by category (General, Artist, Character, Copyright, Meta).
   - Action buttons allow users to **Import Tags to Local Room DB**, **Copy Single/All Tags**, **Search Similar**, or **Export Metadata to JSON**.

3. **Download & MediaStore Flow**:
   - Tapping Download prompts quality selection (Original vs Sample) or uses the default setting.
   - Download is dispatched to the background worker with live bytes progress, status transitions (*Waiting* -> *Downloading* -> *Completed* / *Failed*), and error retry options.
   - Files are written to `Pictures/BooruVault/` with registered MediaStore entries so the device gallery detects them immediately.

4. **Local Gallery & Collection Flow**:
   - The **Gallery** tab presents downloaded images categorized by collections, sources, or tags.
   - Multi-select mode allows bulk operations: Add to Collection, Share, Delete, or Re-scan Missing files.
   - Fullscreen **Gallery Viewer** supports pinch-to-zoom, swipe gestures between pictures, slideshow playback with customizable intervals, and an expandable bottom sheet for local tags and EXIF data.

5. **Settings & Customization Flow**:
   - Manage Dark/Light theme, grid column counts (2 to 5 columns), concurrent download limits (1-4 threads), blur sensitivity, search history clearing, and database backup/export.

### Visual Identity & Theme
- **Theme Mood**: Premium Midnight Dark (default) with crisp slate surfaces and an electric indigo accent (`#6366F1`) paired with cyan highlight (`#06B6D4`).
- **Surface Elevation**: Glass-like tonal surface elevation with subtle rounded borders (`RoundedCornerShape(16.dp)`) and Material 3 cards.
- **Responsive Layout**: Adaptive grid column counts that adjust seamlessly between portrait phones (2 columns) and tablets/landscape mode (3-5 columns).
- **Navigation**: Clean M3 `NavigationBar` with distinct icons for Explore, Downloads, Gallery, and Settings, respecting system navigation bar insets.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Native REST API Integration over HTML Scraping**:
  - *Chosen Approach*: Direct HTTPS JSON API calls to Danbooru (`/posts.json`), Gelbooru (`index.php?page=dapi&s=post&q=index&json=1`), and Yande.re (`/post.json`).
  - *Why*: Reliable, structured, high-speed responses that respect server infrastructure and provide clean metadata fields without fragile DOM parsing.
  - *Fallback Handling*: Custom OkHttp user-agent headers and status interceptors handle 401, 403, 429 rate limits, and missing file URLs gracefully.

- **Decision 2: Local Persistence via Room with Relational Schema**:
  - *Chosen Approach*: Android Room database storing `PostEntity`, `TagEntity`, `PostTagCrossRef`, `CollectionEntity`, `CollectionPostCrossRef`, and `DownloadTaskEntity`.
  - *Why*: Enables offline tag querying, fast indexing, many-to-many tag relationships, and full local search without relying on continuous internet connectivity.

- **Decision 3: Coroutine-Based MediaStore Streaming Downloader**:
  - *Chosen Approach*: Streamlined download engine using OkHttp streaming response bodies directly piped into Android `ContentResolver.openOutputStream` via `MediaStore.Images.Media.getContentUri(VOLUME_EXTERNAL_PRIMARY)`.
  - *Why*: Full Scoped Storage compliance on Android 10+ (API 29 to 36) without needing legacy `WRITE_EXTERNAL_STORAGE` permissions, and immediate visibility in all device photo apps.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                        Jetpack Compose UI Layer                        │
├───────────────┬────────────────┬────────────────┬──────────────────────┤
│  ExploreView  │   DetailView   │  DownloadView  │  Gallery & Viewer    │
└───────┬───────┴────────┬───────┴────────┬───────┴──────────┬───────────┘
        │                │                │                  │
┌───────▼────────────────▼────────────────▼──────────────────▼───────────┐
│                 Jetpack ViewModels (StateFlow / UI State)              │
│       ExploreViewModel, DetailViewModel, GalleryViewModel, etc.       │
└───────┬─────────────────────────────────┬──────────────────┬───────────┘
        │                                 │                  │
┌───────▼──────────────────────┐ ┌────────▼────────┐ ┌───────▼───────────┐
│   Booru Remote Repositories  │ │ Downloader Mgr  │ │ Local Repository  │
│  - DanbooruRepository        │ │ - MediaStore    │ │ - PostDao         │
│  - GelbooruRepository        │ │ - OkHttp Stream │ │ - TagDao          │
│  - YandereRepository         │ │ - Concurrency   │ │ - CollectionDao   │
└──────────────┬───────────────┘ └────────┬────────┘ └───────┬───────────┘
               │                          │                  │
┌──────────────▼───────────────┐ ┌────────▼────────┐ ┌───────▼───────────┐
│   Retrofit2 / OkHttp APIs    │ │ External Storage│ │   Room SQLite DB  │
│  (Danbooru, Gelbooru, Yande) │ │ Pictures/Booru..│ │ (Posts, Tags, Rel)│
└──────────────────────────────┘ └─────────────────┘ └───────────────────┘
```

### Data Entities & Schema
- `PostEntity`: `id`, `source`, `postUrl`, `previewUrl`, `sampleUrl`, `fileUrl`, `fileExtension`, `width`, `height`, `fileSize`, `rating`, `score`, `artist`, `sourceUrl`, `createdAt`, `isDownloaded`, `localUri`, `isFavorite`.
- `TagEntity`: `tagId`, `name`, `category`, `source`, `count`.
- `PostTagCrossRef`: `sourcePostKey`, `tagId`.
- `CollectionEntity`: `id`, `name`, `description`, `createdAt`, `coverUri`.
- `CollectionPostCrossRef`: `collectionId`, `sourcePostKey`.
- `DownloadTaskEntity`: `downloadId`, `postId`, `source`, `title`, `url`, `progress`, `bytesDownloaded`, `totalBytes`, `status`, `targetPath`, `errorMessage`.

---

## 5. Phased Implementation Plan

1. **Phase 1: Gradle & Architecture Foundation**:
   - Configure dependencies in `app/build.gradle.kts` (Coil, Navigation Compose, Room, Retrofit, Moshi, Datastore, Icons).
   - Set app name to **BooruVault** in `res/values/strings.xml` and `metadata.json`.
   - Setup Material 3 Theme, Dark Mode palette, type system, and vector icons.

2. **Phase 2: Remote API Layer & Normalization**:
   - Implement Retrofit clients and Moshi adapters for Danbooru, Gelbooru, and Yande.re.
   - Implement unified `BooruPost` and `BooruTag` domain models and mapping logic.
   - Add rating normalization (`Safe`, `Questionable`, `Explicit`, `Unknown`) and fallback URLs.

3. **Phase 3: Room Database & Local Repositories**:
   - Build Room Database, DAOs (`PostDao`, `TagDao`, `CollectionDao`, `DownloadDao`), and TypeConverters.
   - Implement repository methods for tag importing, local tag searching, and collection grouping.

4. **Phase 4: Downloader Engine & MediaStore Integration**:
   - Implement `DownloadManager` with queue concurrency, cancellation, progress Flow, and Scoped Storage MediaStore writer into `Pictures/BooruVault`.
   - Integrity checks to verify downloaded image headers and file sizes.

5. **Phase 5: User Interface & Navigation**:
   - **Explore Screen**: Dynamic search bar, rating toggles, column size control, animated thumbnail grid, blur mode for sensitive items, quick download and favorite actions.
   - **Post Detail Screen**: Pinch-to-zoom preview, complete metadata chips, tag importer with category grouping, copy tags, export JSON, and open in browser.
   - **Downloads Screen**: Active queue with progress bars, paused/completed/failed lists, retry and cancel buttons.
   - **Gallery Screen & Viewer**: Full offline browsing, multi-select toolbar, collections manager, slideshow viewer, zoom/pan controls, and EXIF/tag inspector.
   - **Settings Screen**: Theme switch, quality defaults, concurrency settings, storage info, cache cleaner, and database export.

6. **Phase 6: Verification & Polish**:
   - Unit tests for API models, rating mapping, and tag cross-referencing.
   - Compilation and UI verification with `compile_applet`.
