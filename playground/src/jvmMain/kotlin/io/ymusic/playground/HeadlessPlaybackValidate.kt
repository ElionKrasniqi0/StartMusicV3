package io.ymusic.playground

import io.ymusic.mse.MseVideoSurface
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import projectcommon.json.longOrNull
import projectcommon.json.objectOrNull
import projectcommon.json.stringOrNull
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.system.exitProcess

/**
 * Throwaway headless MSE playback probe (not product UI).
 * Run: `./gradlew :playground:run -PvalidatePlayback=true`
 * Env: MSE_VIDEO_IDS=id1,id2  MSE_MIN_MS=180000  MSE_TIMEOUT_MS=240000
 */
object HeadlessPlaybackValidate {
    @JvmStatic
    fun main(args: Array<String>) {
        runBlocking {
            val ids =
                (System.getenv("MSE_VIDEO_IDS") ?: System.getenv("MSE_VIDEO_ID") ?: "0_LTL7O1xX0,jfKfPfyJRdk")
                    .split(',')
                    .map { it.trim() }
                    .filter { it.isNotEmpty() }
            val minMs = (System.getenv("MSE_MIN_MS") ?: "180000").toLong()
            val timeoutMs = (System.getenv("MSE_TIMEOUT_MS") ?: (minMs + 90_000L).toString()).toLong()
            val report =
                File(
                    System.getenv("MSE_REPORT")
                        ?: File(System.getProperty("java.io.tmpdir"), "smv3-mse-validate.txt").absolutePath,
                )
            val out = StringBuilder()
            fun line(s: String) {
                println(s)
                System.out.flush()
                out.appendLine(s)
                report.parentFile?.mkdirs()
                report.writeText(out.toString())
            }
            line("=== StartMusicV3 headless MSE validate ===")
            line("minMs=$minMs timeoutMs=$timeoutMs ids=$ids")
            var failed = false
            for (videoId in ids) {
                line("--- videoId=$videoId ---")
                val result = runCatching { validateOne(videoId, minMs, timeoutMs, ::line) }
                if (result.isFailure) {
                    failed = true
                    line("FAIL videoId=$videoId ${result.exceptionOrNull()}")
                    result.exceptionOrNull()?.printStackTrace()
                } else {
                    line("PASS videoId=$videoId ${result.getOrNull()}")
                }
            }
            line("report=${report.absolutePath}")
            exitProcess(if (failed) 1 else 0)
        }
    }
}

private suspend fun validateOne(
    videoId: String,
    minMs: Long,
    timeoutMs: Long,
    line: (String) -> Unit,
): String = coroutineScope {
    val surface = MseVideoSurface()
    val session = withContext(Dispatchers.Default) { HeadlessSession.boot(surface) }
    try {
        withContext(Dispatchers.Default) {
            session.player.init(session.log)
            session.player.setViewport(640, 360, session.log)
        }
        val positionMs = AtomicLong(0L)
        val durationMs = AtomicLong(0L)
        val state = AtomicReference("unknown")
        val error = AtomicReference<String?>(null)
        val statusJob = async(Dispatchers.Default) {
            try {
                session.player.collectStatus({ st ->
                    st.objectOrNull("progress")?.let { p ->
                        positionMs.set(p.longOrNull("positionMs") ?: 0L)
                        durationMs.set(p.longOrNull("durationMs") ?: 0L)
                    }
                    st.objectOrNull("state")?.let { s ->
                        s.stringOrNull("newState")?.let { state.set(it) }
                    }
                    st.objectOrNull("error")?.let { e ->
                        error.set(e.stringOrNull("code") ?: e.toString())
                    }
                }, session.log)
            } catch (t: Throwable) {
                error.compareAndSet(null, t.message ?: t.toString())
            }
        }
        withContext(Dispatchers.Default) {
            session.player.load(
                videoId,
                session.log,
                quality = "auto",
                width = 640,
                height = 360,
                audioOnly = true,
                playWhenReady = true,
            )
            session.player.play(session.log)
        }
        line("loaded videoId=$videoId")
        val crossed60 = AtomicReference(false)
        val lifecycleDone = AtomicReference(false)
        val reached = withTimeoutOrNull(timeoutMs) {
            var lastLogAt = 0L
            var errorSince = 0L
            while (true) {
                val pos = positionMs.get()
                val err = error.get()
                val st = state.get()
                if (err != null) {
                    error("playback error videoId=$videoId code=$err pos=${pos}ms state=$st")
                }
                if (st == "error") {
                    if (errorSince == 0L) errorSince = System.currentTimeMillis()
                    if (System.currentTimeMillis() - errorSince > 15_000L) {
                        error("stuck in error videoId=$videoId pos=${pos}ms")
                    }
                } else {
                    errorSince = 0L
                }
                if (pos >= 60_000L && !crossed60.get()) {
                    crossed60.set(true)
                    line("crossed 60s videoId=$videoId pos=${pos}ms state=$st")
                    if (!lifecycleDone.get()) {
                        lifecycleDone.set(true)
                        withContext(Dispatchers.Default) {
                            session.player.pause(session.log)
                            line("lifecycle pause at ${positionMs.get()}ms")
                            delay(800)
                            session.player.seekTo((pos - 5_000L).coerceAtLeast(0L), session.log)
                            line("lifecycle seek back 5s")
                            delay(500)
                            session.player.play(session.log)
                            line("lifecycle resumed")
                        }
                    }
                }
                if (pos >= minMs) {
                    line("reached min videoId=$videoId pos=${pos}ms dur=${durationMs.get()} state=$st")
                    return@withTimeoutOrNull pos
                }
                val now = System.currentTimeMillis()
                if (now - lastLogAt >= 10_000L) {
                    line("progress videoId=$videoId pos=${pos}ms dur=${durationMs.get()} state=$st")
                    lastLogAt = now
                }
                if (st == "ended" && pos < minMs) {
                    error("ended early videoId=$videoId pos=${pos}ms dur=${durationMs.get()}")
                }
                delay(1_000)
            }
            @Suppress("UNREACHABLE_CODE")
            0L
        }
        statusJob.cancel()
        if (reached == null) {
            error(
                "timeout videoId=$videoId lastPos=${positionMs.get()}ms state=${state.get()} " +
                    "crossed60=${crossed60.get()} err=${error.get()}",
            )
        }
        "pos=${reached}ms crossed60=${crossed60.get()} state=${state.get()}"
    } finally {
        runCatching { session.close() }
    }
}
