package com.music.kugou.models

import kotlinx.serialization.Serializable

@Serializable
data class SearchSongResponse(
    val status: Int,
    val errcode: Int,
    val error: String,
    val data: Data,
) {
    @Serializable
    data class Data(
        val info: List<Info>,
    ) {
        @Serializable
        data class Info(
            val duration: Int,
            val hash: String,
            // KuGou answers a search with fuzzy results: for a title it does
            // not have it happily returns other songs by the same artist. The
            // names are parsed so a candidate can be checked against what was
            // asked for instead of trusting the first duration match.
            val songname: String = "",
            val singername: String = "",
        )
    }
}
