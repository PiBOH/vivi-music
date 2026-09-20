package com.vivimusic.de.data.network

import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO

actual fun createHttpClient(): HttpClient = HttpClient(CIO) {
    applyCommonConfig()
}
