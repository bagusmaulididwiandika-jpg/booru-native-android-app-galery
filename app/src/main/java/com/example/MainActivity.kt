package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.downloader.DownloadManager
import com.example.data.local.AppDatabase
import com.example.data.model.BooruPost
import com.example.data.preferences.AppPreferences
import com.example.data.repository.BooruRepository
import com.example.data.repository.LocalGalleryRepository
import com.example.ui.detail.PostDetailScreen
import com.example.ui.detail.PostDetailViewModel
import com.example.ui.downloads.DownloadsScreen
import com.example.ui.downloads.DownloadsViewModel
import com.example.ui.explore.ExploreScreen
import com.example.ui.explore.ExploreViewModel
import com.example.ui.gallery.GalleryScreen
import com.example.ui.gallery.GalleryViewModel
import com.example.ui.gallery.ImageViewerScreen
import com.example.ui.navigation.Screen
import com.example.ui.settings.SettingsScreen
import com.example.ui.settings.SettingsViewModel
import com.example.ui.theme.BooruVaultTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            val db = remember { AppDatabase.getInstance(context) }
            val appPreferences = remember { AppPreferences(context) }
            val booruRepository = remember { BooruRepository() }
            val localGalleryRepository = remember { LocalGalleryRepository(db) }
            val downloadManager = remember { DownloadManager(context, db) }

            val userPrefs by appPreferences.preferencesFlow.collectAsState(initial = com.example.data.preferences.UserPreferences())

            val isDark = when (userPrefs.darkModeOption) {
                0 -> false
                1 -> true
                else -> isSystemInDarkTheme()
            }

            BooruVaultTheme(darkTheme = isDark) {
                val snackbarHostState = remember { SnackbarHostState() }

                var currentTab by remember { mutableStateOf<Screen>(Screen.Explore) }
                var selectedPostForDetail by remember { mutableStateOf<BooruPost?>(null) }
                var selectedViewerPostKey by remember { mutableStateOf<String?>(null) }
                var viewerPostsList by remember { mutableStateOf<List<BooruPost>>(emptyList()) }

                // ViewModels
                val exploreViewModel = remember {
                    ExploreViewModel(booruRepository, localGalleryRepository, downloadManager, appPreferences)
                }
                val detailViewModel = remember {
                    PostDetailViewModel(localGalleryRepository, downloadManager)
                }
                val downloadsViewModel = remember {
                    DownloadsViewModel(downloadManager)
                }
                val galleryViewModel = remember {
                    GalleryViewModel(localGalleryRepository, appPreferences)
                }
                val settingsViewModel = remember {
                    SettingsViewModel(appPreferences, localGalleryRepository, db, context)
                }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when {
                        // Fullscreen Image Viewer
                        selectedViewerPostKey != null -> {
                            ImageViewerScreen(
                                posts = viewerPostsList,
                                initialPostKey = selectedViewerPostKey!!,
                                onBack = {
                                    selectedViewerPostKey = null
                                    viewerPostsList = emptyList()
                                }
                            )
                        }

                        // Post Detail Screen
                        selectedPostForDetail != null -> {
                            PostDetailScreen(
                                post = selectedPostForDetail!!,
                                viewModel = detailViewModel,
                                onBackClick = { selectedPostForDetail = null },
                                snackbarHostState = snackbarHostState
                            )
                        }

                        // Main Bottom Navigation Flow
                        else -> {
                            Scaffold(
                                snackbarHost = { SnackbarHost(snackbarHostState) },
                                bottomBar = {
                                    NavigationBar(
                                        windowInsets = WindowInsets.navigationBars,
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ) {
                                        Screen.bottomNavItems.forEach { screen ->
                                            val isSelected = currentTab == screen
                                            NavigationBarItem(
                                                selected = isSelected,
                                                onClick = { currentTab = screen },
                                                icon = {
                                                    Icon(
                                                        imageVector = if (isSelected) screen.selectedIcon else screen.unselectedIcon,
                                                        contentDescription = screen.title
                                                    )
                                                },
                                                label = { Text(screen.title) },
                                                modifier = Modifier.testTag("nav_tab_${screen.route}")
                                            )
                                        }
                                    }
                                }
                            ) { innerPadding ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(innerPadding)
                                ) {
                                    when (currentTab) {
                                        Screen.Explore -> {
                                            ExploreScreen(
                                                viewModel = exploreViewModel,
                                                onPostClick = { post -> selectedPostForDetail = post },
                                                snackbarHostState = snackbarHostState
                                            )
                                        }

                                        Screen.Downloads -> {
                                            DownloadsScreen(
                                                viewModel = downloadsViewModel,
                                                onViewImage = { uri ->
                                                    // Load post and open viewer
                                                    selectedViewerPostKey = uri
                                                    viewerPostsList = listOf(
                                                        BooruPost(
                                                            id = 0,
                                                            source = com.example.data.model.BooruSource.DANBOORU,
                                                            postUrl = "",
                                                            previewUrl = uri,
                                                            sampleUrl = uri,
                                                            fileUrl = uri,
                                                            fileExtension = "jpg",
                                                            width = null,
                                                            height = null,
                                                            fileSize = null,
                                                            rating = com.example.data.model.BooruRating.SAFE,
                                                            score = 0,
                                                            tags = emptyList(),
                                                            artist = null,
                                                            sourceUrl = null,
                                                            createdAt = null,
                                                            isDownloaded = true,
                                                            localUri = uri
                                                        )
                                                    )
                                                }
                                            )
                                        }

                                        Screen.Gallery -> {
                                            val galleryState by galleryViewModel.uiState.collectAsState()
                                            GalleryScreen(
                                                viewModel = galleryViewModel,
                                                onOpenPost = { post ->
                                                    viewerPostsList = galleryState.posts
                                                    selectedViewerPostKey = post.uniqueKey
                                                }
                                            )
                                        }

                                        Screen.Settings -> {
                                            SettingsScreen(
                                                viewModel = settingsViewModel,
                                                snackbarHostState = snackbarHostState
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
