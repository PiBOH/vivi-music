package com.vivimusic.de.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.NorthWest
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.vivimusic.de.domain.Song
import com.vivimusic.de.resources.*
import com.vivimusic.de.ui.AppViewModel
import com.vivimusic.de.ui.EmptyState
import com.vivimusic.de.ui.SongRow
import com.vivimusic.de.ui.theme.groupedItemShape
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource

private val SuggestionItemHeight = 56.dp

@Composable
fun SearchScreen(viewModel: AppViewModel) {
    val query by viewModel.searchQuery.collectAsState()
    val results by viewModel.searchResults.collectAsState()
    val suggestions by viewModel.searchSuggestions.collectAsState()
    val history by viewModel.searchHistory.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = query,
            onValueChange = viewModel::onQueryChange,
            placeholder = { Text(stringResource(Res.string.search_placeholder)) },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { viewModel.onQueryChange("") }) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                    }
                }
            },
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { viewModel.submitSearch(query) }),
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            singleLine = true,
        )

        if (query.isBlank()) {
            HistorySection(history, viewModel)
        } else {
            ResultsSection(suggestions, results, viewModel)
        }
    }
}

@Composable
private fun HistorySection(history: List<String>, viewModel: AppViewModel) {
    if (history.isEmpty()) {
        EmptyState(Res.string.empty_history)
        return
    }
    LazyColumn(modifier = Modifier.fillMaxSize()) {
        item(key = "history_header") {
            SearchSectionHeader(Res.string.search_history)
        }
        itemsIndexed(history, key = { _, it -> "history_$it" }) { index, query ->
            SuggestionItem(
                query = query,
                online = false,
                shape = groupedItemShape(index, history.size),
                onClick = { viewModel.submitSearch(query) },
                onDelete = { viewModel.deleteSearchHistory(query) },
                onFillTextField = { viewModel.onQueryChange(query) },
            )
        }
    }
}

@Composable
private fun ResultsSection(
    suggestions: List<String>,
    results: List<Song>,
    viewModel: AppViewModel,
) {
    if (suggestions.isEmpty() && results.isEmpty()) {
        EmptyState(Res.string.empty_search)
        return
    }

    LazyColumn(modifier = Modifier.fillMaxSize()) {
        if (suggestions.isNotEmpty()) {
            item(key = "suggestions_header") {
                SearchSectionHeader(Res.string.suggestions)
            }
            itemsIndexed(suggestions, key = { _, it -> "suggestion_$it" }) { index, suggestion ->
                SuggestionItem(
                    query = suggestion,
                    online = true,
                    shape = groupedItemShape(index, suggestions.size),
                    onClick = { viewModel.submitSearch(suggestion) },
                    onFillTextField = { viewModel.onQueryChange(suggestion) },
                )
            }
        }

        if (results.isNotEmpty()) {
            item(key = "results_header") {
                SearchSectionHeader(Res.string.top_result)
            }
            itemsIndexed(results, key = { _, song -> song.id }) { index, song ->
                SongRow(
                    song = song,
                    viewModel = viewModel,
                    shape = groupedItemShape(index, results.size),
                )
            }
        }
    }
}

@Composable
private fun SearchSectionHeader(text: StringResource) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp),
    )
}

/**
 * A single suggestion/history row, ported from ViVi Music's `SuggestionItem`:
 * leading icon (search for online suggestions, history for past searches), the
 * query text, an optional delete button and a "fill text field" button.
 */
@Composable
private fun SuggestionItem(
    query: String,
    online: Boolean,
    shape: Shape,
    onClick: () -> Unit,
    onDelete: () -> Unit = {},
    onFillTextField: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(horizontal = 16.dp, vertical = 1.dp)
            .fillMaxWidth()
            .height(SuggestionItemHeight)
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick),
    ) {
        Icon(
            imageVector = if (online) Icons.Filled.Search else Icons.Filled.History,
            contentDescription = null,
            modifier = Modifier.padding(horizontal = 16.dp).alpha(0.5f),
        )
        Text(
            text = query,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f),
        )
        if (!online) {
            IconButton(onClick = onDelete, modifier = Modifier.alpha(0.5f)) {
                Icon(Icons.Filled.Close, contentDescription = null)
            }
        }
        IconButton(onClick = onFillTextField, modifier = Modifier.alpha(0.5f)) {
            Icon(Icons.Filled.NorthWest, contentDescription = null)
        }
    }
}
