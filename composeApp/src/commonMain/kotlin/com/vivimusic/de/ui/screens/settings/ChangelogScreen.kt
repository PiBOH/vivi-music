package com.vivimusic.de.ui.screens.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vivimusic.de.resources.Res
import com.vivimusic.de.resources.changelog
import com.vivimusic.de.resources.changelog_error
import com.vivimusic.de.ui.AppViewModel
import com.vivimusic.de.ui.ChipsRow
import org.jetbrains.compose.resources.stringResource

private data class ChangelogSection(val title: String, val items: List<String>)

private data class ChangelogEntry(
    val version: String,
    val date: String,
    val sections: List<ChangelogSection>,
)

/**
 * Parses the repository `CHANGELOG.md` (Keep a Changelog) into per-version
 * entries with their sections and items.
 */
private fun parseChangelog(markdown: String): List<ChangelogEntry> {
    val entries = mutableListOf<ChangelogEntry>()
    var version = ""
    var date = ""
    var sections = mutableListOf<ChangelogSection>()
    var sectionTitle = ""
    var items = mutableListOf<String>()

    fun flushSection() {
        if (sectionTitle.isNotBlank() || items.isNotEmpty()) {
            sections.add(ChangelogSection(sectionTitle, items.toList()))
        }
        sectionTitle = ""
        items = mutableListOf()
    }

    fun flushEntry() {
        flushSection()
        if (version.isNotBlank()) {
            entries.add(ChangelogEntry(version, date, sections.toList()))
        }
        sections = mutableListOf()
    }

    for (rawLine in markdown.lines()) {
        val line = rawLine.trim()
        when {
            line.startsWith("## [") -> {
                flushEntry()
                version = line.substringAfter("[").substringBefore("]").trim()
                date = line.substringAfter("] - ", "").trim()
            }
            line.startsWith("### ") -> {
                flushSection()
                sectionTitle = line.removePrefix("### ").trim()
            }
            line.startsWith("- ") -> items.add(line.removePrefix("- ").trim())
        }
    }
    flushEntry()
    return entries
}

/**
 * Changelog viewer, ported from ViVi Music's `ChangelogScreen` and adapted for
 * the desktop: fetches the raw `CHANGELOG.md` from the repository and shows a
 * version selector plus the Keep-a-Changelog sections for the selected version.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChangelogScreen(viewModel: AppViewModel, onBack: () -> Unit) {
    var entries by remember { mutableStateOf<List<ChangelogEntry>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var failed by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        loading = true
        failed = false
        val markdown = viewModel.fetchChangelogMarkdown()
        val parsed = parseChangelog(markdown)
        if (parsed.isEmpty()) {
            failed = true
        } else {
            entries = parsed
            selected = parsed.first().version
        }
        loading = false
    }

    val current = entries.firstOrNull { it.version == selected } ?: entries.firstOrNull()

    SettingsPage(title = stringResource(Res.string.changelog), onBack = onBack) {
        when {
            loading -> Box(
                modifier = Modifier.fillMaxWidth().padding(vertical = 48.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator()
            }

            failed || entries.isEmpty() -> Text(
                text = stringResource(Res.string.changelog_error),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp),
            )

            else -> {
                ChipsRow(
                    chips = entries.map { it.version to it.version },
                    currentValue = selected,
                    onValueUpdate = { selected = it },
                )

                current?.let { entry ->
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (entry.date.isNotBlank()) "${entry.version} - ${entry.date}" else entry.version,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                    )

                    entry.sections.forEach { section ->
                        if (section.title.isNotBlank()) {
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = section.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                            )
                        }
                        section.items.forEach { item ->
                            Spacer(modifier = Modifier.height(8.dp))
                            Row {
                                Box(
                                    modifier = Modifier
                                        .padding(top = 8.dp)
                                        .size(6.dp)
                                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                                )
                                Spacer(modifier = Modifier.size(12.dp))
                                Text(
                                    text = item,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurface,
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
