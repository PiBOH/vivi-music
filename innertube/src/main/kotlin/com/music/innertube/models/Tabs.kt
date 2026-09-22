package com.music.innertube.models

import kotlinx.serialization.Serializable

@Serializable
data class Tabs(
    val tabs: List<Tab>,
) {
    @Serializable
    data class Tab(
        val tabRenderer: TabRenderer,
    ) {
        @Serializable
        data class TabRenderer(
            val title: String?,
            val content: Content?,
            val endpoint: NavigationEndpoint?,
        ) {
            @Serializable
            data class Content(
                val sectionListRenderer: SectionListRenderer?,
                /**
                 * A library sub-page (the artists corpus, for one) puts its grid
                 * straight in the tab content instead of wrapping it in a
                 * `sectionListRenderer`, and the container was not modelled, so
                 * those pages deserialised to "no content" and parsed to an
                 * empty list.
                 */
                val gridRenderer: GridRenderer? = null,
                val musicQueueRenderer: MusicQueueRenderer?,
            )
        }
    }
}
