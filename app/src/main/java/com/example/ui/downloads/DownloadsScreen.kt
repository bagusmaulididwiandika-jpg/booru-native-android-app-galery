package com.example.ui.downloads

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import com.example.data.local.entity.DownloadTaskEntity
import com.example.ui.components.EmptyStateView

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DownloadsScreen(
    viewModel: DownloadsViewModel,
    onViewImage: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()

    val activeTasks = remember(state.tasks) {
        state.tasks.filter { it.status == "WAITING" || it.status == "DOWNLOADING" }
    }
    val completedTasks = remember(state.tasks) {
        state.tasks.filter { it.status == "COMPLETED" }
    }
    val failedTasks = remember(state.tasks) {
        state.tasks.filter { it.status == "FAILED" || it.status == "CANCELLED" }
    }

    val currentList = when (state.selectedTab) {
        0 -> activeTasks
        1 -> completedTasks
        else -> failedTasks
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text("Downloads", fontWeight = FontWeight.Bold)
                },
                actions = {
                    if (completedTasks.isNotEmpty()) {
                        TextButton(onClick = { viewModel.clearCompleted() }) {
                            Text("Clear Done", fontSize = 12.sp)
                        }
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
            // Tab row
            TabRow(
                selectedTabIndex = state.selectedTab,
                containerColor = MaterialTheme.colorScheme.surface
            ) {
                Tab(
                    selected = state.selectedTab == 0,
                    onClick = { viewModel.selectTab(0) },
                    text = {
                        Text("Active (${activeTasks.size})", fontWeight = FontWeight.SemiBold)
                    }
                )
                Tab(
                    selected = state.selectedTab == 1,
                    onClick = { viewModel.selectTab(1) },
                    text = {
                        Text("Completed (${completedTasks.size})", fontWeight = FontWeight.SemiBold)
                    }
                )
                Tab(
                    selected = state.selectedTab == 2,
                    onClick = { viewModel.selectTab(2) },
                    text = {
                        Text("Failed (${failedTasks.size})", fontWeight = FontWeight.SemiBold)
                    }
                )
            }

            if (currentList.isEmpty()) {
                val (icon, title, sub) = when (state.selectedTab) {
                    0 -> Triple(Icons.Outlined.FileDownload, "No Active Downloads", "Images you download will queue and show live progress here.")
                    1 -> Triple(Icons.Outlined.TaskAlt, "No Completed Downloads", "Downloaded images are stored in Pictures/BooruVault/.")
                    else -> Triple(Icons.Outlined.CheckCircle, "No Failed Downloads", "All downloads succeeded without errors!")
                }
                EmptyStateView(icon = icon, title = title, subtitle = sub)
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(items = currentList, key = { it.downloadId }) { task ->
                        DownloadItemCard(
                            task = task,
                            onCancel = { viewModel.cancelDownload(task.downloadId) },
                            onRetry = { viewModel.retryDownload(task) },
                            onDelete = { viewModel.deleteTask(task.downloadId) },
                            onView = { task.targetUri?.let { uri -> onViewImage(uri) } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DownloadItemCard(
    task: DownloadTaskEntity,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onDelete: () -> Unit,
    onView: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("download_item_${task.downloadId}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(task.previewUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(60.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color.Black.copy(alpha = 0.2f))
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = task.fileName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                when (task.status) {
                    "DOWNLOADING" -> {
                        LinearProgressIndicator(
                            progress = { (task.progressPercent / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${task.progressPercent}%",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            if (task.totalBytes > 0) {
                                val mbDownloaded = task.bytesDownloaded / (1024f * 1024f)
                                val mbTotal = task.totalBytes / (1024f * 1024f)
                                Text(
                                    text = String.format("%.1f / %.1f MB", mbDownloaded, mbTotal),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }

                    "WAITING" -> {
                        Text(
                            text = "Waiting in queue...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    "COMPLETED" -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Filled.CheckCircle,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Saved in Pictures/BooruVault",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    "FAILED", "CANCELLED" -> {
                        Text(
                            text = task.errorMessage ?: "Download cancelled or interrupted",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.error,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Action Buttons
            when (task.status) {
                "DOWNLOADING", "WAITING" -> {
                    IconButton(onClick = onCancel, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Filled.Close, contentDescription = "Cancel", tint = MaterialTheme.colorScheme.error)
                    }
                }

                "COMPLETED" -> {
                    IconButton(onClick = onView, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Outlined.Visibility, contentDescription = "View", tint = MaterialTheme.colorScheme.primary)
                    }
                }

                "FAILED", "CANCELLED" -> {
                    Row {
                        IconButton(onClick = onRetry, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Refresh, contentDescription = "Retry", tint = MaterialTheme.colorScheme.primary)
                        }
                        IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Filled.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}
