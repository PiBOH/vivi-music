package com.vivimusic.de.data.network

import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.serialization.kotlinx.json.json
import kotlinx.serialization.json.Json

/**
 * Shared JSON instance used by the InnerTube client and the sync layer.
 */
internal val sharedJson: Json = Json {
    ignoreUnknownKeys = true
    isLenient = true
    explicitNulls = false
    encodeDefaults = true
}

/**
 * Creates the HTTP client with the CIO engine (JVM desktop target).
 */
expect fun createHttpClient(): HttpClient

internal fun HttpClientConfig<*>.applyCommonConfig() {
    expectSuccess = false
    install(ContentNegotiation) {
        json(sharedJson)
    }
    install(HttpTimeout) {
        connectTimeoutMillis = 15_000
        requestTimeoutMillis = 30_000
        socketTimeoutMillis = 30_000
    }
}
