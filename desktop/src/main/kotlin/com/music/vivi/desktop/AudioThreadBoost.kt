package com.music.vivi.desktop

import com.sun.jna.Native
import com.sun.jna.Pointer
import com.sun.jna.Structure
import com.sun.jna.WString
import com.sun.jna.ptr.IntByReference
import com.sun.jna.win32.StdCallLibrary
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Windows scheduling protection for the threads that feed the sound card
 * (issue #3).
 *
 * Every measured audio dropout on Windows so far had the same shape: the
 * writer thread and the watchdog were both held up for hundreds of
 * milliseconds (once **1161 ms**) while the process used 4-6 % CPU and every
 * GC counter stayed frozen at a 38 MB heap. That is not the JVM: it is the OS
 * not scheduling a *normal* process while the machine is busy (the export that
 * contained it also reported the system at 57-85 % busy, i.e. another workload
 * — a browser decoding video, for instance — was running next to VIVI).
 *
 * A browser never has this problem because its audio thread does not rely on
 * being a merely *high priority* thread: it registers itself with the
 * Multimedia Class Scheduler Service (MMCSS) as an `"Audio"` task, which is the
 * documented Windows contract for "give this thread a guaranteed share of the
 * CPU, ahead of everything that is not latency sensitive". Java offers no API
 * for it, so it is called through JNA here:
 *
 *  - [boost] — called *on* the audio threads (MMCSS is per-thread):
 *    `AvSetMmThreadCharacteristics("Audio")` + `AvSetMmThreadPriority`, the
 *    same pair Chrome/Firefox call on their audio render thread.
 *  - [start] — called once at startup for the process: `timeBeginPeriod(1)`
 *    (1 ms timer, so the 25/50 ms waits of the pipeline return on time),
 *    `ABOVE_NORMAL_PRIORITY_CLASS` and **power throttling (EcoQoS) off**, so
 *    Windows 11 cannot demote the app — and with it the audio thread — to a
 *    "background" scheduling class while the user works in another window.
 *
 * Everything is best-effort and Windows-only: on any other OS, or when a call
 * fails, playback keeps working exactly as before and the outcome is written
 * to the session's `playback.log` (the log has to say whether the protection
 * is in effect, otherwise the next "still lags" report cannot be read).
 */
object AudioThreadBoost {

    private val os = System.getProperty("os.name", "").lowercase(Locale.ROOT)
    private val isWindows = os.contains("win")

    private interface Avrt : StdCallLibrary {
        fun AvSetMmThreadCharacteristicsW(taskName: WString, taskIndex: IntByReference): Pointer?
        fun AvSetMmThreadPriority(handle: Pointer, priority: Int): Boolean
        fun AvRevertMmThreadCharacteristics(handle: Pointer): Boolean
    }

    private interface Winmm : StdCallLibrary {
        fun timeBeginPeriod(period: Int): Int
    }

    private interface Kernel32 : StdCallLibrary {
        fun GetCurrentProcess(): Pointer
        fun SetPriorityClass(process: Pointer, priorityClass: Int): Boolean
        fun SetProcessInformation(process: Pointer, informationClass: Int, info: Structure, size: Int): Boolean
    }

    /**
     * `PROCESS_POWER_THROTTLING_STATE` (winnt.h) passed to
     * `SetProcessInformation(ProcessPowerThrottling)`.
     */
    class PowerThrottlingState : Structure() {
        @JvmField var version: Int = PROCESS_POWER_THROTTLING_CURRENT_VERSION
        @JvmField var controlMask: Int = 0
        @JvmField var stateMask: Int = 0

        override fun getFieldOrder(): List<String> = listOf("version", "controlMask", "stateMask")
    }

    // avrt.h: AVRT_PRIORITY_LOW = -1, NORMAL = 0, HIGH = 1, CRITICAL = 2.
    private const val AVRT_PRIORITY_HIGH = 1
    private const val AVRT_PRIORITY_CRITICAL = 2

    private const val ABOVE_NORMAL_PRIORITY_CLASS = 0x00008000
    private const val PROCESS_POWER_THROTTLING = 4
    private const val PROCESS_POWER_THROTTLING_CURRENT_VERSION = 1
    private const val PROCESS_POWER_THROTTLING_EXECUTION_SPEED = 0x1

    private val avrt: Avrt? =
        if (isWindows) runCatching { Native.load("avrt", Avrt::class.java) }.getOrNull() else null
    private val winmm: Winmm? =
        if (isWindows) runCatching { Native.load("winmm", Winmm::class.java) }.getOrNull() else null
    private val kernel32: Kernel32? =
        if (isWindows) runCatching { Native.load("kernel32", Kernel32::class.java) }.getOrNull() else null

    /** MMCSS handle of the calling thread, so a per-track thread can release it. */
    private val handle = ThreadLocal<Pointer?>()

    private val started = AtomicBoolean(false)
    private val loggedThreads = java.util.concurrent.ConcurrentHashMap.newKeySet<String>()

    /** One-time, process-wide setup. Safe to call more than once. */
    fun start() {
        if (!isWindows) return
        if (!started.compareAndSet(false, true)) return
        val notes = mutableListOf<String>()
        val timer = winmm?.let { runCatching { it.timeBeginPeriod(1) }.getOrDefault(-1) } ?: -1
        notes += "timer resolution ${if (timer == 0) "1ms" else "unchanged"}"
        val k = kernel32
        val priority = k?.let { lib ->
            runCatching { lib.SetPriorityClass(lib.GetCurrentProcess(), ABOVE_NORMAL_PRIORITY_CLASS) }
                .getOrDefault(false)
        } ?: false
        notes += "process priority ${if (priority) "above normal" else "unchanged"}"
        val throttling = k?.let { lib ->
            runCatching {
                val state = PowerThrottlingState().apply {
                    controlMask = PROCESS_POWER_THROTTLING_EXECUTION_SPEED
                    stateMask = 0
                }
                lib.SetProcessInformation(
                    lib.GetCurrentProcess(),
                    PROCESS_POWER_THROTTLING,
                    state,
                    state.size(),
                )
            }.getOrDefault(false)
        } ?: false
        notes += "power throttling (EcoQoS) ${if (throttling) "disabled" else "not supported"}"
        AppLog.log("playback", "audio scheduling: ${notes.joinToString(", ")}")
    }

    /**
     * Registers the *calling* thread with MMCSS as an `"Audio"` task. [role] is
     * only used for the log line; [critical] picks `AVRT_PRIORITY_CRITICAL`
     * (the thread that actually feeds the sound card) over `HIGH`.
     */
    fun boost(role: String, critical: Boolean = false) {
        if (!isWindows) return
        val lib = avrt ?: return
        // A thread can be reused for a later track: drop the previous
        // association first, so the refcount on the task does not grow.
        release()
        runCatching {
            val mmcss = lib.AvSetMmThreadCharacteristicsW(WString(MMCSS_TASK), IntByReference(0))
            if (mmcss == null) {
                if (loggedThreads.add("$role:failed")) {
                    AppLog.log(
                        "playback",
                        "audio scheduling: thread '$role' could not join the Windows 'Audio' class " +
                            "(MMCSS refused) — it keeps Java priority ${Thread.currentThread().priority}",
                    )
                }
                return
            }
            handle.set(mmcss)
            val ok = lib.AvSetMmThreadPriority(mmcss, if (critical) AVRT_PRIORITY_CRITICAL else AVRT_PRIORITY_HIGH)
            if (loggedThreads.add("$role:ok")) {
                AppLog.log(
                    "playback",
                    "audio scheduling: thread '$role' joined the Windows 'Audio' class " +
                        "(MMCSS priority ${if (critical) "critical" else "high"}${if (ok) "" else " refused"})",
                )
            }
        }
    }

    /** Leaves the MMCSS task for the calling thread (call when it ends). */
    fun release() {
        val current = handle.get() ?: return
        handle.set(null)
        val lib = avrt ?: return
        runCatching { lib.AvRevertMmThreadCharacteristics(current) }
    }

    private const val MMCSS_TASK = "Audio"
}
