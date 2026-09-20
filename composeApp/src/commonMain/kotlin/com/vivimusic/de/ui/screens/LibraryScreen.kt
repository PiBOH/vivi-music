package com.vivimusic.de.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.vivimusic.de.domain.Album
import com.vivimusic.de.domain.Artist
import com.vivimusic.de.domain.Playlist
import com.vivimusic.de.domain.Song
import com.vivimusic.de.resources.*
import com.vivimusic.de.ui.AppViewModel
import com.vivimusic.de.ui.ChipsRow
import com.vivimusic.de.ui.EmptyState
import com.vivimusic.de.ui.SongRow
import com.vivimusic.de.ui.theme.groupedItemShape
import com.vivimusic.de.ui.theme.listItemColors
import org.jetbrains.compose.resources.stringResource

private val ThumbnailCornerRadius = 6.dp

/** The four Library views, mirroring ViVi Music's `LibraryFilter`. */
private enum class LibraryFilter { PLAYLISTS, SONGS, ALBUMS, ARTISTS }

/** A single row in the combined "mix" view, which is a flat list like upstream. */
private sealed interface LibraryRow {
    val id: String

    data class PlaylistEntry(val value: Playlist) : LibraryRow {
        override val id: String get() = "pl-${value.id}"
    }

    data class AlbumEntry(val value: Album) : LibraryRow {
        override val id: String get() = "al-${value.id}"
    }

    data class ArtistEntry(val value: Artist) : LibraryRow {
        override val id: String get() = "ar-${value.id}"
    }

    data class SongEntry(val value: Song) : LibraryRow {
        override val id: String get() = "sg-${value.id}"
    }
}

@Composable
fun LibraryScreen(
    viewModel: AppViewModel,
    onOpenAlbum: (Album) -> Unit,
    onOpenArtist: (Artist) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
) {
    val favorites by viewModel.favorites.collectAsState()
    val playlists by viewModel.playlists.collectAsState()
    var filter by remember { mutableStateOf<LibraryFilter?>(null) }

    val albums = remember(favorites) { deriveAlbums(favorites) }
    val artists = remember(favorites) { deriveArtists(favorites) }

    Column(modifier = Modifier.fillMaxSize()) {
        ChipsRow(
            chips = listOf(
                LibraryFilter.PLAYLISTS to stringResource(Res.string.filter_playlists),
                LibraryFilter.SONGS to stringResource(Res.string.filter_songs),
                LibraryFilter.ALBUMS to stringResource(Res.string.filter_albums),
                LibraryFilter.ARTISTS to stringResource(Res.string.filter_artists),
            ),
            currentValue = filter,
            onValueUpdate = { selected ->
                // Tapping the active chip deselects it and returns to the mix view.
                filter = if (filter == selected) null else selected
            },
        )

        when (filter) {
            null -> LibraryMix(playlists, albums, artists, favorites, viewModel, onOpenAlbum, onOpenArtist, onOpenPlaylist)
            LibraryFilter.PLAYLISTS -> PlaylistsView(playlists, viewModel, onOpenPlaylist)
            LibraryFilter.SONGS -> SongList(favorites, viewModel)
            LibraryFilter.ALBUMS -> AlbumsView(albums, onOpenAlbum)
            LibraryFilter.ARTISTS -> ArtistsView(artists, onOpenArtist)
        }
    }
}

// ----- data derivation (albums/artists are not stored separately yet) -----

private fun deriveAlbums(songs: List<Song>): List<Album> =
    songs.filter { it.album.isNotBlank() }
        .groupBy { it.album }
        .map { (name, group) ->
            Album(
                id = "album-$name",
                title = name,
                artist = group.first().artist,
                thumbnailUrl = group.first().thumbnailUrl,
                songs = group,
            )
        }

private fun deriveArtists(songs: List<Song>): List<Artist> =
    songs.filter { it.artist.isNotBlank() }
        .groupBy { it.artist }
        .map { (name, group) ->
            Artist(
                id = "artist-$name",
                name = name,
                thumbnailUrl = group.first().thumbnailUrl,
                songs = group,
            )
        }

// ----- mixed view (default) -----

@Composable
private fun LibraryMix(
    playlists: List<Playlist>,
    albums: List<Album>,
    artists: List<Artist>,
    songs: List<Song>,
    viewModel: AppViewModel,
    onOpenAlbum: (Album) -> Unit,
    onOpenArtist: (Artist) -> Unit,
    onOpenPlaylist: (Playlist) -> Unit,
) {
    val rows = remember(playlists, albums, artists, songs) {
        buildList {
            playlists.forEach { add(LibraryRow.PlaylistEntry(it)) }
            albums.forEach { add(LibraryRow.AlbumEntry(it)) }
            artists.forEach { add(LibraryRow.ArtistEntry(it)) }
            songs.forEach { add(LibraryRow.SongEntry(it)) }
        }
    }

    if (rows.isEmpty()) {
        EmptyState(Res.string.empty_library)
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(rows, key = { _, row -> row.id }) { index, row ->
            when (row) {
                is LibraryRow.PlaylistEntry -> PlaylistRow(
                    playlist = row.value,
                    viewModel = viewModel,
                    shape = groupedItemShape(index, rows.size),
                    onClick = { onOpenPlaylist(row.value) },
                )

                is LibraryRow.AlbumEntry -> AlbumRow(
                    album = row.value,
                    shape = groupedItemShape(index, rows.size),
                    onClick = { onOpenAlbum(row.value) },
                )

                is LibraryRow.ArtistEntry -> ArtistRow(
                    artist = row.value,
                    shape = groupedItemShape(index, rows.size),
                    onClick = { onOpenArtist(row.value) },
                )

                is LibraryRow.SongEntry -> SongRow(
                    song = row.value,
                    viewModel = viewModel,
                    shape = groupedItemShape(index, rows.size),
                )
            }
        }
    }
}

// ----- playlists view -----

@Composable
private fun PlaylistsView(
    playlists: List<Playlist>,
    viewModel: AppViewModel,
    onOpenPlaylist: (Playlist) -> Unit,
) {
    var name by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                placeholder = { Text(stringResource(Res.string.new_playlist)) },
                modifier = Modifier.weight(1f),
                singleLine = true,
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = {
                    viewModel.createPlaylist(name.trim())
                    name = ""
                },
            ) {
                Text(stringResource(Res.string.new_playlist))
            }
        }

        if (playlists.isEmpty()) {
            EmptyState(Res.string.empty_playlists)
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                itemsIndexed(playlists, key = { _, it -> it.id }) { index, playlist ->
                    PlaylistRow(
                        playlist = playlist,
                        viewModel = viewModel,
                        shape = groupedItemShape(index, playlists.size),
                        onClick = { onOpenPlaylist(playlist) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PlaylistRow(
    playlist: Playlist,
    viewModel: AppViewModel,
    shape: androidx.compose.ui.graphics.Shape,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(playlist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = { Thumbnail(playlist.thumbnailUrl) },
        trailingContent = {
            TextButton(onClick = { viewModel.deletePlaylist(playlist.id) }) {
                Text(stringResource(Res.string.delete))
            }
        },
        colors = listItemColors(),
        modifier = Modifier.clip(shape).clickable(onClick = onClick),
    )
}

// ----- songs view -----

@Composable
private fun SongList(songs: List<Song>, viewModel: AppViewModel) {
    if (songs.isEmpty()) {
        EmptyState(Res.string.empty_favorites)
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(songs, key = { _, song -> song.id }) { index, song ->
            SongRow(song, viewModel, shape = groupedItemShape(index, songs.size), queue = songs)
        }
    }
}

// ----- albums view -----

@Composable
private fun AlbumsView(albums: List<Album>, onOpenAlbum: (Album) -> Unit) {
    if (albums.isEmpty()) {
        EmptyState(Res.string.empty_albums)
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(albums, key = { _, album -> album.id }) { index, album ->
            AlbumRow(album, shape = groupedItemShape(index, albums.size), onClick = { onOpenAlbum(album) })
        }
    }
}

@Composable
private fun AlbumRow(album: Album, shape: androidx.compose.ui.graphics.Shape, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(album.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        supportingContent = {
            Text(
                album.artist.ifBlank { stringResource(Res.string.unknown_artist) },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        },
        leadingContent = { Thumbnail(album.thumbnailUrl) },
        colors = listItemColors(),
        modifier = Modifier.clip(shape).clickable(onClick = onClick),
    )
}

// ----- artists view -----

@Composable
private fun ArtistsView(artists: List<Artist>, onOpenArtist: (Artist) -> Unit) {
    if (artists.isEmpty()) {
        EmptyState(Res.string.empty_artists)
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        itemsIndexed(artists, key = { _, artist -> artist.id }) { index, artist ->
            ArtistRow(artist, shape = groupedItemShape(index, artists.size), onClick = { onOpenArtist(artist) })
        }
    }
}

@Composable
private fun ArtistRow(artist: Artist, shape: androidx.compose.ui.graphics.Shape, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(artist.name, maxLines = 1, overflow = TextOverflow.Ellipsis) },
        leadingContent = { Thumbnail(artist.thumbnailUrl, circular = true) },
        colors = listItemColors(),
        modifier = Modifier.clip(shape).clickable(onClick = onClick),
    )
}

// ----- shared thumbnail -----

@Composable
private fun Thumbnail(url: String?, size: Dp = 48.dp, circular: Boolean = false) {
    val shape = if (circular) CircleShape else RoundedCornerShape(ThumbnailCornerRadius)
    Box(
        modifier = Modifier
            .size(size)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        if (!url.isNullOrBlank()) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
