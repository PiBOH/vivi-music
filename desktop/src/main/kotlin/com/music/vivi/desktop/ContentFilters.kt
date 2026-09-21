package com.music.vivi.desktop

import com.music.innertube.models.YTItem
import com.music.innertube.models.filterExplicit
import com.music.innertube.models.filterVideoSongs
import com.music.innertube.models.filterYoutubeShorts
import com.music.innertube.pages.AlbumPage
import com.music.innertube.pages.ArtistItemsPage
import com.music.innertube.pages.ArtistPage
import com.music.innertube.pages.BrowseResult
import com.music.innertube.pages.HomePage
import com.music.innertube.pages.PlaylistPage
import com.music.innertube.pages.SearchSummaryPage

/**
 * The content filters of the Content screen (port of the mobile one).
 *
 * The values are cached in memory, like [Animations], because the screens need
 * them while rendering a list: reading the settings file per item would be a
 * file read per row. [load] is called at startup and whenever the screen
 * changes one, and every fetch applies them where the items enter the UI.
 */
object ContentFilters {

    @Volatile
    var hideExplicit: Boolean = false

    @Volatile
    var hideVideoSongs: Boolean = false

    @Volatile
    var hideYoutubeShorts: Boolean = false

    /** Reloads the persisted filters (startup + on change). */
    fun load() {
        val state = DesktopSettings.load()
        hideExplicit = state.hideExplicit
        hideVideoSongs = state.hideVideoSongs
        hideYoutubeShorts = state.hideYoutubeShorts
    }
}

/** Applies every content filter of the Content screen to a list of items. */
fun <T : YTItem> List<T>.filteredContent(): List<T> =
    filterExplicit(ContentFilters.hideExplicit)
        .filterVideoSongs(ContentFilters.hideVideoSongs)
        .filterYoutubeShorts(ContentFilters.hideYoutubeShorts)

fun BrowseResult.filteredContent(): BrowseResult =
    filterExplicit(ContentFilters.hideExplicit)
        .filterVideoSongs(ContentFilters.hideVideoSongs)
        .filterYoutubeShorts(ContentFilters.hideYoutubeShorts)

fun HomePage.filteredContent(): HomePage =
    filterExplicit(ContentFilters.hideExplicit)
        .filterVideoSongs(ContentFilters.hideVideoSongs)

fun SearchSummaryPage.filteredContent(): SearchSummaryPage =
    filterExplicit(ContentFilters.hideExplicit)
        .filterVideoSongs(ContentFilters.hideVideoSongs)
        .filterYoutubeShorts(ContentFilters.hideYoutubeShorts)

fun AlbumPage.filteredContent(): AlbumPage = copy(
    songs = songs.filteredContent(),
    otherVersions = otherVersions.filteredContent(),
    releasesForYou = releasesForYou.filteredContent(),
)

fun PlaylistPage.filteredContent(): PlaylistPage = copy(
    songs = songs.filteredContent(),
    related = related?.filteredContent(),
)

fun ArtistPage.filteredContent(): ArtistPage = copy(
    sections = sections.map { it.copy(items = it.items.filteredContent()) },
)

fun ArtistItemsPage.filteredContent(): ArtistItemsPage = copy(items = items.filteredContent())
