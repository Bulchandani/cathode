package io.github.bulchandani.cathode.log

import android.util.Log
import androidx.compose.runtime.mutableStateOf
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentLinkedDeque

/**
 * Process-wide log ring buffer. Backs the in-app Log Viewer at
 * Settings → ABOUT → 5-tap CRT logo.
 *
 * Also pipes to android.util.Log so `adb logcat -s Cathode` works.
 *
 * Keep it tiny — every read happens on a single thread and the buffer
 * size is bounded so we don't OOM in long sessions.
 */
object Logger {

    enum class Level { D, I, W, E }

    data class Entry(
        val at: Long,
        val level: Level,
        val tag: String,
        val message: String,
    ) {
        fun render(): String {
            val ts = TIME_FMT.format(Date(at))
            return "$ts  ${level.name}  $tag  $message"
        }
    }

    private const val MAX_ENTRIES = 2_000
    private val TIME_FMT = SimpleDateFormat("HH:mm:ss.SSS", Locale.US)

    private val buffer = ConcurrentLinkedDeque<Entry>()

    /** Bumped on every append so Compose readers can recompose. */
    val rev = mutableStateOf(0L)

    fun d(tag: String, message: String) = log(Level.D, tag, message)
    fun i(tag: String, message: String) = log(Level.I, tag, message)
    fun w(tag: String, message: String) = log(Level.W, tag, message)
    fun e(tag: String, message: String, t: Throwable? = null) {
        val msg = if (t == null) message else "$message :: ${t::class.simpleName}: ${t.message}"
        log(Level.E, tag, msg)
    }

    private fun log(level: Level, tag: String, message: String) {
        val entry = Entry(System.currentTimeMillis(), level, tag, message)
        buffer.addLast(entry)
        while (buffer.size > MAX_ENTRIES) buffer.pollFirst()
        rev.value = rev.value + 1
        when (level) {
            Level.D -> Log.d("Cathode:$tag", message)
            Level.I -> Log.i("Cathode:$tag", message)
            Level.W -> Log.w("Cathode:$tag", message)
            Level.E -> Log.e("Cathode:$tag", message)
        }
    }

    fun snapshot(): List<Entry> = buffer.toList()

    fun clear() {
        buffer.clear()
        rev.value = rev.value + 1
    }

    fun renderAll(): String = buffer.joinToString("\n") { it.render() }
}
