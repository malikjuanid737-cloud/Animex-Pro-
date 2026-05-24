package com.example.ui.screens

import android.content.Context
import android.media.AudioManager
import android.util.Log
import androidx.compose.foundation.BorderStroke
import android.net.Uri
import android.view.ViewGroup
import android.view.Window
import android.widget.FrameLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.annotation.OptIn
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.data.Anime
import com.example.data.Episode
import com.example.data.MockData
import com.example.service.FirebaseServiceHelper
import com.example.ui.theme.AnimeAccent
import com.example.ui.theme.AnimeCardBg
import com.example.ui.theme.AnimeDarkBg
import com.example.ui.theme.AnimePrimary
import com.example.ui.theme.AnimeSecondary
import kotlinx.coroutines.delay

@OptIn(androidx.media3.common.util.UnstableApi::class)
@Composable
fun WatchScreen(
    animeId: String,
    episodeId: String,
    onNavigateBack: () -> Unit,
    onNavigateToNextEpisode: (String, String) -> Unit
) {
    val context = LocalContext.current
    val currentActivity = remember(context) { context as? ComponentActivity }

    val anime = remember(animeId) {
        MockData.animeList.firstOrNull { it.id == animeId } ?: MockData.animeList.first()
    }

    var currentEp by remember(episodeId) {
        mutableStateOf(
            anime.episodes.firstOrNull { it.id == episodeId } ?: anime.episodes.first()
        )
    }

    // AudioManager for Volume gestures
    val audioManager = remember(context) {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    // Brightness and Volume variables
    var showGestureHUD by remember { mutableStateOf(false) }
    var gestureHUDType by remember { mutableStateOf("Volume") } // "Volume" or "Brightness"
    var gestureHUDValue by remember { mutableStateOf(0.5f) } // range 0..1

    // Video states
    var isPlaying by remember { mutableStateOf(true) }
    var playbackPosition by remember { mutableStateOf(0L) }
    var videoDuration by remember { mutableStateOf(0L) }
    var subtitleEnabled by remember { mutableStateOf(true) }
    var activeQuality by remember { mutableStateOf("1080p") }
    
    // UI controls visibility
    var showControls by remember { mutableStateOf(true) }
    var isFullScreen by remember { mutableStateOf(false) }

    // Dialog state
    var showQualityDialog by remember { mutableStateOf(false) }

    // Initialize ExoPlayer
    val exoPlayer = remember {
        ExoPlayer.Builder(context).build().apply {
            repeatMode = Player.REPEAT_MODE_OFF
        }
    }

    // Set media source
    LaunchedEffect(currentEp) {
        exoPlayer.stop()
        val mediaItem = MediaItem.fromUri(Uri.parse(currentEp.videoUrl))
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.playWhenReady = isPlaying
        
        // Save progress trigger on start
        FirebaseServiceHelper.saveWatchProgress(anime, currentEp, 0L, 100L)
    }

    // Track position / playing states and duration
    LaunchedEffect(exoPlayer) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(isPlayingChange: Boolean) {
                isPlaying = isPlayingChange
            }
            override fun onPlaybackStateChanged(state: Int) {
                if (state == Player.STATE_READY) {
                    videoDuration = exoPlayer.duration
                }
            }
        }
        exoPlayer.addListener(listener)

        while (true) {
            if (exoPlayer.isPlaying) {
                playbackPosition = exoPlayer.currentPosition
                videoDuration = exoPlayer.duration
            }
            delay(500)
        }
    }

    // Periodical watch progress save (syncs with Firestore/fallback memory every 30 seconds)
    LaunchedEffect(currentEp, isPlaying) {
        while (isPlaying) {
            delay(30000) // 30 seconds tick
            val pSec = exoPlayer.currentPosition / 1000
            val tSec = exoPlayer.duration / 1000
            if (tSec > 0) {
                FirebaseServiceHelper.saveWatchProgress(anime, currentEp, pSec, tSec)
                LogWatchProgressNotification(context, currentEp.episodeNumber)
            }
        }
    }

    // Auto-hide controls loop
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(4000)
            showControls = false
        }
    }

    // Screen brightness & Volume adjuster helper
    fun adjustSettings(isLeft: Boolean, amount: Float) {
        showGestureHUD = true
        if (isLeft) {
            // Brightness control
            gestureHUDType = "Brightness"
            currentActivity?.let { activity ->
                val layoutParams = activity.window.attributes
                var currentBrightness = layoutParams.screenBrightness
                if (currentBrightness < 0) currentBrightness = 0.5f // Default
                val nextBrightness = (currentBrightness - amount).coerceIn(0.01f, 1.0f)
                layoutParams.screenBrightness = nextBrightness
                activity.window.attributes = layoutParams
                gestureHUDValue = nextBrightness
            }
        } else {
            // Volume control
            gestureHUDType = "Volume"
            val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
            val currentVol = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            // Adjust smoothly
            val step = if (amount > 0) -1 else 1
            val nextVol = (currentVol + step).coerceIn(0, maxVolume)
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, nextVol, 0)
            gestureHUDValue = nextVol.toFloat() / maxVolume.toFloat()
        }
        
        // Hide gesture HUD automatically shortly after touch end
    }

    // Clear gesture HUD delay
    LaunchedEffect(showGestureHUD) {
        if (showGestureHUD) {
            delay(2000)
            showGestureHUD = false
        }
    }

    // Full screen handling
    val view = LocalView.current
    val window = remember(currentActivity) { currentActivity?.window }
    
    fun toggleFullScreen() {
        if (window == null) return
        isFullScreen = !isFullScreen
        val windowInsetsController = WindowCompat.getInsetsController(window, view)
        if (isFullScreen) {
            // Toggle to Fullbleed Landscape
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            windowInsetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        } else {
            // Restore portrait systems
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        }
    }

    // Clean up resource releases
    DisposableEffect(key1 = true) {
        onDispose {
            exoPlayer.release()
            // Ensure status bar restored
            window?.let { w ->
                val controller = WindowCompat.getInsetsController(w, view)
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }

    if (showQualityDialog) {
        AlertDialog(
            onDismissRequest = { showQualityDialog = false },
            title = { Text("Stream Resolution", color = Color.White, fontWeight = FontWeight.Bold) },
            containerColor = AnimeCardBg,
            text = {
                Column {
                    listOf("Auto", "1080p Premium", "720p HD", "480p Fast").forEach { quality ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    activeQuality = quality.replace(" Premium", "").replace(" HD", "").replace(" Fast", "")
                                    Toast.makeText(context, "Buffered Quality: $quality", Toast.LENGTH_SHORT).show()
                                    showQualityDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(quality, color = Color.White)
                            if (quality.startsWith(activeQuality)) {
                                Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = AnimeAccent)
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showQualityDialog = false }) { Text("Dismiss", color = AnimeAccent) }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(AnimeDarkBg)
    ) {
        // Player Area (Responsive size)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isFullScreen) RowMaxField() else 225.dp)
                .background(Color.Black)
                .pointerInput(Unit) {
                    // Touch gestures for adjusting volume / brightness
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            val isLeft = change.position.x < (size.width / 2)
                            // Normalized drag amount
                            val sensitivity = dragAmount / size.height.toFloat()
                            adjustSettings(isLeft, sensitivity)
                        }
                    )
                }
                .clickable { showControls = !showControls }
        ) {
            // Android ExoPlayer Attachment
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exoPlayer
                        useController = false // Hide native controls, utilizing custom overlay
                        layoutParams = FrameLayout.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT
                        )
                    }
                },
                modifier = Modifier.fillMaxSize()
            )

            // Custom Player Custom Overlay controls
            androidx.compose.animation.AnimatedVisibility(
                visible = showControls,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.55f))
                ) {
                    // Top Player Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.TopCenter)
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            Text(
                                text = anime.title,
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Episode ${currentEp.episodeNumber}: ${currentEp.title}",
                                color = Color.White.copy(alpha = 0.6f),
                                fontSize = 11.sp,
                                maxLines = 1
                            )
                        }

                        // Subtitle toggle and Resolution Quality selectors
                        Row {
                            IconButton(onClick = {
                                subtitleEnabled = !subtitleEnabled
                                Toast.makeText(context, if (subtitleEnabled) "Subtitles: ON" else "Subtitles: OFF", Toast.LENGTH_SHORT).show()
                            }) {
                                Icon(
                                    imageVector = if (subtitleEnabled) Icons.Default.ClosedCaption else Icons.Default.ClosedCaptionDisabled,
                                    contentDescription = "Subtitles",
                                    tint = if (subtitleEnabled) AnimeAccent else Color.White
                                )
                            }
                            IconButton(onClick = { showQualityDialog = true }) {
                                Icon(imageVector = Icons.Default.Settings, contentDescription = "Quality", tint = Color.White)
                            }
                        }
                    }

                    // Center playback operations (Play/Pause, Rewind, FastForward)
                    Row(
                        modifier = Modifier.align(Alignment.Center),
                        horizontalArrangement = Arrangement.spacedBy(28.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { exoPlayer.seekTo((exoPlayer.currentPosition - 10000).coerceAtLeast(0L)) },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.Replay10, contentDescription = "Back 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                        }

                        IconButton(
                            onClick = {
                                if (exoPlayer.isPlaying) {
                                    exoPlayer.pause()
                                } else {
                                    exoPlayer.play()
                                }
                            },
                            modifier = Modifier
                                .size(56.dp)
                                .background(AnimeAccent, CircleShape)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(36.dp)
                            )
                        }

                        IconButton(
                            onClick = { exoPlayer.seekTo((exoPlayer.currentPosition + 10000).coerceAtMost(videoDuration)) },
                            modifier = Modifier.background(Color.Black.copy(alpha = 0.4f), CircleShape)
                        ) {
                            Icon(imageVector = Icons.Default.Forward10, contentDescription = "Skip 10s", tint = Color.White, modifier = Modifier.size(28.dp))
                        }
                    }

                    // Bottom progress sliders / Screen controls
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .align(Alignment.BottomCenter)
                            .padding(12.dp)
                    ) {
                        // Position text row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(formatTime(playbackPosition), color = Color.White, fontSize = 11.sp)
                            Text(formatTime(videoDuration), color = Color.White, fontSize = 11.sp)
                        }

                        // Progress slider bar
                        Slider(
                            value = if (videoDuration > 0) playbackPosition.toFloat() / videoDuration.toFloat() else 0f,
                            onValueChange = { percent ->
                                exoPlayer.seekTo((percent * videoDuration).toLong())
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = AnimeAccent,
                                activeTrackColor = AnimeAccent,
                                inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                            ),
                            modifier = Modifier.height(24.dp)
                        )

                        Spacer(modifier = Modifier.height(2.dp))

                        // Next episode & Full screen triggers
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Stream Server: Tokyo Cloud Alpha",
                                color = Color.White.copy(alpha = 0.5f),
                                fontSize = 11.sp
                            )
                            Row {
                                val nextEpIndex = anime.episodes.indexOf(currentEp) + 1
                                if (nextEpIndex < anime.episodes.size) {
                                    TextButton(onClick = {
                                        val nextEp = anime.episodes[nextEpIndex]
                                        onNavigateToNextEpisode(anime.id, nextEp.id)
                                    }) {
                                        Icon(imageVector = Icons.Default.SkipNext, contentDescription = null, tint = Color.White)
                                        Spacer(modifier = Modifier.width(4.dp))
                                        Text("Next EP", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                                    }
                                }
                                IconButton(onClick = { toggleFullScreen() }) {
                                    Icon(
                                        imageVector = if (isFullScreen) Icons.Default.FullscreenExit else Icons.Default.Fullscreen,
                                        contentDescription = "Fullscreen Toggle",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // HUD Adjuster Bar Grapic for Volume/Brightness
            androidx.compose.animation.AnimatedVisibility(
                visible = showGestureHUD,
                enter = fadeIn() + scaleIn(),
                exit = fadeOut() + scaleOut(),
                modifier = Modifier.align(Alignment.Center)
            ) {
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.72f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = if (gestureHUDType == "Volume") Icons.Default.VolumeUp else Icons.Default.WbSunny,
                            contentDescription = null,
                            tint = AnimeAccent,
                            modifier = Modifier.size(32.dp)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(gestureHUDType, color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
                        Spacer(modifier = Modifier.height(8.dp))
                        // Mini volume bar
                        Box(
                            modifier = Modifier
                                .width(80.dp)
                                .height(4.dp)
                                .background(Color.White.copy(alpha = 0.2f), RoundedCornerShape(2.dp))
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth(gestureHUDValue)
                                    .fillMaxHeight()
                                    .background(AnimeAccent, RoundedCornerShape(2.dp))
                            )
                        }
                    }
                }
            }
        }

        // Expanded Screen Details (Only show when not in full-screen)
        if (!isFullScreen) {
            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                item {
                    Column {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .background(AnimeAccent, RoundedCornerShape(4.dp))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text("EP ${currentEp.episodeNumber}", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                            }
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = currentEp.title,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = currentEp.synopsis,
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 12.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

            // Episode selection horizontal or vertical list, highlighting what is active
            Text(
                text = "Episodes (${anime.episodes.size})",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(16.dp)
            )

            LazyRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(anime.episodes) { ep ->
                    val isActive = ep.id == currentEp.id
                    EpisodeListTile(ep, isActive) {
                        currentEp = ep
                    }
                }
            }
        }
    }
}

@Composable
fun EpisodeListTile(
    episode: Episode,
    isActive: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isActive) AnimePrimary.copy(alpha = 0.15f) else AnimeCardBg
        ),
        shape = RoundedCornerShape(16.dp),
        border = if (isActive) BorderStroke(1.5.dp, AnimeSecondary) else BorderStroke(1.dp, Color.White.copy(alpha = 0.05f)),
        modifier = Modifier
            .width(160.dp)
            .height(130.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AsyncImage(
                    model = episode.thumbnailUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.5f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(imageVector = Icons.Default.PlayCircle, contentDescription = null, tint = AnimeSecondary, modifier = Modifier.size(32.dp))
                    }
                }
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.75f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(episode.duration, color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Black)
                }
            }
            Column(modifier = Modifier.padding(8.dp)) {
                Text(
                    text = "EP ${episode.episodeNumber}: ${episode.title}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSecs = ms / 1000
    val mins = totalSecs / 60
    val secs = totalSecs % 60
    return String.format("%02d:%02d", mins, secs)
}

private fun LogWatchProgressNotification(context: Context, epNo: Int) {
     Log.d("AnimExPlayer", "State check synchronized: Episode $epNo sync saved to Cloud Firestore.")
}

@Composable
fun RowMaxField(): androidx.compose.ui.unit.Dp {
    return 360.dp // High-density screen landscape bounds modifier
}
