package com.music.vivi.desktop

import kotlin.math.roundToInt

/**
 * Global UI animation-speed control ("Animation speed": Fast / Normal / Slow).
 *
 * The value is cached in memory ([speed]) so scaling an animation duration never
 * touches the settings file: it is loaded once at startup and refreshed by the
 * settings screen when the user changes it. [ms] scales a base duration, [f] a
 * tween/spring fraction, so any transition can opt in.
 */
object Animations {

    /** Current speed name: "fast" / "normal" / "slow". */
    @Volatile
    var speed: String = "normal"

    /** Reloads the persisted speed (startup + on change). */
    fun load() {
        speed = DesktopSettings.load().animationSpeed
    }

    /** Multiplier applied to every base duration. */
    private val multiplier: Float
        get() = when (speed) {
            "fast" -> 0.6f
            "slow" -> 1.6f
            else -> 1f
        }

    /** Scales a base duration in milliseconds (never below 1 ms). */
    fun ms(base: Int): Int = (base * multiplier).roundToInt().coerceAtLeast(1)

    /** Scales a base duration in milliseconds as a Long. */
    fun ms(base: Long): Long = ms(base.toInt()).toLong()
}
