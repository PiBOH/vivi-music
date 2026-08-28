package com.music.vivi.desktop

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.automirrored.filled.PlaylistAdd
import androidx.compose.material.icons.automirrored.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.SpeakerGroup
import androidx.compose.material.icons.automirrored.filled.Subject
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Fullscreen
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Lyrics
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.AllInclusive
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.TextButton
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import coil3.compose.AsyncImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.music.innertube.YouTube
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.music.lrclib.LrcLib
import com.music.vivi.canvas.CanvasArtwork
import com.music.vivi.desktop.player.LoadPhase
import com.music.vivi.desktop.player.RepeatMode
import kotlinx.coroutines.delay

private enum class M3ETab { NONE, QUEUE, LYRICS, HISTORY }

@Composable
fun PlayerScreen(
    queue: List<NowPlaying>,
    index: Int,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    volume: Float,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    errorKey: String?,
    errorDetail: String?,
    loadPhase: LoadPhase,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolume: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    language: String,
    onOpenLyrics: () -> Unit,
    onOpenLyricsFocus: (() -> Unit)? = null,
    onOpenQueue: () -> Unit,
    onAddToPlaylist: (NowPlaying) -> Unit,
    onSkipTo: (Int) -> Unit = {},
    onRemoveAt: (Int) -> Unit = {},
    onClearQueue: () -> Unit = {},
    onReorderQueue: (List<NowPlaying>) -> Unit = {},
    sliderStyle: ViviSliderStyle = ViviSliderStyle.SLIM,
    design: PlayerDesign = PlayerDesign.CLASSIC,
    background: PlayerBackgroundStyle = PlayerBackgroundStyle.CANVAS,
    rotatingThumbnail: Boolean = false,
    accent: Color = MaterialTheme.colorScheme.primary,
    audioLevel: Float = 0f,
    onBack: (() -> Unit)? = null,
) {
    val np = queue.getOrNull(index)
    var canvasArt by remember { mutableStateOf<CanvasArtwork?>(null) }

    LaunchedEffect(np?.videoId) {
        canvasArt = null
        val track = np ?: return@LaunchedEffect
        val settings = DesktopSettings.load()
        canvasArt = if (settings.canvasEnabled) {
            withContext(Dispatchers.IO) {
                CanvasResolver.resolve(track.title, track.artist, null, CanvasSource.from(settings.canvasSource))
            }
        } else {
            null
        }
    }

    val bgUrl = CanvasResolver.displayUrl(canvasArt, np?.thumbnail)

    Box(Modifier.fillMaxSize()) {
        PlayerBackground(
            style = background,
            bgUrl = bgUrl,
            accent = accent,
            modifier = Modifier.fillMaxSize(),
            audioLevel = audioLevel,
            isPlaying = isPlaying,
        )
        if (np == null) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(Localization.get(language, "nothing_playing"), style = MaterialTheme.typography.titleLarge)
            }
        } else if (design == PlayerDesign.EXPRESSIVE) {
            M3EPlayerContent(
                np = np,
                queue = queue,
                index = index,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                volume = volume,
                isShuffle = isShuffle,
                repeatMode = repeatMode,
                errorKey = errorKey,
                errorDetail = errorDetail,
                loadPhase = loadPhase,
                onTogglePlay = onTogglePlay,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeek = onSeek,
                onVolume = onVolume,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                language = language,
                onOpenLyrics = onOpenLyrics,
                onOpenQueue = onOpenQueue,
                onAddToPlaylist = { onAddToPlaylist(np) },
                onSkipTo = onSkipTo,
                onRemoveAt = onRemoveAt,
                onClearQueue = onClearQueue,
                onReorderQueue = onReorderQueue,
                sliderStyle = sliderStyle,
                rotatingThumbnail = rotatingThumbnail,
                accent = accent,
                onBack = onBack,
            )
        } else {
            PlayerContent(
                np = np,
                queueSize = queue.size,
                onBack = onBack,
                isPlaying = isPlaying,
                positionMs = positionMs,
                durationMs = durationMs,
                volume = volume,
                isShuffle = isShuffle,
                repeatMode = repeatMode,
                errorKey = errorKey,
                errorDetail = errorDetail,
                loadPhase = loadPhase,
                onTogglePlay = onTogglePlay,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeek = onSeek,
                onVolume = onVolume,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                language = language,
                onOpenLyrics = onOpenLyrics,
                onOpenLyricsFocus = onOpenLyricsFocus,
                onOpenQueue = onOpenQueue,
                onAddToPlaylist = { onAddToPlaylist(np) },
                sliderStyle = sliderStyle,
                design = design,
                background = background,
                rotatingThumbnail = rotatingThumbnail,
                accent = accent,
            )
        }
    }
}

@Composable
private fun M3EPlayerContent(
    np: NowPlaying,
    queue: List<NowPlaying>,
    index: Int,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    volume: Float,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    errorKey: String?,
    errorDetail: String?,
    loadPhase: LoadPhase,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolume: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    language: String,
    onOpenLyrics: () -> Unit,
    onOpenQueue: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    onSkipTo: (Int) -> Unit = {},
    onRemoveAt: (Int) -> Unit = {},
    onClearQueue: () -> Unit = {},
    onReorderQueue: (List<NowPlaying>) -> Unit = {},
    sliderStyle: ViviSliderStyle = ViviSliderStyle.SLIM,
    rotatingThumbnail: Boolean = false,
    accent: Color = MaterialTheme.colorScheme.primary,
    onBack: (() -> Unit)? = null,
) {
    var activeTab by remember { mutableStateOf(M3ETab.QUEUE) }
    var isFavorite by remember { mutableStateOf(false) }
    Box(Modifier.fillMaxSize().padding(horizontal = 36.dp, vertical = 24.dp)) {
        Row(
            Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left Player Panel: Full-Size Unchanged Apple Music Player Layout
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                contentAlignment = Alignment.Center,
            ) {
                // 1. Artwork Thumbnail
                val artworkSize = 467.dp
                val artworkOffsetX = (-5).dp
                val artworkOffsetY = 0.dp

                // 2. Song Title
                val titleFontSize = 22.sp
                val titleOffsetX = (-178).dp
                val titleOffsetY = 0.dp

                // 3. Artist Text
                val artistFontSize = 15.sp
                val artistOffsetX = (-76).dp
                val artistOffsetY = 0.dp

                // 4. Play Bar (Seekbar & Timestamps)
                val playBarWidth = 465.dp
                val playBarOffsetX = (-9).dp
                val playBarOffsetY = (-10).dp

                // 5. Playback Controls (Previous, Play/Pause, Next)
                val playPauseButtonSize = 55.dp
                val skipButtonSize = 45.dp
                val controlsOffsetX = 0.dp
                val controlsOffsetY = (-20).dp

                // 6. Volume Control Bar
                val volumeBarWidth = 453.dp
                val volumeOffsetX = -8.dp
                val volumeOffsetY = (-20).dp

                Column(
                    Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    // 1. Artwork Thumbnail
                    Box(
                        Modifier
                            .offset(x = artworkOffsetX, y = artworkOffsetY)
                            .shadow(20.dp, RoundedCornerShape(12.dp))
                    ) {
                        PlayerThumbnail(np.thumbnail, artworkSize, 12.dp, rotatingThumbnail)
                    }

                    Spacer(Modifier.height(14.dp))

                    // 2. Song Info (Title & Artist)
                    Column(
                        Modifier.fillMaxWidth().padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            np.title,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = titleFontSize,
                            ),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.offset(x = titleOffsetX, y = titleOffsetY)
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            np.artist,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontSize = artistFontSize,
                            ),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.offset(x = artistOffsetX, y = artistOffsetY)
                        )
                    }

                    Spacer(Modifier.height(14.dp))

                    // 4. Play Bar (Seekbar & Timestamps)
                    var isSeeking by remember(np.videoId) { mutableStateOf(false) }
                    var seekValue by remember(np.videoId) { mutableStateOf(0f) }
                    val sliderMax = durationMs.coerceAtLeast(1L)
                    val displayPosition = if (isSeeking) seekValue else positionMs.toFloat().coerceIn(0f, sliderMax.toFloat())
                    
                    Column(
                        Modifier
                            .width(playBarWidth)
                            .offset(x = playBarOffsetX, y = playBarOffsetY)
                    ) {
                        ViviSlider(
                            value = displayPosition.coerceIn(0f, sliderMax.toFloat()),
                            onValueChange = {
                                seekValue = it.coerceIn(0f, sliderMax.toFloat())
                                isSeeking = true
                            },
                            onValueChangeFinished = {
                                if (durationMs > 0) onSeek(seekValue.toLong())
                                isSeeking = false
                            },
                            enabled = durationMs > 0,
                            valueRange = 0f..sliderMax.toFloat(),
                            style = ViviSliderStyle.EXPRESSIVE,
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Row(Modifier.fillMaxWidth()) {
                            Text(
                                formatTime(displayPosition.toLong()),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Spacer(Modifier.weight(1f))
                            Text(
                                "-" + formatTime(maxOf(0L, durationMs - displayPosition.toLong())),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // 5. Main Playback Controls (Previous, Play/Pause, Next)
                    Row(
                        modifier = Modifier.offset(x = controlsOffsetX, y = controlsOffsetY),
                        horizontalArrangement = Arrangement.spacedBy(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Surface(
                            onClick = onPrevious,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(skipButtonSize),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.SkipPrevious,
                                    contentDescription = Localization.get(language, "previous"),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }

                        Surface(
                            onClick = onTogglePlay,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(playPauseButtonSize),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (loadPhase != LoadPhase.NONE) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(22.dp),
                                        strokeWidth = 2.dp,
                                        color = MaterialTheme.colorScheme.onPrimary
                                    )
                                } else {
                                    Icon(
                                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(28.dp),
                                    )
                                }
                            }
                        }

                        Surface(
                            onClick = onNext,
                            shape = CircleShape,
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.7f),
                            modifier = Modifier.size(skipButtonSize),
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    Icons.Filled.SkipNext,
                                    contentDescription = Localization.get(language, "next"),
                                    tint = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.size(26.dp),
                                )
                            }
                        }
                    }

                    Spacer(Modifier.height(14.dp))

                    // 6. Sound Bar (Volume Slider flanked by VolumeDown & VolumeUp icons)
                    Row(
                        Modifier
                            .width(volumeBarWidth)
                            .offset(x = volumeOffsetX, y = volumeOffsetY),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeDown,
                            contentDescription = "Volume Low",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        ViviSlider(
                            value = volume,
                            onValueChange = { /* Volume update */ },
                            valueRange = 0f..1f,
                            style = ViviSliderStyle.EXPRESSIVE,
                            modifier = Modifier.weight(1f)
                        )
                        Icon(
                            Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Volume Up",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Right Side Panel: Displayed when activeTab != M3ETab.NONE
            if (activeTab != M3ETab.NONE) {

                // Right Column: Apple Up Next Queue, Lyrics, or History panel taking full remaining width/height
                Box(
                    Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color(0xFF141416).copy(alpha = 0.45f))
                        .border(BorderStroke(1.dp, Color.White.copy(alpha = 0.06f)), RoundedCornerShape(20.dp))
                        .padding(16.dp)
                ) {
                    if (activeTab == M3ETab.LYRICS) {
                        LyricsScreen(
                            nowPlaying = np,
                            positionMs = positionMs,
                            isPlaying = isPlaying,
                            language = language,
                            onTogglePlay = onTogglePlay,
                            onBack = { activeTab = M3ETab.NONE },
                        )
                    } else if (activeTab == M3ETab.QUEUE) {
                        AppleUpNextQueueScreen(
                            queue = queue,
                            index = index,
                            language = language,
                            onSkipTo = onSkipTo,
                            onRemoveAt = onRemoveAt,
                            onClear = onClearQueue,
                            onReorder = onReorderQueue,
                            onAddToPlaylist = { onAddToPlaylist?.invoke() },
                            accent = accent,
                        )
                    } else if (activeTab == M3ETab.HISTORY) {
                        QueueHistoryScreen(
                            queue = queue,
                            index = index,
                            language = language,
                            onSkipTo = onSkipTo,
                            accent = accent,
                        )
                    }

                    // Floating Bottom Segmented Pill Toolbar (Queue, Lyrics, History)
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color(0xFF1C1C1F).copy(alpha = 0.95f),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                        shadowElevation = 8.dp,
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 12.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            // Tab 1: Queue
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (activeTab == M3ETab.QUEUE) Color.White.copy(alpha = 0.22f) else Color.Transparent)
                                    .clickable { activeTab = if (activeTab == M3ETab.QUEUE) M3ETab.NONE else M3ETab.QUEUE },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.AutoMirrored.Filled.QueueMusic,
                                    contentDescription = "Queue",
                                    tint = if (activeTab == M3ETab.QUEUE) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }

                            // Tab 2: Lyrics
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (activeTab == M3ETab.LYRICS) Color.White.copy(alpha = 0.22f) else Color.Transparent)
                                    .clickable { activeTab = if (activeTab == M3ETab.LYRICS) M3ETab.NONE else M3ETab.LYRICS },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.ChatBubbleOutline,
                                    contentDescription = "Lyrics",
                                    tint = if (activeTab == M3ETab.LYRICS) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }

                            // Tab 3: History
                            Box(
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (activeTab == M3ETab.HISTORY) Color.White.copy(alpha = 0.22f) else Color.Transparent)
                                    .clickable { activeTab = if (activeTab == M3ETab.HISTORY) M3ETab.NONE else M3ETab.HISTORY },
                                contentAlignment = Alignment.Center,
                            ) {
                                Icon(
                                    Icons.Filled.AccessTime,
                                    contentDescription = "History",
                                    tint = if (activeTab == M3ETab.HISTORY) Color.White else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                }
            }
        }

        onBack?.let { back ->
            Surface(
                onClick = back,
                shape = CircleShape,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .size(40.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.KeyboardArrowDown,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(26.dp),
                    )
                }
        }
    }
}
}

@Composable
private fun PlayerContent(
    np: NowPlaying,
    queueSize: Int,
    onBack: (() -> Unit)? = null,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    volume: Float,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    errorKey: String?,
    errorDetail: String?,
    loadPhase: LoadPhase,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolume: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    language: String,
    onOpenLyrics: () -> Unit,
    onOpenLyricsFocus: (() -> Unit)? = null,
    onOpenQueue: () -> Unit,
    onAddToPlaylist: (() -> Unit)? = null,
    sliderStyle: ViviSliderStyle = ViviSliderStyle.SLIM,
    design: PlayerDesign = PlayerDesign.CLASSIC,
    background: PlayerBackgroundStyle = PlayerBackgroundStyle.CANVAS,
    rotatingThumbnail: Boolean = false,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val contentWidth = 980.dp
    val metrics = design.metrics()
    val singleColumn = design == PlayerDesign.NEW || design == PlayerDesign.EXPRESSIVE
    val pillPlay = design == PlayerDesign.NEW

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 32.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Header: back button (the sidebar and the top header are hidden on
        // the full player, so this is the only visual way back) + label.
        Row(
            Modifier.widthIn(max = contentWidth).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (onBack != null) {
                Surface(
                    onClick = onBack,
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(40.dp),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Filled.KeyboardArrowDown,
                            contentDescription = Localization.get(language, "back"),
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(26.dp),
                        )
                    }
                }
                Spacer(Modifier.width(12.dp))
            }
            Text(
                Localization.get(language, "now_playing"),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        Spacer(Modifier.height(28.dp))

        if (singleColumn) {
            // Single-column "hero" layout: artwork + title centered on top,
            // controls stacked below (new / expressive designs).
            PlayerArtworkBlock(
                np = np,
                queueSize = queueSize,
                metrics = metrics,
                rotatingThumbnail = rotatingThumbnail,
                language = language,
                onAddToPlaylist = onAddToPlaylist,
                onOpenQueue = onOpenQueue,
            )
            Spacer(Modifier.height(24.dp))
            Column(
                Modifier.widthIn(max = contentWidth).fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                PlayerControlPanel(
                    np = np,
                    isPlaying = isPlaying,
                    positionMs = positionMs,
                    durationMs = durationMs,
                    volume = volume,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    loadPhase = loadPhase,
                    onTogglePlay = onTogglePlay,
                    onNext = onNext,
                    onPrevious = onPrevious,
                    onSeek = onSeek,
                    onVolume = onVolume,
                    onToggleShuffle = onToggleShuffle,
                    onCycleRepeat = onCycleRepeat,
                    language = language,
                    onOpenLyrics = onOpenLyrics,
                    onOpenLyricsFocus = onOpenLyricsFocus,
                    sliderStyle = sliderStyle,
                    pillPlay = pillPlay,
                )
            }
        } else {
            // Two-column layout: artwork on the left, controls on the right
            // (classic / v2 designs).
            Row(
                Modifier.widthIn(max = contentWidth).fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(40.dp),
            ) {
                Column(
                    Modifier.widthIn(max = metrics.artSize + 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    PlayerArtworkBlock(
                        np = np,
                        queueSize = queueSize,
                        metrics = metrics,
                        rotatingThumbnail = rotatingThumbnail,
                        language = language,
                        onAddToPlaylist = onAddToPlaylist,
                        onOpenQueue = onOpenQueue,
                    )
                }
                Column(Modifier.weight(1f)) {
                    PlayerControlPanel(
                        np = np,
                        isPlaying = isPlaying,
                        positionMs = positionMs,
                        durationMs = durationMs,
                        volume = volume,
                        isShuffle = isShuffle,
                        repeatMode = repeatMode,
                        loadPhase = loadPhase,
                        onTogglePlay = onTogglePlay,
                        onNext = onNext,
                        onPrevious = onPrevious,
                        onSeek = onSeek,
                        onVolume = onVolume,
                        onToggleShuffle = onToggleShuffle,
                        onCycleRepeat = onCycleRepeat,
                        language = language,
                        onOpenLyrics = onOpenLyrics,
                        onOpenLyricsFocus = onOpenLyricsFocus,
                        sliderStyle = sliderStyle,
                        pillPlay = pillPlay,
                    )
                }
            }
        }

        if (errorKey != null || errorDetail != null) {
            Spacer(Modifier.height(16.dp))
            if (errorKey != null) {
                Text(
                    Localization.get(language, errorKey),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error,
                    textAlign = TextAlign.Center,
                )
            }
            if (errorDetail != null) {
                // Selectable so the user can copy the full error/URL to report it.
                SelectionContainer {
                    Text(
                        errorDetail,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun PlayerArtworkBlock(
    np: NowPlaying,
    queueSize: Int,
    metrics: PlayerDesignMetrics,
    rotatingThumbnail: Boolean,
    language: String,
    onAddToPlaylist: (() -> Unit)?,
    onOpenQueue: () -> Unit,
) {
    Box(Modifier.shadow(16.dp, RoundedCornerShape(metrics.artCorner))) {
        Box {
            PlayerThumbnail(np.thumbnail, metrics.artSize, metrics.artCorner, rotatingThumbnail)
            if (metrics.overlayTitle) {
                Box(
                    Modifier
                        .matchParentSize()
                        .clip(RoundedCornerShape(metrics.artCorner))
                        .background(
                            Brush.verticalGradient(
                                0.5f to Color.Transparent,
                                1f to Color.Black.copy(alpha = 0.72f),
                            )
                        )
                )
                Column(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        np.title,
                        style = MaterialTheme.typography.titleLarge,
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        np.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.85f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
    if (!metrics.overlayTitle) {
        Spacer(Modifier.height(16.dp))
        Text(
            np.title,
            style = MaterialTheme.typography.titleLarge,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.height(6.dp))
        Text(
            np.artist,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }

    // Under the song text: add-to-playlist + queue, side by side.
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        if (onAddToPlaylist != null) {
            OutlinedButton(onClick = onAddToPlaylist) {
                Icon(Icons.AutoMirrored.Filled.PlaylistAdd, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(Localization.get(language, "add_to_playlist"))
            }
        }
        OutlinedButton(onClick = onOpenQueue) {
            Icon(Icons.AutoMirrored.Filled.QueueMusic, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("${Localization.get(language, "queue")} ($queueSize)")
        }
    }
}

@Composable
private fun PlayerControlPanel(
    np: NowPlaying,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    volume: Float,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    loadPhase: LoadPhase,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolume: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    language: String,
    onOpenLyrics: () -> Unit,
    onOpenLyricsFocus: (() -> Unit)? = null,
    sliderStyle: ViviSliderStyle,
    pillPlay: Boolean,
) {
    // Seek slider (position / duration). Disabled until the duration
    // is known so the slider can never degenerate into a 0..1 range
    // (which made the thumb snap to the start or the end). While the
    // user drags, the live position is ignored so it can't fight the
    // drag and yank the thumb back.
    var isSeeking by remember(np.videoId) { mutableStateOf(false) }
    var seekValue by remember(np.videoId) { mutableStateOf(0f) }
    val sliderMax = durationMs.coerceAtLeast(1L)
    val displayPosition = if (isSeeking) {
        seekValue
    } else {
        positionMs.toFloat().coerceIn(0f, sliderMax.toFloat())
    }
    ViviSlider(
        value = displayPosition.coerceIn(0f, sliderMax.toFloat()),
        onValueChange = {
            seekValue = it.coerceIn(0f, sliderMax.toFloat())
            isSeeking = true
        },
        onValueChangeFinished = {
            if (durationMs > 0) onSeek(seekValue.toLong())
            isSeeking = false
        },
        enabled = durationMs > 0,
        valueRange = 0f..sliderMax.toFloat(),
        style = sliderStyle,
        modifier = Modifier.fillMaxWidth(),
    )
    Row(Modifier.fillMaxWidth()) {
        Text(
            formatTime(displayPosition.toLong()),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.weight(1f))
        Text(
            formatTime(durationMs),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }

    if (loadPhase != LoadPhase.NONE) {
        Spacer(Modifier.height(12.dp))
        Row(
            Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            Spacer(Modifier.width(8.dp))
            Text(
                Localization.get(language, if (loadPhase == LoadPhase.RESOLVING) "resolving" else "downloading"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    // Transport controls: shuffle / previous / play / next / repeat.
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(onClick = onToggleShuffle) {
            Icon(
                Icons.Filled.Shuffle,
                contentDescription = Localization.get(language, "shuffle"),
                tint = if (isShuffle) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = onPrevious, modifier = Modifier.size(52.dp)) {
            Icon(
                Icons.Filled.SkipPrevious,
                contentDescription = Localization.get(language, "previous"),
                modifier = Modifier.size(36.dp),
            )
        }
        if (pillPlay) {
            Button(
                onClick = onTogglePlay,
                shape = RoundedCornerShape(50),
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                )
                Spacer(Modifier.width(8.dp))
                Text(Localization.get(language, if (isPlaying) "pause" else "play"))
            }
        } else {
            FilledIconButton(onClick = onTogglePlay, modifier = Modifier.size(72.dp)) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = Localization.get(language, if (isPlaying) "pause" else "play"),
                    modifier = Modifier.size(40.dp),
                )
            }
        }
        IconButton(onClick = onNext, modifier = Modifier.size(52.dp)) {
            Icon(
                Icons.Filled.SkipNext,
                contentDescription = Localization.get(language, "next"),
                modifier = Modifier.size(36.dp),
            )
        }
        IconButton(onClick = onCycleRepeat) {
            Icon(
                repeatIcon(repeatMode),
                contentDescription = Localization.get(language, "repeat"),
                tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    Spacer(Modifier.height(16.dp))

    // Volume.
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            volumeIcon(volume),
            contentDescription = Localization.get(language, "volume"),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.width(8.dp))
        ViviSlider(
            value = volume.coerceIn(0f, 1f),
            onValueChange = onVolume,
            valueRange = 0f..1f,
            style = sliderStyle,
            modifier = Modifier.weight(1f),
        )
    }

    Spacer(Modifier.height(16.dp))

    // Secondary actions.
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        OutlinedButton(onClick = onOpenLyrics) {
            Icon(Icons.AutoMirrored.Filled.Subject, contentDescription = null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(Localization.get(language, "lyrics"))
        }
        if (onOpenLyricsFocus != null) {
            OutlinedButton(onClick = onOpenLyricsFocus) {
                Icon(Icons.Filled.Lyrics, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(Localization.get(language, "lyrics_focus"))
            }
        }
    }
}

private fun volumeIcon(volume: Float) = when {
    volume <= 0f -> Icons.AutoMirrored.Filled.VolumeOff
    volume < 0.5f -> Icons.AutoMirrored.Filled.VolumeDown
    else -> Icons.AutoMirrored.Filled.VolumeUp
}

private fun repeatIcon(mode: RepeatMode) = when (mode) {
    RepeatMode.OFF, RepeatMode.ALL -> Icons.Filled.Repeat
    RepeatMode.ONE -> Icons.Filled.RepeatOne
}

@Composable
fun AppleUpNextQueueScreen(
    queue: List<NowPlaying>,
    index: Int,
    language: String,
    onSkipTo: (Int) -> Unit,
    onRemoveAt: (Int) -> Unit,
    onClear: () -> Unit,
    onReorder: (List<NowPlaying>) -> Unit,
    onAddToPlaylist: (NowPlaying) -> Unit,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val lazyListState = rememberLazyListState()
    val localQueue = remember { mutableStateListOf<NowPlaying>() }
    var hasDragged by remember { mutableStateOf(false) }

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localQueue.add(to.index, localQueue.removeAt(from.index))
        hasDragged = true
    }

    LaunchedEffect(queue) {
        if (!reorderableState.isAnyItemDragging) {
            localQueue.clear()
            localQueue.addAll(queue)
        }
    }

    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging && hasDragged) {
            onReorder(localQueue.toList())
            hasDragged = false
        }
    }

    val currentVideoId = queue.getOrNull(index)?.videoId
    var isAutoplayEnabled by remember { mutableStateOf(true) }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        // Header: "Up Next" title on left, Clear / Playlist / Autoplay buttons on right
        Row(
            Modifier.fillMaxWidth().padding(bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = Localization.get(language, "up_next"),
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                ),
                color = MaterialTheme.colorScheme.onSurface,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                if (queue.isNotEmpty()) {
                    TextButton(
                        onClick = onClear,
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                    ) {
                        Text(
                            text = Localization.get(language, "clear_queue"),
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                IconButton(
                    onClick = { },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.PlaylistAdd,
                        contentDescription = "Queue Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp),
                    )
                }

                IconButton(
                    onClick = { isAutoplayEnabled = !isAutoplayEnabled },
                    modifier = Modifier.size(32.dp),
                ) {
                    Icon(
                        Icons.Filled.AllInclusive,
                        contentDescription = "Autoplay",
                        tint = if (isAutoplayEnabled) Color.White else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp),
                    )
                }
            }
        }

        if (queue.isEmpty()) {
            Box(
                Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    Localization.get(language, "queue_empty"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(
                state = lazyListState,
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                itemsIndexed(localQueue, key = { _, item -> item.videoId }) { i, item ->
                    val isCurrent = item.videoId == currentVideoId
                    ReorderableItem(state = reorderableState, key = item.videoId) {
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .clickable { onSkipTo(i) }
                                .padding(vertical = 3.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                // Rounded square thumbnail
                                Box(
                                    Modifier
                                        .size(46.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                ) {
                                    PlayerThumbnail(item.thumbnail, 46.dp, 8.dp, false)
                                }

                                Spacer(Modifier.width(14.dp))

                                // Song title and artist - album
                                Column(Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    ) {
                                        Text(
                                            text = item.title,
                                            style = MaterialTheme.typography.bodyLarge.copy(
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                            ),
                                            color = if (isCurrent) accent else MaterialTheme.colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            modifier = Modifier.weight(1f, fill = false),
                                        )

                                        // Explicit badge if title contains explicit/live/acoustic indicators
                                        if (item.title.contains("Explicit", ignoreCase = true) || item.title.contains("Live", ignoreCase = true) && item.title.contains("Acoustic", ignoreCase = true)) {
                                            Surface(
                                                shape = RoundedCornerShape(3.dp),
                                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.25f),
                                            ) {
                                                Text(
                                                    "E",
                                                    style = MaterialTheme.typography.labelSmall.copy(
                                                        fontSize = 9.sp,
                                                        fontWeight = FontWeight.Bold,
                                                    ),
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp),
                                                )
                                            }
                                        }
                                    }

                                    Spacer(Modifier.height(2.dp))

                                    Text(
                                        text = "${item.artist} - ${item.artist}",
                                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }

                            // Subtle row separator line matching screenshot
                            Box(
                                Modifier
                                    .fillMaxWidth()
                                    .height(0.5.dp)
                                    .padding(top = 2.dp)
                                    .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun QueueHistoryScreen(
    queue: List<NowPlaying>,
    index: Int,
    language: String,
    onSkipTo: (Int) -> Unit,
    accent: Color = MaterialTheme.colorScheme.primary,
) {
    val historyItems = remember(queue, index) {
        if (index > 0) queue.take(index).reversed() else emptyList()
    }

    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp, vertical = 12.dp)
    ) {
        Text(
            text = Localization.get(language, "history"),
            style = MaterialTheme.typography.titleLarge.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
            ),
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(bottom = 12.dp),
        )

        if (historyItems.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    Localization.get(language, "history_empty"),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                itemsIndexed(historyItems, key = { i, item -> "${item.videoId}_$i" }) { i, item ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .clickable { onSkipTo(index - 1 - i) }
                            .padding(vertical = 3.dp)
                    ) {
                        Row(
                            Modifier.fillMaxWidth().padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                Modifier
                                    .size(46.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                PlayerThumbnail(item.thumbnail, 46.dp, 8.dp, false)
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    item.title,
                                    style = MaterialTheme.typography.bodyLarge.copy(
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                    ),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    "${item.artist} - ${item.artist}",
                                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 13.sp),
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .height(0.5.dp)
                                .padding(top = 2.dp)
                                .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.07f))
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun QueueScreen(
    queue: List<NowPlaying>,
    index: Int,
    language: String,
    onBack: () -> Unit,
    onSkipTo: (Int) -> Unit,
    onRemoveAt: (Int) -> Unit,
    onClear: () -> Unit,
    onReorder: (List<NowPlaying>) -> Unit,
    onAddToPlaylist: (NowPlaying) -> Unit,
) {
    val lazyListState = rememberLazyListState()
    val localQueue = remember { mutableStateListOf<NowPlaying>() }
    var hasDragged by remember { mutableStateOf(false) }

    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        localQueue.add(to.index, localQueue.removeAt(from.index))
        hasDragged = true
    }

    // Keep the local copy in sync with the real queue (skip while dragging).
    LaunchedEffect(queue) {
        if (!reorderableState.isAnyItemDragging) {
            localQueue.clear()
            localQueue.addAll(queue)
        }
    }

    // Commit the new order once the drag ends.
    LaunchedEffect(reorderableState.isAnyItemDragging) {
        if (!reorderableState.isAnyItemDragging && hasDragged) {
            onReorder(localQueue.toList())
            hasDragged = false
        }
    }

    val currentVideoId = queue.getOrNull(index)?.videoId

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Localization.get(language, "queue"), style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.weight(1f))
            if (queue.isNotEmpty()) {
                Button(onClick = onClear) { Text(Localization.get(language, "clear_queue")) }
            }
        }
        if (queue.isEmpty()) {
            Text(
                Localization.get(language, "queue_empty"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
        } else {
            Text(
                Localization.get(language, "drag_to_reorder"),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp),
            )
            LazyColumn(state = lazyListState, modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                itemsIndexed(localQueue, key = { _, item -> item.videoId }) { i, item ->
                    val isCurrent = item.videoId == currentVideoId
                    ReorderableItem(state = reorderableState, key = item.videoId) {
                        // Swipe gestures: swipe right to play, swipe left to remove.
                        var dragX by remember { mutableStateOf(0f) }
                        val density = LocalDensity.current
                        val threshold = with(density) { 64.dp.toPx() }
                        Box(Modifier.fillMaxWidth()) {
                            // Play hint revealed behind the row while swiping right.
                            val progress = (kotlin.math.abs(dragX) / threshold).coerceIn(0f, 1f)
                            Box(
                                Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.85f))
                                    .graphicsLayer { alpha = progress }
                                    .matchParentSize(),
                                contentAlignment = Alignment.CenterStart,
                            ) {
                                Text(
                                    "▶ " + Localization.get(language, "play"),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(start = 48.dp),
                                )
                            }
                            Row(
                                Modifier
                                    .fillMaxWidth()
                                    .graphicsLayer { translationX = dragX }
                                    .pointerInput(item.videoId) {
                                        detectHorizontalDragGestures(
                                            onHorizontalDrag = { _, dragAmount ->
                                                dragX = (dragX + dragAmount).coerceIn(-threshold * 2f, threshold * 2f)
                                            },
                                            onDragEnd = {
                                                when {
                                                    dragX < -threshold -> onRemoveAt(i)
                                                    dragX > threshold -> onSkipTo(i)
                                                }
                                                dragX = 0f
                                            },
                                            onDragCancel = { dragX = 0f },
                                        )
                                    }
                                    .clickable { onSkipTo(i) }
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    "⠿",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .draggableHandle()
                                        .padding(horizontal = 6.dp, vertical = 4.dp),
                                )
                                Text(
                                    if (isCurrent) "▶" else "${i + 1}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.padding(end = 8.dp),
                                )
                                Thumbnail(item.thumbnail, Modifier.size(44.dp))
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        item.title,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        item.artist,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                                IconButton(onClick = { onAddToPlaylist(item) }) {
                                    Icon(
                                        Icons.AutoMirrored.Filled.PlaylistAdd,
                                        contentDescription = Localization.get(language, "add_to_playlist"),
                                        tint = MaterialTheme.colorScheme.primary,
                                    )
                                }
                                Text(
                                    "✕",
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier
                                        .clickable { onRemoveAt(i) }
                                        .padding(horizontal = 10.dp, vertical = 4.dp),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun cleanLyrics(text: String): List<String> =
    text.lines()
        .map { it.replace(Regex("""\[\d{1,2}:\d{1,2}(\.\d{1,3})?\]"""), "").trim() }
        .filter { it.isNotEmpty() }

/** A single synced lyric line with its start time in milliseconds. */
private data class LyricLine(val timeMs: Long, val text: String)

/**
 * Parses LRC synced lyrics. Returns null when the text has no timestamps
 * (plain lyrics). Rich-sync `<mm:ss.xx>` word tags are stripped.
 */
private fun parseLrc(text: String): List<LyricLine>? {
    val timeRegex = Regex("""\[(\d{1,2}):(\d{1,2})(?:\.(\d{1,3}))?]""")
    val lineRegex = Regex("""((\[\d{1,2}:\d{1,2}(?:\.\d{1,3})?]\s*)+)(.*)""")
    val result = mutableListOf<LyricLine>()
    var hasTimestamps = false

    for (raw in text.lines()) {
        val line = raw.trim()
        if (line.isEmpty()) continue
        val match = lineRegex.find(line) ?: continue
        val timeTokens = match.groupValues[1]
        var content = match.groupValues[3]
            .replace(Regex("""<\d{1,2}:\d{2}(?:\.\d{1,3})?>\s*"""), "")
            .trim()
        if (content.isEmpty()) continue

        timeRegex.findAll(timeTokens).forEach { t ->
            val min = t.groupValues[1].toLongOrNull() ?: 0L
            val sec = t.groupValues[2].toLongOrNull() ?: 0L
            val frac = (t.groupValues[3].padEnd(3, '0').take(3).toLongOrNull() ?: 0L)
            result.add(LyricLine(min * 60_000 + sec * 1_000 + frac, content))
            hasTimestamps = true
        }
    }

    if (!hasTimestamps) return null
    return result.sortedBy { it.timeMs }
}

/** Index of the line currently being sung (last line with time <= position). */
private fun currentLineIndex(lines: List<LyricLine>, positionMs: Long): Int {
    var index = -1
    for (i in lines.indices) {
        if (lines[i].timeMs <= positionMs) index = i else break
    }
    return index
}

@Composable
fun LyricsScreen(
    nowPlaying: NowPlaying?,
    positionMs: Long,
    isPlaying: Boolean,
    language: String,
    synced: Boolean = true,
    textSizeSp: Float = 18f,
    lineSpacing: Float = 1.35f,
    onTogglePlay: () -> Unit = {},
    onBack: () -> Unit,
) {
    var lyrics by remember { mutableStateOf<String?>(null) }
    var loading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    val np = nowPlaying

    LaunchedEffect(np?.videoId) {
        if (np == null) {
            loading = false
            return@LaunchedEffect
        }
        loading = true
        val cached = LyricsCache.get(np.videoId)
        if (cached != null) {
            lyrics = cached
            error = null
            loading = false
            return@LaunchedEffect
        }
        LrcLib.getLyrics(title = np.title, artist = np.artist, duration = -1).fold(
            onSuccess = { lyrics = it; error = null; LyricsCache.put(np.videoId, it) },
            onFailure = { error = it.message },
        )
        loading = false
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        BackButton(language, onBack)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(Localization.get(language, "lyrics"), style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
            // Thumbnail play/pause (port of the mobile advanced-lyrics control).
            np?.let {
                Box {
                    Thumbnail(it.thumbnail, Modifier.size(40.dp).clip(RoundedCornerShape(8.dp)))
                    // Small play/pause overlay.
                    Box(
                        Modifier
                            .size(40.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.Black.copy(alpha = 0.45f))
                            .clickable(onClick = onTogglePlay),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }
        when {
            np == null -> Text(
                Localization.get(language, "nothing_playing"),
                style = MaterialTheme.typography.bodyLarge,
                modifier = Modifier.padding(top = 8.dp),
            )
            loading -> LoadingBox(language)
            error != null || lyrics == null -> Text(
                Localization.get(language, "no_lyrics"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            else -> {
                val lines = remember(lyrics) { parseLrc(lyrics.orEmpty()) }
                if (!synced || lines.isNullOrEmpty()) {
                    // Plain (non-synced) lyrics fallback.
                    Column(
                        Modifier.verticalScroll(rememberScrollState()).padding(top = 8.dp),
                    ) {
                        cleanLyrics(lyrics!!).forEach { line ->
                            Text(
                                line,
                                fontSize = textSizeSp.sp,
                                lineHeight = (textSizeSp * lineSpacing).sp,
                                modifier = Modifier.padding(vertical = 2.dp),
                            )
                        }
                    }
                } else {
                    val listState = rememberLazyListState()
                    var currentIndex by remember(lines) { mutableStateOf(-1) }
                    val latestPosition by rememberUpdatedState(positionMs)

                    // Debounce: poll the position ~5x/s and only commit the
                    // highlighted line when it actually changes, so the lyric
                    // list isn't recomposed on every decoded-frame position
                    // update (~40/s).
                    LaunchedEffect(lines) {
                        while (true) {
                            val idx = currentLineIndex(lines, latestPosition)
                            if (idx != currentIndex) currentIndex = idx
                            delay(200)
                        }
                    }

                    LaunchedEffect(currentIndex) {
                        if (currentIndex >= 0) {
                            listState.animateScrollToItem(maxOf(0, currentIndex - 3))
                        }
                    }
                    LazyColumn(state = listState, modifier = Modifier.fillMaxSize().padding(top = 8.dp)) {
                        itemsIndexed(lines) { i, line ->
                            val isCurrent = i == currentIndex
                            Text(
                                line.text,
                                fontSize = if (isCurrent) (textSizeSp + 4).sp else textSizeSp.sp,
                                lineHeight = (textSizeSp * lineSpacing).sp,
                                fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                color = if (isCurrent) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(vertical = 6.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun getArtistInitials(name: String): String {
    val parts = name.trim().split(" ").filter { it.isNotBlank() }
    return when {
        parts.isEmpty() -> "A"
        parts.size == 1 -> parts[0].take(2).uppercase()
        else -> "${parts[0].first()}${parts.last().first()}".uppercase()
    }
}

private fun getArtistGradient(name: String): List<Color> {
    val hash = name.hashCode()
    val palette = listOf(
        listOf(Color(0xFF6366F1), Color(0xFFA855F7)),
        listOf(Color(0xFFEC4899), Color(0xFF8B5CF6)),
        listOf(Color(0xFF3B82F6), Color(0xFF06B6D4)),
        listOf(Color(0xFF10B981), Color(0xFF059669)),
        listOf(Color(0xFFF59E0B), Color(0xFFEF4444)),
        listOf(Color(0xFF8B5CF6), Color(0xFFD946EF)),
    )
    return palette[kotlin.math.abs(hash) % palette.size]
}

@Composable
fun SpotifyRightNowPlayingPanel(
    nowPlaying: NowPlaying?,
    isPlaying: Boolean,
    positionMs: Long,
    language: String,
    onClose: () -> Unit,
    onOpenLyrics: () -> Unit,
    onAddToPlaylist: ((NowPlaying) -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val np = nowPlaying
    Surface(
        modifier = modifier.fillMaxHeight().width(310.dp),
        color = Color(0xFF121212),
        shape = RoundedCornerShape(12.dp),
    ) {
        if (np == null) {
            Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                Text(
                    Localization.get(language, "nothing_playing"),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Surface
        }

        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
        ) {
            // Panel Header
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    Localization.get(language, "now_playing"),
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(onClick = onClose) {
                    Icon(
                        Icons.Filled.Close,
                        contentDescription = Localization.get(language, "close"),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(12.dp))

            // Large Art Card
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(278.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.Black.copy(alpha = 0.2f)),
            ) {
                PlayerThumbnail(np.thumbnail, 278.dp, 16.dp, false, Modifier.fillMaxSize())
            }

            Spacer(Modifier.height(16.dp))

            // Song Info & Actions
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(Modifier.weight(1f)) {
                    Text(
                        np.title,
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Spacer(Modifier.height(2.dp))
                    Text(
                        np.artist,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                if (onAddToPlaylist != null) {
                    IconButton(onClick = { onAddToPlaylist(np) }) {
                        Icon(
                            Icons.AutoMirrored.Filled.PlaylistAdd,
                            contentDescription = Localization.get(language, "add_to_playlist"),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // "About the Artist" Card (Multi-Artist Aware + Dynamic Image Fetching)
            val artistList = remember(np.artist) {
                np.artist.split(",", "/", "&").map { it.trim() }.filter { it.isNotBlank() }
            }
            val artistThumbnails = remember { mutableStateMapOf<String, String>() }

            LaunchedEffect(artistList) {
                artistList.forEach { artistName ->
                    if (!artistThumbnails.containsKey(artistName)) {
                        withContext(Dispatchers.IO) {
                            try {
                                val result = YouTube.search(artistName, com.music.innertube.YouTube.SearchFilter.FILTER_ARTIST).getOrNull()
                                val artistItem = result?.items?.filterIsInstance<com.music.innertube.models.ArtistItem>()?.firstOrNull()
                                val thumb = artistItem?.thumbnail
                                if (!thumb.isNullOrBlank()) {
                                    artistThumbnails[artistName] = thumb
                                }
                            } catch (_: Exception) {}
                        }
                    }
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text(
                        if (artistList.size > 1) "About the artists" else "About the artist",
                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(12.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        artistList.forEachIndexed { index, artistName ->
                            val initials = getArtistInitials(artistName)
                            val gradientColors = getArtistGradient(artistName)
                            val thumbUrl = artistThumbnails[artistName]
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(10.dp))
                                    .padding(vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Box(
                                    Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(Brush.linearGradient(gradientColors)),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    if (!thumbUrl.isNullOrBlank()) {
                                        AsyncImage(
                                            model = thumbUrl,
                                            contentDescription = artistName,
                                            modifier = Modifier.fillMaxSize().clip(CircleShape),
                                            contentScale = ContentScale.Crop,
                                        )
                                    } else {
                                        Text(
                                            initials,
                                            style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                            color = Color.White,
                                        )
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        artistName,
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                    Text(
                                        if (index == 0) "Primary Artist" else "Featured Artist",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Quick Lyrics Card
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .clickable(onClick = onOpenLyrics),
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.AutoMirrored.Filled.Subject,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                Localization.get(language, "lyrics"),
                                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                            )
                        }
                        Icon(
                            Icons.AutoMirrored.Filled.KeyboardArrowRight,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Click to view synchronized lyrics",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
fun BoxScope.DesktopMiniPlayerBackgroundLayer(
    style: MiniPlayerBackgroundStyle,
    pureBlack: Boolean,
    thumbnailUrl: String?,
    modifier: Modifier = Modifier,
) {
    val dark = isSystemInDarkTheme()
    val baseSurface = if (pureBlack && dark) Color.Black else MaterialTheme.colorScheme.surfaceContainerHigh
    var extractedColors by remember(thumbnailUrl) { mutableStateOf<List<Color>>(emptyList()) }

    LaunchedEffect(thumbnailUrl, style) {
        if (!thumbnailUrl.isNullOrBlank() && (style == MiniPlayerBackgroundStyle.GRADIENT || style == MiniPlayerBackgroundStyle.GLOW_MOTION || style == MiniPlayerBackgroundStyle.LIVE_MESH)) {
            withContext(Dispatchers.IO) {
                runCatching {
                    val url = java.net.URL(thumbnailUrl)
                    val img = javax.imageio.ImageIO.read(url)
                    if (img != null) {
                        val colors = mutableListOf<Color>()
                        val stepX = (img.width / 4).coerceAtLeast(1)
                        val stepY = (img.height / 4).coerceAtLeast(1)
                        for (x in 0 until img.width step stepX) {
                            for (y in 0 until img.height step stepY) {
                                val rgb = img.getRGB(x, y)
                                val c = Color(
                                    red = ((rgb shr 16) and 0xFF) / 255f,
                                    green = ((rgb shr 8) and 0xFF) / 255f,
                                    blue = (rgb and 0xFF) / 255f,
                                )
                                colors.add(c)
                            }
                        }
                        colors.distinctBy { (it.red * 8).toInt() to (it.green * 8).toInt() to (it.blue * 8).toInt() }.take(4)
                    } else emptyList()
                }.getOrNull()?.let { extractedColors = it }
            }
        } else {
            extractedColors = emptyList()
        }
    }

    val glowTransition = rememberInfiniteTransition(label = "glowMotion")
    val glowShift by glowTransition.animateFloat(
        initialValue = -0.25f,
        targetValue = 0.25f,
        animationSpec = infiniteRepeatable(tween(4000, easing = LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "shift",
    )

    val meshTransition = rememberInfiniteTransition(label = "liveMesh")
    val meshDx by meshTransition.animateFloat(
        initialValue = -0.2f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(tween(5500, easing = LinearEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "dx",
    )

    Box(modifier = modifier.matchParentSize().clipToBounds()) {
        when (style) {
            MiniPlayerBackgroundStyle.FOLLOW_THEME -> {
                Box(Modifier.matchParentSize().background(baseSurface))
            }
            MiniPlayerBackgroundStyle.GRADIENT -> {
                val c1 = extractedColors.getOrNull(0) ?: MaterialTheme.colorScheme.primaryContainer
                val c2 = extractedColors.getOrNull(1) ?: MaterialTheme.colorScheme.surface
                Box(
                    Modifier.matchParentSize().background(
                        Brush.horizontalGradient(
                            listOf(c1.copy(alpha = 0.75f), c2.copy(alpha = 0.85f), baseSurface)
                        )
                    )
                )
            }
            MiniPlayerBackgroundStyle.BLUR -> {
                if (!thumbnailUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = thumbnailUrl,
                        contentDescription = null,
                        modifier = Modifier.matchParentSize().blur(24.dp).graphicsLayer { scaleX = 1.25f; scaleY = 1.25f },
                    )
                }
                Box(Modifier.matchParentSize().background(Color.Black.copy(alpha = 0.5f)))
            }
            MiniPlayerBackgroundStyle.GLOW_MOTION -> {
                val accent = extractedColors.firstOrNull() ?: MaterialTheme.colorScheme.primary
                val secAccent = extractedColors.getOrNull(1) ?: MaterialTheme.colorScheme.secondary
                Box(
                    Modifier
                        .matchParentSize()
                        .background(baseSurface)
                        .drawBehind {
                            val glowRadius = (size.width * 0.4f).coerceAtLeast(size.height * 2.5f)
                            drawRect(
                                brush = Brush.radialGradient(
                                    colors = listOf(
                                        accent.copy(alpha = 0.55f),
                                        secAccent.copy(alpha = 0.2f),
                                        Color.Transparent,
                                    ),
                                    center = Offset(size.width * (0.5f + glowShift), size.height * 0.5f),
                                    radius = glowRadius,
                                )
                            )
                        }
                )
            }
            MiniPlayerBackgroundStyle.LIVE_MESH -> {
                val accent = extractedColors.firstOrNull() ?: MaterialTheme.colorScheme.primary
                val secColor = MaterialTheme.colorScheme.secondary
                val tertColor = MaterialTheme.colorScheme.tertiary
                Box(
                    Modifier
                        .matchParentSize()
                        .background(baseSurface)
                        .drawBehind {
                            drawRect(
                                brush = Brush.linearGradient(
                                    colors = listOf(
                                        accent.copy(alpha = 0.4f),
                                        secColor.copy(alpha = 0.3f),
                                        tertColor.copy(alpha = 0.3f),
                                    ),
                                    start = Offset(size.width * meshDx, 0f),
                                    end = Offset(size.width * (1f + meshDx), size.height),
                                )
                            )
                        }
                )
            }
        }
    }
}

@Composable
fun ClassicDesktopMiniPlayer(
    nowPlaying: NowPlaying,
    isPlaying: Boolean,
    isLoading: Boolean,
    positionMs: Long,
    durationMs: Long,
    volume: Float,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    backgroundStyle: MiniPlayerBackgroundStyle,
    pureBlack: Boolean,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolume: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    showRightSidebar: Boolean,
    onToggleRightSidebar: () -> Unit,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    language: String,
    modifier: Modifier = Modifier,
) {
    val isDynamicBg = backgroundStyle != MiniPlayerBackgroundStyle.FOLLOW_THEME
    val contentColor = if (isDynamicBg || (pureBlack && isSystemInDarkTheme())) Color.White else MaterialTheme.colorScheme.onSurface
    val mutedColor = if (isDynamicBg || (pureBlack && isSystemInDarkTheme())) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = Color.Transparent,
    ) {
        Box(Modifier.fillMaxWidth()) {
            DesktopMiniPlayerBackgroundLayer(
                style = backgroundStyle,
                pureBlack = pureBlack,
                thumbnailUrl = nowPlaying.thumbnail,
            )

            Column {
                if (durationMs > 0) {
                    LinearProgressIndicator(
                        progress = { (positionMs.toFloat() / durationMs.toFloat()).coerceIn(0f, 1f) },
                        modifier = Modifier.fillMaxWidth().height(2.dp),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = mutedColor.copy(alpha = 0.2f),
                    )
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Left: Artwork & metadata
                    Row(
                        Modifier.weight(0.3f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Box(
                            Modifier
                                .size(52.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(onClick = onOpenPlayer),
                        ) {
                            Thumbnail(nowPlaying.thumbnail, Modifier.fillMaxSize())
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f).clickable(onClick = onOpenPlayer)) {
                            Text(
                                nowPlaying.title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                                color = contentColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Spacer(Modifier.height(2.dp))
                            Text(
                                nowPlaying.artist,
                                style = MaterialTheme.typography.bodySmall,
                                color = mutedColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }

                    // Center: Controls & seek slider
                    Column(
                        Modifier.weight(0.4f),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            IconButton(onClick = onToggleShuffle) {
                                Icon(
                                    Icons.Filled.Shuffle,
                                    contentDescription = Localization.get(language, "shuffle"),
                                    tint = if (isShuffle) MaterialTheme.colorScheme.primary else mutedColor,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                            IconButton(onClick = onPrevious) {
                                Icon(
                                    Icons.Filled.SkipPrevious,
                                    contentDescription = Localization.get(language, "previous"),
                                    tint = contentColor,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                            Box(
                                Modifier
                                    .size(36.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary)
                                    .clickable(onClick = onTogglePlay),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (isLoading) {
                                    CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = MaterialTheme.colorScheme.onPrimary)
                                } else {
                                    Icon(
                                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onPrimary,
                                        modifier = Modifier.size(22.dp),
                                    )
                                }
                            }
                            IconButton(onClick = onNext) {
                                Icon(
                                    Icons.Filled.SkipNext,
                                    contentDescription = Localization.get(language, "next"),
                                    tint = contentColor,
                                    modifier = Modifier.size(24.dp),
                                )
                            }
                            IconButton(onClick = onCycleRepeat) {
                                Icon(
                                    repeatIcon(repeatMode),
                                    contentDescription = Localization.get(language, "repeat"),
                                    tint = if (repeatMode != RepeatMode.OFF) MaterialTheme.colorScheme.primary else mutedColor,
                                    modifier = Modifier.size(18.dp),
                                )
                            }
                        }

                        Spacer(Modifier.height(4.dp))

                        Row(
                            Modifier.fillMaxWidth(0.9f),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                formatTime(positionMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = mutedColor,
                            )
                            Spacer(Modifier.width(8.dp))
                            ViviSlider(
                                value = positionMs.toFloat().coerceIn(0f, durationMs.coerceAtLeast(1L).toFloat()),
                                onValueChange = { onSeek(it.toLong()) },
                                valueRange = 0f..durationMs.coerceAtLeast(1L).toFloat(),
                                style = ViviSliderStyle.SLIM,
                                modifier = Modifier.weight(1f),
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                formatTime(durationMs),
                                style = MaterialTheme.typography.labelSmall,
                                color = mutedColor,
                            )
                        }
                    }

                    // Right: Volume & layout controls
                    Row(
                        Modifier.weight(0.3f),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onOpenLyrics) {
                            Icon(
                                Icons.AutoMirrored.Filled.Subject,
                                contentDescription = Localization.get(language, "lyrics"),
                                tint = mutedColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        IconButton(onClick = onOpenQueue) {
                            Icon(
                                Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = Localization.get(language, "queue"),
                                tint = mutedColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        Row(
                            Modifier.width(110.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                volumeIcon(volume),
                                contentDescription = Localization.get(language, "volume"),
                                tint = mutedColor,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(Modifier.width(4.dp))
                            ViviSlider(
                                value = volume.coerceIn(0f, 1f),
                                onValueChange = onVolume,
                                valueRange = 0f..1f,
                                style = ViviSliderStyle.SLIM,
                                modifier = Modifier.weight(1f),
                            )
                        }
                        Spacer(Modifier.width(4.dp))
                        IconButton(onClick = onToggleRightSidebar) {
                            Icon(
                                Icons.AutoMirrored.Filled.QueueMusic,
                                contentDescription = "Right Panel",
                                tint = if (showRightSidebar) MaterialTheme.colorScheme.primary else mutedColor,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                        IconButton(onClick = onOpenPlayer) {
                            Icon(
                                Icons.Filled.Fullscreen,
                                contentDescription = "Open Full Player",
                                tint = mutedColor,
                                modifier = Modifier.size(22.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NewDesktopMiniPlayer(
    nowPlaying: NowPlaying,
    isPlaying: Boolean,
    isLoading: Boolean,
    positionMs: Long,
    durationMs: Long,
    volume: Float,
    backgroundStyle: MiniPlayerBackgroundStyle,
    pureBlack: Boolean,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onVolume: (Float) -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenQueue: () -> Unit,
    language: String,
    modifier: Modifier = Modifier,
) {
    val isDynamicBg = backgroundStyle != MiniPlayerBackgroundStyle.FOLLOW_THEME
    val contentColor = if (isDynamicBg || (pureBlack && isSystemInDarkTheme())) Color.White else MaterialTheme.colorScheme.onSurface
    val mutedColor = if (isDynamicBg || (pureBlack && isSystemInDarkTheme())) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary
    val progress = (positionMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
    var isFavorite by remember { mutableStateOf(false) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(28.dp))
    ) {
        DesktopMiniPlayerBackgroundLayer(
            style = backgroundStyle,
            pureBlack = pureBlack,
            thumbnailUrl = nowPlaying.thumbnail,
        )

        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Circular Artwork Thumbnail with Song Progress Arc & Play/Pause Button Inside Cover
            Box(
                Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center,
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    drawArc(
                        color = primaryColor.copy(alpha = 0.25f),
                        startAngle = 0f,
                        sweepAngle = 360f,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                    drawArc(
                        color = primaryColor,
                        startAngle = -90f,
                        sweepAngle = 360f * progress,
                        useCenter = false,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )
                }
                Box(Modifier.size(38.dp).clip(CircleShape)) {
                    Thumbnail(nowPlaying.thumbnail, Modifier.fillMaxSize())
                    Box(
                        Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.35f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                        } else {
                            Icon(
                                if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                                contentDescription = if (isPlaying) "Pause" else "Play",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // Metadata
            Column(
                Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenPlayer),
            ) {
                Text(
                    nowPlaying.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    nowPlaying.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(10.dp))

            // Right Action Controls matching Mobile New MiniPlayer Design
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                IconButton(onClick = { onVolume((volume + 0.1f) % 1.05f) }) {
                    Icon(
                        Icons.Filled.SpeakerGroup,
                        contentDescription = "Output device",
                        tint = mutedColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = { isFavorite = !isFavorite }) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = if (isFavorite) primaryColor else mutedColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onNext) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = Localization.get(language, "next"),
                        tint = contentColor,
                        modifier = Modifier.size(24.dp),
                    )
                }
                IconButton(onClick = onOpenQueue) {
                    Icon(
                        Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = Localization.get(language, "queue"),
                        tint = mutedColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onOpenPlayer) {
                    Icon(
                        Icons.Filled.Fullscreen,
                        contentDescription = "Open Full Player",
                        tint = contentColor,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun AppleDesktopMiniPlayer(
    nowPlaying: NowPlaying,
    isPlaying: Boolean,
    isLoading: Boolean,
    positionMs: Long,
    durationMs: Long,
    backgroundStyle: MiniPlayerBackgroundStyle,
    pureBlack: Boolean,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    language: String,
    modifier: Modifier = Modifier,
) {
    val isDynamicBg = backgroundStyle != MiniPlayerBackgroundStyle.FOLLOW_THEME
    val contentColor = if (isDynamicBg || (pureBlack && isSystemInDarkTheme())) Color.White else MaterialTheme.colorScheme.onSurface
    val mutedColor = if (isDynamicBg || (pureBlack && isSystemInDarkTheme())) Color.White.copy(alpha = 0.7f) else MaterialTheme.colorScheme.onSurfaceVariant
    val primaryColor = MaterialTheme.colorScheme.primary

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(64.dp)
            .clip(RoundedCornerShape(12.dp))
    ) {
        DesktopMiniPlayerBackgroundLayer(
            style = backgroundStyle,
            pureBlack = pureBlack,
            thumbnailUrl = nowPlaying.thumbnail,
        )

        // Bottom 3dp Progress Bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(3.dp)
                .align(Alignment.BottomCenter)
                .drawBehind {
                    val progress = (positionMs.toFloat() / durationMs.coerceAtLeast(1L).toFloat()).coerceIn(0f, 1f)
                    drawRect(mutedColor.copy(alpha = 0.2f))
                    drawRect(primaryColor, size = Size(size.width * progress, size.height))
                }
        )

        Row(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Squircle Artwork Thumbnail with Play/Pause Button Inside Cover
            Box(
                Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .clickable(onClick = onTogglePlay),
                contentAlignment = Alignment.Center,
            ) {
                Thumbnail(nowPlaying.thumbnail, Modifier.fillMaxSize())
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f)),
                    contentAlignment = Alignment.Center,
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Icon(
                            if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            Spacer(Modifier.width(12.dp))

            // Metadata
            Column(
                Modifier
                    .weight(1f)
                    .clickable(onClick = onOpenPlayer),
            ) {
                Text(
                    nowPlaying.title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
                    color = contentColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    nowPlaying.artist,
                    style = MaterialTheme.typography.bodySmall,
                    color = mutedColor,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            Spacer(Modifier.width(12.dp))

            // Apple Control Buttons
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                IconButton(onClick = onNext) {
                    Icon(
                        Icons.Filled.SkipNext,
                        contentDescription = Localization.get(language, "next"),
                        tint = contentColor,
                        modifier = Modifier.size(26.dp),
                    )
                }
                IconButton(onClick = onOpenLyrics) {
                    Icon(
                        Icons.AutoMirrored.Filled.Subject,
                        contentDescription = Localization.get(language, "lyrics"),
                        tint = mutedColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onOpenQueue) {
                    Icon(
                        Icons.AutoMirrored.Filled.QueueMusic,
                        contentDescription = Localization.get(language, "queue"),
                        tint = mutedColor,
                        modifier = Modifier.size(20.dp),
                    )
                }
                IconButton(onClick = onOpenPlayer) {
                    Icon(
                        Icons.Filled.Fullscreen,
                        contentDescription = "Open Full Player",
                        tint = contentColor,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }
        }
    }
}

@Composable
fun DesktopMiniPlayer(
    nowPlaying: NowPlaying?,
    isPlaying: Boolean,
    isLoading: Boolean,
    positionMs: Long,
    durationMs: Long,
    volume: Float,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    design: MiniPlayerDesign = MiniPlayerDesign.CLASSIC,
    backgroundStyle: MiniPlayerBackgroundStyle = MiniPlayerBackgroundStyle.FOLLOW_THEME,
    pureBlack: Boolean = false,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolume: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    showRightSidebar: Boolean,
    onToggleRightSidebar: () -> Unit,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    language: String,
    modifier: Modifier = Modifier,
) {
    val np = nowPlaying ?: return

    when (design) {
        MiniPlayerDesign.NEW -> {
            NewDesktopMiniPlayer(
                nowPlaying = np,
                isPlaying = isPlaying,
                isLoading = isLoading,
                positionMs = positionMs,
                durationMs = durationMs,
                volume = volume,
                backgroundStyle = backgroundStyle,
                pureBlack = pureBlack,
                onTogglePlay = onTogglePlay,
                onNext = onNext,
                onVolume = onVolume,
                onOpenPlayer = onOpenPlayer,
                onOpenQueue = onOpenQueue,
                language = language,
                modifier = modifier,
            )
        }
        MiniPlayerDesign.APPLE -> {
            AppleDesktopMiniPlayer(
                nowPlaying = np,
                isPlaying = isPlaying,
                isLoading = isLoading,
                positionMs = positionMs,
                durationMs = durationMs,
                backgroundStyle = backgroundStyle,
                pureBlack = pureBlack,
                onTogglePlay = onTogglePlay,
                onNext = onNext,
                onOpenPlayer = onOpenPlayer,
                onOpenQueue = onOpenQueue,
                onOpenLyrics = onOpenLyrics,
                language = language,
                modifier = modifier,
            )
        }
        else -> {
            ClassicDesktopMiniPlayer(
                nowPlaying = np,
                isPlaying = isPlaying,
                isLoading = isLoading,
                positionMs = positionMs,
                durationMs = durationMs,
                volume = volume,
                isShuffle = isShuffle,
                repeatMode = repeatMode,
                backgroundStyle = backgroundStyle,
                pureBlack = pureBlack,
                onTogglePlay = onTogglePlay,
                onNext = onNext,
                onPrevious = onPrevious,
                onSeek = onSeek,
                onVolume = onVolume,
                onToggleShuffle = onToggleShuffle,
                onCycleRepeat = onCycleRepeat,
                onOpenPlayer = onOpenPlayer,
                onOpenQueue = onOpenQueue,
                onOpenLyrics = onOpenLyrics,
                showRightSidebar = showRightSidebar,
                onToggleRightSidebar = onToggleRightSidebar,
                isFullscreen = isFullscreen,
                onToggleFullscreen = onToggleFullscreen,
                language = language,
                modifier = modifier,
            )
        }
    }
}

/** Legacy Spotify-style player bar now delegates to [DesktopMiniPlayer]. */
@Composable
fun SpotifyPlayerBar(
    nowPlaying: NowPlaying?,
    isPlaying: Boolean,
    isLoading: Boolean,
    positionMs: Long,
    durationMs: Long,
    volume: Float,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    miniPlayerDesign: MiniPlayerDesign = MiniPlayerDesign.CLASSIC,
    miniPlayerBackgroundStyle: MiniPlayerBackgroundStyle = MiniPlayerBackgroundStyle.FOLLOW_THEME,
    pureBlackMiniPlayer: Boolean = false,
    onTogglePlay: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onVolume: (Float) -> Unit,
    onToggleShuffle: () -> Unit,
    onCycleRepeat: () -> Unit,
    onOpenPlayer: () -> Unit,
    onOpenQueue: () -> Unit,
    onOpenLyrics: () -> Unit,
    showRightSidebar: Boolean,
    onToggleRightSidebar: () -> Unit,
    isFullscreen: Boolean,
    onToggleFullscreen: () -> Unit,
    language: String,
    modifier: Modifier = Modifier,
) {
    DesktopMiniPlayer(
        nowPlaying = nowPlaying,
        isPlaying = isPlaying,
        isLoading = isLoading,
        positionMs = positionMs,
        durationMs = durationMs,
        volume = volume,
        isShuffle = isShuffle,
        repeatMode = repeatMode,
        design = miniPlayerDesign,
        backgroundStyle = miniPlayerBackgroundStyle,
        pureBlack = pureBlackMiniPlayer,
        onTogglePlay = onTogglePlay,
        onNext = onNext,
        onPrevious = onPrevious,
        onSeek = onSeek,
        onVolume = onVolume,
        onToggleShuffle = onToggleShuffle,
        onCycleRepeat = onCycleRepeat,
        onOpenPlayer = onOpenPlayer,
        onOpenQueue = onOpenQueue,
        onOpenLyrics = onOpenLyrics,
        showRightSidebar = showRightSidebar,
        onToggleRightSidebar = onToggleRightSidebar,
        isFullscreen = isFullscreen,
        onToggleFullscreen = onToggleFullscreen,
        language = language,
        modifier = modifier,
    )
}

/**
 * Cider-style fullscreen lyrics focus mode: the artwork fills the screen as a
 * blurred backdrop, the synced lyrics sit centered on top, and a compact
 * transport bar (previous / play-pause / next / back) stays at the bottom.
 * The standard [LyricsScreen] provides the actual lyric list + loading states.
 */
@Composable
fun LyricsFocusScreen(
    nowPlaying: NowPlaying?,
    positionMs: Long,
    isPlaying: Boolean,
    language: String,
    synced: Boolean,
    textSizeSp: Float,
    lineSpacing: Float,
    bgUrl: String? = null,
    accent: Color = MaterialTheme.colorScheme.primary,
    onTogglePlay: () -> Unit = {},
    onNext: () -> Unit = {},
    onPrevious: () -> Unit = {},
    onBack: () -> Unit,
) {
    // Resolve the animated/canvas artwork backdrop, mirroring PlayerScreen.
    var canvasArt by remember { mutableStateOf<CanvasArtwork?>(null) }
    LaunchedEffect(nowPlaying?.videoId) {
        canvasArt = null
        val track = nowPlaying ?: return@LaunchedEffect
        val settings = DesktopSettings.load()
        canvasArt = if (settings.canvasEnabled) {
            withContext(Dispatchers.IO) {
                CanvasResolver.resolve(track.title, track.artist, null, CanvasSource.from(settings.canvasSource))
            }
        } else {
            null
        }
    }
    val resolvedBg = CanvasResolver.displayUrl(canvasArt, nowPlaying?.thumbnail) ?: bgUrl

    Box(Modifier.fillMaxSize()) {
        // Backdrop: blurred artwork (or accent wash when unavailable).
        if (resolvedBg != null) {
            AsyncImage(
                model = resolvedBg,
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(46.dp)
                    .graphicsLayer { scaleX = 1.2f; scaleY = 1.2f },
            )
        } else {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to accent.copy(alpha = 0.5f),
                            1f to Color.Black.copy(alpha = 0.85f),
                        )
                    )
            )
        }
        // Scrim so the white lyrics text always reads.
        Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f)))

        LyricsScreen(
            nowPlaying = nowPlaying,
            positionMs = positionMs,
            isPlaying = isPlaying,
            language = language,
            synced = synced,
            textSizeSp = textSizeSp,
            lineSpacing = lineSpacing,
            onTogglePlay = onTogglePlay,
            onBack = onBack,
        )

        // Bottom transport bar.
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.45f))
                .padding(horizontal = 24.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            IconButton(onClick = onPrevious) {
                Icon(
                    Icons.Filled.SkipPrevious,
                    contentDescription = Localization.get(language, "previous"),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
            Spacer(Modifier.width(20.dp))
            FilledIconButton(
                onClick = onTogglePlay,
                modifier = Modifier.size(56.dp),
                colors = IconButtonDefaults.filledIconButtonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Icon(
                    if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                    contentDescription = Localization.get(language, if (isPlaying) "pause" else "play"),
                    modifier = Modifier.size(30.dp),
                )
            }
            Spacer(Modifier.width(20.dp))
            IconButton(onClick = onNext) {
                Icon(
                    Icons.Filled.SkipNext,
                    contentDescription = Localization.get(language, "next"),
                    tint = Color.White,
                    modifier = Modifier.size(32.dp),
                )
            }
        }
    }
}
