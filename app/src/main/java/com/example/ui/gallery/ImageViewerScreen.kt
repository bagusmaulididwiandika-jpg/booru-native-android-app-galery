package com.example.ui.gallery

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.BooruPost
import com.example.ui.components.RatingBadge
import com.example.ui.components.TagChip
import kotlinx.coroutines.delay

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ImageViewerScreen(
    posts: List<BooruPost>,
    initialPostKey: String,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    var currentIndex by remember {
        val idx = posts.indexOfFirst { it.uniqueKey == initialPostKey }
        mutableIntStateOf(if (idx >= 0) idx else 0)
    }

    val currentPost = posts.getOrNull(currentIndex)

    BackHandler {
        onBack()
    }

    var scale by remember(currentIndex) { mutableFloatStateOf(1f) }
    var offset by remember(currentIndex) { mutableStateOf(Offset.Zero) }

    var isSlideshowActive by remember { mutableStateOf(false) }
    var showInfoSheet by remember { mutableStateOf(false) }

    // Slideshow coroutine
    LaunchedEffect(isSlideshowActive, currentIndex) {
        if (isSlideshowActive && posts.isNotEmpty()) {
            delay(4000)
            currentIndex = (currentIndex + 1) % posts.size
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        if (currentPost != null) {
            // Main Zoomable Image
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(currentPost.bestDisplayUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 5f)
                            offset = if (scale == 1f) Offset.Zero else offset + pan
                        }
                    }
                    .graphicsLayer(
                        scaleX = scale,
                        scaleY = scale,
                        translationX = offset.x,
                        translationY = offset.y
                    )
            )

            // Top Control Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }

                Text(
                    text = "${currentIndex + 1} / ${posts.size}",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )

                Row {
                    // Slideshow Toggle
                    IconButton(
                        onClick = { isSlideshowActive = !isSlideshowActive },
                        modifier = Modifier.background(
                            if (isSlideshowActive) MaterialTheme.colorScheme.primary else Color.Black.copy(alpha = 0.5f),
                            CircleShape
                        )
                    ) {
                        Icon(
                            imageVector = if (isSlideshowActive) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = "Slideshow",
                            tint = Color.White
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Info Sheet Toggle
                    IconButton(
                        onClick = { showInfoSheet = !showInfoSheet },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Outlined.Info, contentDescription = "Info", tint = Color.White)
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Share
                    IconButton(
                        onClick = {
                            val uriToShare = currentPost.localUri?.let { Uri.parse(it) }
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                if (uriToShare != null) {
                                    type = "image/*"
                                    putExtra(Intent.EXTRA_STREAM, uriToShare)
                                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                } else {
                                    type = "text/plain"
                                    putExtra(Intent.EXTRA_TEXT, currentPost.postUrl)
                                }
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Image"))
                        },
                        modifier = Modifier.background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Filled.Share, contentDescription = "Share", tint = Color.White)
                    }
                }
            }

            // Bottom Navigation Arrows (Prev / Next)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.Center)
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                if (currentIndex > 0) {
                    IconButton(
                        onClick = { currentIndex-- },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Filled.ChevronLeft, contentDescription = "Previous", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }

                if (currentIndex < posts.size - 1) {
                    IconButton(
                        onClick = { currentIndex++ },
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Filled.ChevronRight, contentDescription = "Next", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                } else {
                    Spacer(modifier = Modifier.size(48.dp))
                }
            }

            // Info Bottom Sheet Overlay
            AnimatedVisibility(
                visible = showInfoSheet,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(bottom = 8.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${currentPost.source.displayName} #${currentPost.id}",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            RatingBadge(rating = currentPost.rating)
                        }

                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Resolution: ${currentPost.width ?: "?"} × ${currentPost.height ?: "?"}  •  Score: ★${currentPost.score ?: 0}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        if (!currentPost.artist.isNullOrBlank()) {
                            Text(
                                text = "Artist: ${currentPost.artist}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }

                        if (currentPost.tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Tags (${currentPost.tags.size})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .heightIn(max = 140.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                currentPost.tags.take(25).forEach { tag ->
                                    TagChip(tag = tag)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
