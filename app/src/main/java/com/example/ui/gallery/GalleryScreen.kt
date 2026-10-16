package com.example.ui.gallery

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.BooruPost
import com.example.data.model.BooruRating
import com.example.data.model.BooruSource
import com.example.ui.components.EmptyStateView
import com.example.ui.components.RatingBadge

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GalleryScreen(
    viewModel: GalleryViewModel,
    onOpenPost: (BooruPost) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    var showSortMenu by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    var newCollectionDesc by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = state.selectedCollectionName ?: "Offline Gallery",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${state.posts.size} artworks",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                actions = {
                    // Sort dropdown button
                    Box {
                        IconButton(onClick = { showSortMenu = true }) {
                            Icon(Icons.Filled.Sort, contentDescription = "Sort")
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false }
                        ) {
                            GallerySort.values().forEach { sort ->
                                DropdownMenuItem(
                                    text = { Text(sort.label) },
                                    leadingIcon = if (state.sortOption == sort) {
                                        { Icon(Icons.Filled.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                                    } else null,
                                    onClick = {
                                        viewModel.setSortOption(sort)
                                        showSortMenu = false
                                    }
                                )
                            }
                        }
                    }

                    // Multi-select toggle
                    IconButton(onClick = { viewModel.toggleMultiSelect() }) {
                        Icon(
                            imageVector = if (state.isMultiSelectMode) Icons.Filled.ChecklistRtl else Icons.Outlined.Checklist,
                            contentDescription = "Multi-select",
                            tint = if (state.isMultiSelectMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Local Search Input
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { viewModel.onSearchQueryChanged(it) },
                placeholder = { Text("Filter downloaded by tag or artist...", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                },
                trailingIcon = {
                    if (state.searchQuery.isNotBlank()) {
                        IconButton(onClick = { viewModel.onSearchQueryChanged("") }) {
                            Icon(Icons.Filled.Close, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            )

            // Collections Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 4.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // All Downloads chip
                FilterChip(
                    selected = state.selectedCollectionId == null,
                    onClick = { viewModel.selectCollection(null) },
                    label = { Text("All Vault") },
                    leadingIcon = if (state.selectedCollectionId == null) {
                        { Icon(Icons.Filled.Done, contentDescription = null, modifier = Modifier.size(14.dp)) }
                    } else null
                )

                // Favorite filter chip
                FilterChip(
                    selected = state.onlyFavorites,
                    onClick = { viewModel.toggleOnlyFavorites() },
                    label = { Text("★ Favorites") },
                    leadingIcon = if (state.onlyFavorites) {
                        { Icon(Icons.Filled.Favorite, contentDescription = null, tint = Color.Red, modifier = Modifier.size(14.dp)) }
                    } else null
                )

                // User collection chips
                state.collections.forEach { collection ->
                    FilterChip(
                        selected = state.selectedCollectionId == collection.collectionId,
                        onClick = { viewModel.selectCollection(collection) },
                        label = { Text(collection.name) },
                        trailingIcon = {
                            Icon(
                                Icons.Filled.Close,
                                contentDescription = "Delete collection",
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { viewModel.deleteCollection(collection.collectionId) }
                            )
                        }
                    )
                }

                // New Collection button
                InputChip(
                    selected = false,
                    onClick = { viewModel.setShowCreateCollectionDialog(true) },
                    label = { Text("+ New Collection") },
                    leadingIcon = { Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(14.dp)) }
                )
            }

            // Multi-select Action Bar
            AnimatedVisibility(visible = state.isMultiSelectMode) {
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${state.selectedPostKeys.size} selected",
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = { viewModel.selectAll() }) {
                                Text("Select All")
                            }
                            FilledTonalButton(
                                onClick = { viewModel.setShowAddToCollectionDialog(true) },
                                enabled = state.selectedPostKeys.isNotEmpty()
                            ) {
                                Text("Add to Collection")
                            }
                            IconButton(
                                onClick = { viewModel.deleteSelectedPosts() },
                                enabled = state.selectedPostKeys.isNotEmpty()
                            ) {
                                Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                            }
                        }
                    }
                }
            }

            // Gallery Grid
            if (state.posts.isEmpty()) {
                EmptyStateView(
                    icon = Icons.Outlined.PhotoAlbum,
                    title = "Vault is Empty",
                    subtitle = "Browse Explore tab to download artworks. Downloaded files are stored in Pictures/BooruVault/ for offline viewing."
                )
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(state.gridColumnCount),
                    contentPadding = PaddingValues(8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("gallery_grid")
                ) {
                    items(items = state.posts, key = { it.uniqueKey }) { post ->
                        val isSelected = state.selectedPostKeys.contains(post.uniqueKey)

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .aspectRatio(0.75f)
                                .then(
                                    if (isSelected) Modifier.border(
                                        3.dp,
                                        MaterialTheme.colorScheme.primary,
                                        RoundedCornerShape(12.dp)
                                    ) else Modifier
                                )
                                .clickable {
                                    if (state.isMultiSelectMode) {
                                        viewModel.togglePostSelection(post.uniqueKey)
                                    } else {
                                        onOpenPost(post)
                                    }
                                }
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = ImageRequest.Builder(LocalContext.current)
                                        .data(post.bestDisplayUrl)
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Rating Badge
                                RatingBadge(
                                    rating = post.rating,
                                    modifier = Modifier
                                        .align(Alignment.TopStart)
                                        .padding(6.dp)
                                )

                                if (post.isFavorite) {
                                    Icon(
                                        Icons.Filled.Favorite,
                                        contentDescription = "Favorite",
                                        tint = Color.Red,
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(20.dp)
                                    )
                                }

                                if (state.isMultiSelectMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { viewModel.togglePostSelection(post.uniqueKey) },
                                        modifier = Modifier
                                            .align(Alignment.BottomEnd)
                                            .padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Dialog: Create Collection
        if (state.showCreateCollectionDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.setShowCreateCollectionDialog(false) },
                title = { Text("New Collection") },
                text = {
                    Column {
                        OutlinedTextField(
                            value = newCollectionName,
                            onValueChange = { newCollectionName = it },
                            label = { Text("Collection Name") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newCollectionDesc,
                            onValueChange = { newCollectionDesc = it },
                            label = { Text("Description (Optional)") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.createCollection(newCollectionName, newCollectionDesc)
                            newCollectionName = ""
                            newCollectionDesc = ""
                        },
                        enabled = newCollectionName.isNotBlank()
                    ) {
                        Text("Create")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.setShowCreateCollectionDialog(false) }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Dialog: Add to Collection
        if (state.showAddToCollectionDialog) {
            AlertDialog(
                onDismissRequest = { viewModel.setShowAddToCollectionDialog(false) },
                title = { Text("Add ${state.selectedPostKeys.size} to Collection") },
                text = {
                    if (state.collections.isEmpty()) {
                        Text("No custom collections created yet. Please create a collection first.")
                    } else {
                        Column {
                            state.collections.forEach { col ->
                                Surface(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { viewModel.addSelectedToCollection(col.collectionId) }
                                        .padding(vertical = 8.dp),
                                    shape = RoundedCornerShape(8.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant
                                ) {
                                    Text(
                                        text = col.name,
                                        modifier = Modifier.padding(12.dp),
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { viewModel.setShowAddToCollectionDialog(false) }) {
                        Text("Close")
                    }
                }
            )
        }
    }
}
