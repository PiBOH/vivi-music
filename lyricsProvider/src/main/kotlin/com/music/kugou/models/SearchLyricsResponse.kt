package com.music.kugou.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SearchLyricsResponse(
    val status: Int,
    val info: String,
    val errcode: Int,
    val errmsg: String,
    val expire: Int,
    val candidates: List<Candidate>,
) {
    @Serializable
    data class Candidate(
        val id: Long,
        @SerialName("product_from")
        val productFrom: String, // Consider choosing '官方推荐歌词'
        val duration: Long,
        val accesskey: String,
        // Names of the song this lyrics file belongs to (seconds-long
        // duration). KuGou sometimes swaps them (song holds the artist and
        // vice versa), which the matcher accounts for. Nullable so the model
        // keeps working if the endpoint ever omits them.
        val song: String? = null,
        val singer: String? = null,
    )
}
