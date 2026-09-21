package com.music.vivi.desktop

import com.sun.management.GarbageCollectionNotificationInfo
import java.lang.management.ManagementFactory
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import javax.management.Notification
import javax.management.NotificationEmitter
import javax.management.NotificationListener
import javax.management.openmbean.CompositeData

/**
 * GC telemetry for playback diagnostics (issue #3).
 *
 * A stop-the-world collection freezes *every* thread — the writer that feeds
 * the sound card included — so a long pause is audible as a gap and as an UI
 * hitch at the same instant. Until now that had to be *inferred* from the GC
 * bean counters printed next to a stall (`gc G1 Young Generation:31/1633ms`),
 * which say how much collection time accumulated since the JVM started but not
 * **when** and not **which pause** the thread hit. One exported log even
 * showed a 1161 ms hold-up next to counters that could not account for it,
 * which left "the JVM froze" and "the OS did not schedule us" indistinguishable.
 *
 * This listener is the missing half: it is fed by the JVM's own GC
 * notifications (no `-Xlog` flag, which a packaged `jpackage` image cannot
 * point at the session folder) and writes every collection to the session's
 * `gc.log` — plus a line in `playback.log` for the pauses long enough to reach
 * the device ring (>100 ms). The watchdog then carries `last GC pause Nms (Ns
 * ago, worst Nms)` into its stall line, so the two causes separate themselves:
 *
 *  - a stall line next to a GC pause of the same size → the JVM stopped;
 *  - a stall line with a small/long-ago GC pause → the process was not
 *    scheduled (see [AudioThreadBoost], which protects against exactly that).
 */
object GcMonitor {

    /** Collections shorter than this are counted but not logged line by line. */
    private const val LOG_THRESHOLD_MS = 5L

    /** A pause this long can reach the sound card: it gets a `playback` line. */
    private const val LOUD_THRESHOLD_MS = 100L

    private val started = AtomicBoolean(false)

    @Volatile private var lastPauseMs = 0L
    @Volatile private var lastPauseAtNanos = 0L
    @Volatile private var lastPauseName = ""

    private val pauseCount = AtomicLong()
    private val totalPauseMs = AtomicLong()
    private val worstPauseMs = AtomicLong()

    fun start() {
        if (!started.compareAndSet(false, true)) return
        val listener = NotificationListener { notification, _ -> handle(notification) }
        var attached = 0
        runCatching {
            for (bean in ManagementFactory.getGarbageCollectorMXBeans()) {
                val emitter = bean as? NotificationEmitter ?: continue
                if (runCatching { emitter.addNotificationListener(listener, null, null) }.isSuccess) {
                    attached++
                }
            }
        }
        AppLog.log(
            "gc",
            "gc telemetry: listening on $attached collector(s) — " +
                "every collection >${LOG_THRESHOLD_MS}ms is listed here, " +
                "a pause >${LOUD_THRESHOLD_MS}ms is flagged in playback.log",
        )
    }

    /**
     * One line for the stall watchdog: the last pause, how long ago it was and
     * the worst pause seen so far. Deliberately cheap (no allocation of note).
     */
    fun context(): String {
        val at = lastPauseAtNanos
        if (at == 0L) return "no GC pause measured yet (worst 0ms, total 0ms)"
        val ageMs = (System.nanoTime() - at) / 1_000_000L
        val age = if (ageMs < 1_000L) "${ageMs}ms" else "${ageMs / 1_000L}s"
        return "last GC pause ${lastPauseMs}ms $age ago (${lastPauseName}), " +
            "worst ${worstPauseMs.get()}ms, total ${totalPauseMs.get()}ms in ${pauseCount.get()} pauses"
    }

    private fun handle(notification: Notification) {
        if (notification.type != GarbageCollectionNotificationInfo.GARBAGE_COLLECTION_NOTIFICATION) return
        val data = notification.userData as? CompositeData ?: return
        val info = runCatching { GarbageCollectionNotificationInfo.from(data) }.getOrNull() ?: return
        val durationMs = info.gcInfo.duration
        val heap = runCatching { ManagementFactory.getMemoryMXBean().heapMemoryUsage.used / 1024 / 1024 }
            .getOrDefault(0L)
        lastPauseMs = durationMs
        lastPauseAtNanos = System.nanoTime()
        lastPauseName = "${info.gcName}, ${info.gcCause}"
        pauseCount.incrementAndGet()
        totalPauseMs.addAndGet(durationMs)
        worstPauseMs.updateAndGet { maxOf(it, durationMs) }
        if (durationMs >= LOUD_THRESHOLD_MS) {
            AppLog.log(
                "playback",
                "gc pause: ${durationMs}ms (${lastPauseName}) — every thread was stopped here " +
                    "(heap ${heap}MB in use)",
            )
        }
        if (durationMs >= LOG_THRESHOLD_MS) {
            AppLog.log("gc", "${durationMs}ms — ${lastPauseName} (heap ${heap}MB)")
        }
    }
}
