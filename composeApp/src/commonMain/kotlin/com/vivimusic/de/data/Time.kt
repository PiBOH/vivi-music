package com.vivimusic.de.data

import kotlin.time.Clock

/** Current epoch time in milliseconds, available on all targets. */
fun nowEpochMillis(): Long = Clock.System.now().toEpochMilliseconds()
