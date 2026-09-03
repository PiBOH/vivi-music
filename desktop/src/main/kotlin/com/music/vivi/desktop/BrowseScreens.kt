package com.music.vivi.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.Icon
import androidx.compose.ui.Alignment
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items as gridItems
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.clickable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.music.innertube.YouTube
import com.music.innertube.models.SongItem
import com.music.innertube.models.YTItem
import com.music.innertube.pages.BrowseResult
import com.music.innertube.pages.HomePage
import com.music.innertube.pages.MoodAndGenres
import com.music.innertube.pages.SearchResult
import com.music.innertube.pages.SearchSummary
import com.music.innertube.pages.SearchSummaryPage

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.graphics.Brush
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import java.time.LocalTime

data class CuratedMix(
    val title: String,
    val subtitle: String,
    val gradientColors: List<Color>,
)

@Composable
fun HomeScreen(
    language: String,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
    onPlayAll: (List<SongItem>) -> Unit,
    onOpenBrowse: (String, String?) -> Unit,
    userName: String = "Guest",
    useLastListen: Boolean = false,
    onUseLastListenChange: (Boolean) -> Unit = {},
    randomizeOrder: Boolean = false,
    onRandomizeOrderChange: (Boolean) -> Unit = {},
    wrappedStats: WrappedStats = WrappedStats(),
    showWrapped: Boolean = false,
) {
    var home by remember { mutableStateOf<HomePage?>(null) }
    var moodAndGenres by remember { mutableStateOf<List<MoodAndGenres>?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var selectedChip by remember { mutableStateOf<HomePage.Chip?>(null) }

    LaunchedEffect(selectedChip) {
        val params = selectedChip?.endpoint?.params
        YouTube.home(params = params).fold(
            onSuccess = { home = it; error = null },
            onFailure = { error = it.message },
        )
    }

    LaunchedEffect(Unit) {
        YouTube.moodAndGenres().fold(
            onSuccess = { moodAndGenres = it },
            onFailure = { /* mood & genres is optional */ },
        )
    }

    val greetingText = remember(language) {
        val hour = LocalTime.now().hour
        when {
            hour < 12 -> Localization.get(language, "home_greeting_morning")
            hour < 17 -> Localization.get(language, "home_greeting_afternoon")
            else -> Localization.get(language, "home_greeting_evening")
        }
    }

    // Expressive Spotify/Apple-style mix cards, but derived from the theme's
    // accent (hue rotations of it) so they stay inside the Material palette.
    val accent = MaterialTheme.colorScheme.primary
    val curatedMixes = remember(accent) {
        listOf(
            CuratedMix("Get Up! Mix", "Upbeat Essentials", listOf(accent, rotateHue(accent, 40f))),
            CuratedMix("Chill Mix", "Ambient & Relaxing", listOf(rotateHue(accent, 160f), rotateHue(accent, 200f))),
            CuratedMix("New Music Mix", "Fresh Releases", listOf(rotateHue(accent, 280f), rotateHue(accent, 320f))),
            CuratedMix("Heavy Rotation Mix", "Most Played", listOf(rotateHue(accent, 60f), rotateHue(accent, 100f))),
            CuratedMix("Focus Mix", "Deep Concentration", listOf(rotateHue(accent, 220f), rotateHue(accent, 260f))),
        )
    }

    when {
        error != null && home == null -> ErrorBox(language, error)
        home == null -> LoadingBox(language)
        else -> LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .padding(horizontal = 24.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // 1. Greeting Header Row
            item(key = "greeting_header") {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        val displayName = userName.ifBlank { "Guest" }
                        Text(
                            text = "$greetingText, $displayName",
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Tooltip(Localization.get(language, "notifications")) {
                        IconButton(
                            onClick = { /* Home hub / notifications */ },
                            colors = IconButtonDefaults.iconButtonColors(
                                containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                contentColor = MaterialTheme.colorScheme.onSurface,
                            ),
                            modifier = Modifier.size(42.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Notifications,
                                contentDescription = "Notifications",
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                }
            }

            // "VIVI Wrapped" session stats card (if active).
            if (showWrapped && wrappedStats.trackStarts > 0) {
                item(key = "wrapped") {
                    WrappedCard(wrappedStats = wrappedStats, language = language)
                }
            }

            // 2. Segmented Capsule Pill Control Row
            item(key = "home_toggles") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    ) {
                        Row(
                            modifier = Modifier.padding(3.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val quickPicksSelected = !useLastListen
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (quickPicksSelected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable { onUseLastListenChange(false) }
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = Localization.get(language, "quick_picks"),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (quickPicksSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (quickPicksSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }

                            val lastListenSelected = useLastListen
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(50))
                                    .background(
                                        if (lastListenSelected) MaterialTheme.colorScheme.primaryContainer
                                        else Color.Transparent
                                    )
                                    .clickable { onUseLastListenChange(true) }
                                    .padding(horizontal = 16.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    text = Localization.get(language, "last_listen"),
                                    style = MaterialTheme.typography.labelLarge.copy(
                                        fontWeight = if (lastListenSelected) FontWeight.Bold else FontWeight.Medium
                                    ),
                                    color = if (lastListenSelected) MaterialTheme.colorScheme.onPrimaryContainer
                                    else MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                        }
                    }

                    Spacer(Modifier.weight(1f))

                    val shuffleColor = if (randomizeOrder) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    Surface(
                        onClick = { onRandomizeOrderChange(!randomizeOrder) },
                        shape = RoundedCornerShape(50),
                        color = if (randomizeOrder) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                        else MaterialTheme.colorScheme.surfaceContainerHigh,
                        modifier = Modifier.height(34.dp),
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Icon(
                                Icons.Filled.Shuffle,
                                contentDescription = Localization.get(language, "randomize_home_order"),
                                tint = shuffleColor,
                                modifier = Modifier.size(16.dp),
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = Localization.get(language, "randomize"),
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                                color = shuffleColor
                            )
                        }
                    }
                }
            }

            val page = home!!

            // Chips row (if available)
            val chipsList = page.chips.orEmpty().filter { !it.title.equals("Podcasts", ignoreCase = true) }
            if (chipsList.isNotEmpty()) {
                item(key = "chips") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                    ) {
                        items(chipsList, key = { chip -> chip.title }) { chip ->
                            val selected = selectedChip?.title == chip.title
                            Surface(
                                onClick = { selectedChip = if (selected) null else chip },
                                shape = RoundedCornerShape(50),
                                color = if (selected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier.height(32.dp),
                            ) {
                                Box(
                                    modifier = Modifier.padding(horizontal = 14.dp),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Text(
                                        text = chip.title,
                                        style = MaterialTheme.typography.labelMedium.copy(
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium
                                        ),
                                        color = if (selected) MaterialTheme.colorScheme.onPrimary
                                        else MaterialTheme.colorScheme.onSurface,
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 3. Recently Played Section
            val wantedSections = page.sections
                .filter { s ->
                    val t = s.title.lowercase()
                    when {
                        t.contains("quick picks") -> !useLastListen
                        t.contains("last listen") -> useLastListen
                        else -> true
                    }
                }
                .toMutableList()
            if (randomizeOrder) wantedSections.shuffle()

            wantedSections.forEachIndexed { index, section ->
                val songs = section.items.filterIsInstance<SongItem>()
                val isSongsOnly = section.items.isNotEmpty() && songs.size == section.items.size

                item(key = "header-$index-${section.title}") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface,
                        )

                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val endpoint = section.endpoint
                            if (endpoint != null) {
                                Tooltip(Localization.get(language, "see_all")) {
                                    IconButton(
                                        onClick = { onOpenBrowse(endpoint.browseId, endpoint.params) },
                                        colors = IconButtonDefaults.iconButtonColors(
                                            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                                            contentColor = MaterialTheme.colorScheme.onSurface,
                                        ),
                                        modifier = Modifier.size(32.dp),
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                            contentDescription = Localization.get(language, "view_section"),
                                            modifier = Modifier.size(18.dp),
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                item(key = "content-$index-${section.title}") {
                    if (isSongsOnly) {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            val sectionSongs = songs.distinctBy { it.id }
                            items(sectionSongs, key = { it.id }) { song ->
                                Box(Modifier.width(300.dp)) {
                                    SongRow(
                                        song = song,
                                        language = language,
                                        // Like the Android app: tapping a song in a Home
                                        // recommendation section plays the WHOLE section as a
                                        // queue, starting from the tapped track (next/prev move
                                        // through the rest of the section).
                                        onClick = {
                                            val start = sectionSongs.indexOfFirst { it.id == song.id }.coerceAtLeast(0)
                                            val rotated = sectionSongs.slice(start until sectionSongs.size) +
                                                sectionSongs.slice(0 until start)
                                            onPlayAll(rotated)
                                        },
                                        onAddToPlaylist = { onAddToPlaylist(song) },
                                    )
                                }
                            }
                        }
                    } else {
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                        ) {
                            items(section.items, key = { it.id }) { item ->
                                YtItemCard(
                                    item = item,
                                    onClick = { onItemClick(item, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong) },
                                )
                            }
                        }
                    }
                }
            }

            // 4. Your Artists Feed Section
            item(key = "artists_feed_header") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onOpenBrowse("FEmusic_library_corpus", null) },
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = Localization.get(language, "your_artists_feed"),
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            // 5. Made For You Section (Dynamic Gradient Cards)
            item(key = "made_for_you_header") {
                Text(
                    text = Localization.get(language, "made_for_you"),
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }

            item(key = "made_for_you_list") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    items(curatedMixes, key = { it.title }) { mix ->
                        Box(
                            modifier = Modifier
                                .width(220.dp)
                                .height(260.dp)
                                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 10.dp, bottomEnd = 28.dp, bottomStart = 10.dp))
                                .background(Brush.linearGradient(mix.gradientColors))
                                .clickable { onOpenBrowse("FEmusic_home", null) }
                                .padding(20.dp),
                            contentAlignment = Alignment.BottomStart,
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "VIVI MUSIC",
                                    style = MaterialTheme.typography.labelSmall.copy(
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = androidx.compose.ui.unit.TextUnit(1.5f, androidx.compose.ui.unit.TextUnitType.Sp)
                                    ),
                                    color = Color.White.copy(alpha = 0.85f),
                                )
                                Text(
                                    text = mix.title,
                                    style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
                                    color = Color.White,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis,
                                )
                                Text(
                                    text = mix.subtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color.White.copy(alpha = 0.75f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            }
                        }
                    }
                }
            }

            // The API can repeat the same mood/genre (identical browseId+title)
            // across category sections; the LazyRow below is keyed by
            // `browseId + title`, so duplicates must be removed or Compose
            // crashes with "key ... was already used" as soon as the row
            // scrolls into view.
            val moodItems = moodAndGenres
                ?.flatMap { it.items }
                ?.distinctBy { it.endpoint.browseId + it.title }
            if (!moodItems.isNullOrEmpty()) {
                item(key = "mood_header") {
                    SectionHeader(title = Localization.get(language, "mood_and_genres"), language = language)
                }
                item(key = "mood_list") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(moodItems, key = { it.endpoint.browseId + it.title }) { item ->
                            MoodAndGenresButton(
                                title = item.title,
                                onClick = { onOpenBrowse(item.endpoint.browseId, item.endpoint.params) },
                                modifier = Modifier.width(180.dp),
                            )
                        }
                    }
                }
            }

            item(key = "bottom_spacer") {
                Box(Modifier.fillMaxWidth().padding(bottom = 24.dp))
            }
        }
    }
}

/** "VIVI Wrapped" card showing the current session's listening stats. */
@Composable
fun WrappedCard(wrappedStats: WrappedStats, language: String) {
    val minutes = wrappedStats.playedMs / 60_000
    val timeText = if (minutes >= 60) {
        "${minutes / 60}h ${minutes % 60}m"
    } else {
        "${minutes}m"
    }
    val accent = MaterialTheme.colorScheme.primary
    Card(
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 12.dp, bottomEnd = 28.dp, bottomStart = 12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(
                Localization.get(language, "wrapped_title"),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer,
            )
            Spacer(Modifier.height(8.dp))
            Row(Modifier.fillMaxWidth()) {
                WrappedStat(
                    value = wrappedStats.trackStarts.toString(),
                    label = Localization.get(language, "wrapped_tracks"),
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
                WrappedStat(
                    value = timeText,
                    label = Localization.get(language, "wrapped_listening_time"),
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
                WrappedStat(
                    value = wrappedStats.topSongCount.toString(),
                    label = wrappedStats.topSongTitle?.take(18) ?: Localization.get(language, "wrapped_top_song"),
                    accent = accent,
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun WrappedStat(value: String, label: String, accent: Color, modifier: Modifier = Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineSmall, color = accent)
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onPrimaryContainer,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** Generic browse screen (mood/genre pages, "More" endpoints, etc.). */
@Composable
fun BrowseScreen(
    browseId: String,
    params: String?,
    language: String,
    gridItemSize: Int,
    onBack: () -> Unit,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
) {
    var result by remember { mutableStateOf<BrowseResult?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(browseId, params) {
        YouTube.browse(browseId, params).fold(
            onSuccess = { result = it; error = null },
            onFailure = { error = it.message },
        )
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        when {
            error != null -> ErrorBox(language, error)
            result == null -> LoadingBox(language)
            else -> LazyVerticalGrid(
                columns = GridCells.Adaptive(gridItemSize.dp),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                result!!.items.forEach { section ->
                    if (!section.title.isNullOrBlank()) {
                        item(key = "header-${section.title}", span = { GridItemSpan(maxLineSpan) }) {
                            SectionHeader(title = section.title!!, language = language)
                        }
                    }
                    section.items.forEach { ytItem ->
                        item(key = ytItem.id) {
                            YtItemCard(
                                item = ytItem,
                                width = null,
                                onClick = { onItemClick(ytItem, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SearchScreen(
    language: String,
    gridItemSize: Int,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
    searchHistory: List<String> = emptyList(),
    onRecordSearch: (String) -> Unit = {},
    onClearSearchHistory: () -> Unit = {},
    externalQuery: String? = null,
    onQueryChange: ((String) -> Unit)? = null,
    showTextField: Boolean = false,
    externalFilter: YouTube.SearchFilter? = null,
    onFilterChange: ((YouTube.SearchFilter?) -> Unit)? = null,
    showFiltersInBody: Boolean = false,
) {
    var internalQuery by remember { mutableStateOf("") }
    val query = externalQuery ?: internalQuery
    val updateQuery: (String) -> Unit = { newQ ->
        onQueryChange?.invoke(newQ)
        if (externalQuery == null) {
            internalQuery = newQ
        }
    }

    var page by remember { mutableStateOf<SearchSummaryPage?>(null) }
    var filterItems by remember { mutableStateOf<List<YTItem>?>(null) }
    var internalFilter by remember { mutableStateOf<YouTube.SearchFilter?>(null) }
    val selectedFilter = externalFilter ?: internalFilter
    val setSelectedFilter: (YouTube.SearchFilter?) -> Unit = { f ->
        onFilterChange?.invoke(f)
        if (externalFilter == null) {
            internalFilter = f
        }
    }

    var suggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    var focused by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }

    // Results: summary when "All", filtered results when a chip is selected.
    LaunchedEffect(query, selectedFilter) {
        val q = query.trim()
        if (q.isEmpty()) {
            page = null
            filterItems = null
            error = null
            loading = false
            return@LaunchedEffect
        }
        loading = true
        val filter = selectedFilter
        if (filter == null) {
            YouTube.searchSummary(q).fold(
                onSuccess = { page = it; filterItems = null; error = null },
                onFailure = { error = it.message },
            )
        } else {
            YouTube.search(q, filter).fold(
                onSuccess = { page = null; filterItems = it.items.distinctBy { item -> item.id }; error = null },
                onFailure = { error = it.message },
            )
        }
        loading = false
    }

    // Live suggestions while typing (hidden once results are shown).
    LaunchedEffect(query, focused) {
        val q = query.trim()
        if (!focused || q.isEmpty() || page != null || filterItems != null) {
            suggestions = emptyList()
            return@LaunchedEffect
        }
        suggestions = YouTube.searchSuggestions(q).getOrNull()?.queries.orEmpty().take(6)
    }

    Column(Modifier.fillMaxSize().padding(16.dp)) {
        if (showTextField) {
            OutlinedTextField(
                value = query,
                onValueChange = updateQuery,
                modifier = Modifier.fillMaxWidth().onFocusChanged { focused = it.isFocused },
                singleLine = true,
                placeholder = { Text(Localization.get(language, "search_placeholder")) },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = {
                    val q = query.trim()
                    if (q.isNotEmpty()) onRecordSearch(q)
                }),
            )
        }

        // Filter chips (All / Songs / Videos / Albums / Artists / Playlists).
        if (showFiltersInBody) {
            val filters = listOf(
                null to Localization.get(language, "filter_all"),
                YouTube.SearchFilter.FILTER_SONG to Localization.get(language, "filter_songs"),
                YouTube.SearchFilter.FILTER_VIDEO to Localization.get(language, "filter_videos"),
                YouTube.SearchFilter.FILTER_ALBUM to Localization.get(language, "filter_albums"),
                YouTube.SearchFilter.FILTER_ARTIST to Localization.get(language, "filter_artists"),
                YouTube.SearchFilter.FILTER_FEATURED_PLAYLIST to Localization.get(language, "filter_playlists"),
            )
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(filters.size) { i ->
                    val (filter, label) = filters[i]
                    val selected = selectedFilter == filter
                    FilterChip(
                        selected = selected,
                        onClick = { setSelectedFilter(if (selected) null else filter) },
                        label = { Text(label) },
                    )
                }
            }
        }

        // Recent searches (saved locally, shown when the query is empty).
        if (query.isBlank() && searchHistory.isNotEmpty()) {
            Row(
                Modifier.fillMaxWidth().padding(top = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    Localization.get(language, "search_history"),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.weight(1f))
                TextButton(onClick = onClearSearchHistory) {
                    Text(Localization.get(language, "clear_search_history"))
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                searchHistory.take(12).forEach { term ->
                    FilterChip(
                        selected = false,
                        onClick = { updateQuery(term); onRecordSearch(term) },
                        label = { Text(term) },
                    )
                }
            }
        }

        // Suggestion list.
        if (focused && query.isNotBlank() && suggestions.isNotEmpty() && page == null && filterItems == null) {
            Surface(
                tonalElevation = 3.dp,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Column {
                    suggestions.forEach { s ->
                        Text(
                            s,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { updateQuery(s); focused = false; onRecordSearch(s) }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                        )
                    }
                }
            }
        }

        val result = page
        when {
            error != null -> ErrorBox(language, error)
            query.isBlank() -> Text(
                Localization.get(language, "search"),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 16.dp),
            )
            loading && result == null && filterItems == null -> LoadingBox(language)
            result != null -> LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                result.summaries.forEach { summary ->
                    item(key = "header-${summary.title}") {
                        Text(
                            summary.title,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.padding(top = 16.dp, bottom = 8.dp),
                        )
                    }
                    item(key = "body-${summary.title}") {
                        SummaryBody(summary, language, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong, onAddToQueue, onAddToPlaylist)
                    }
                }
                if (result.summaries.isEmpty()) {
                    item {
                        Text(
                            Localization.get(language, "no_results_found"),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = 16.dp),
                        )
                    }
                }
            }
            filterItems != null -> {
                val results = filterItems!!
                if (results.all { it is SongItem }) {
                    LazyColumn(Modifier.fillMaxSize().padding(top = 8.dp)) {
                        items(results.filterIsInstance<SongItem>(), key = { it.id }) { song ->
                            SongRow(song, language, { onPlaySong(song) }, onAddToQueue = { onAddToQueue(song) }, onAddToPlaylist = { onAddToPlaylist(song) })
                        }
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Adaptive(gridItemSize.dp),
                        modifier = Modifier.fillMaxSize().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        gridItems(results, key = { it.id }) { item ->
                            YtItemCard(
                                item = item,
                                width = null,
                                onClick = { onItemClick(item, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong) },
                            )
                        }
                    }
                }
                if (results.isEmpty()) {
                    Text(
                        Localization.get(language, "no_results_found"),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun SummaryBody(
    summary: SearchSummary,
    language: String,
    onOpenAlbum: (String) -> Unit,
    onOpenArtist: (String) -> Unit,
    onOpenPlaylist: (String) -> Unit,
    onPlaySong: (SongItem) -> Unit,
    onAddToQueue: (SongItem) -> Unit,
    onAddToPlaylist: (SongItem) -> Unit,
) {
    val songs = summary.items.filterIsInstance<SongItem>()
    val others = summary.items.filterNot { it is SongItem }

    Column {
        songs.forEach { song ->
            SongRow(song, language, { onPlaySong(song) }, onAddToQueue = { onAddToQueue(song) }, onAddToPlaylist = { onAddToPlaylist(song) })
        }
        if (others.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(others, key = { it.id }) { item ->
                    YtItemCard(item = item, onClick = { onItemClick(item, onOpenAlbum, onOpenArtist, onOpenPlaylist, onPlaySong) })
                }
            }
        }
    }
}
