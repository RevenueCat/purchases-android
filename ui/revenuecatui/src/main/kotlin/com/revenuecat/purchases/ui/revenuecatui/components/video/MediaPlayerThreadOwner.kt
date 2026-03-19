package com.revenuecat.purchases.ui.revenuecatui.components.video

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Surface
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

@Suppress("TooManyFunctions")
internal class MediaPlayerThreadOwner(
    context: Context,
    private val muteAudio: Boolean,
    private val playerFactory: () -> MediaPlayer = { MediaPlayer() },
) {

    private companion object {
        const val POSITION_POLL_INTERVAL_MS = 250L
        const val DETACH_SURFACE_AWAIT_TIMEOUT_MS = 1_000L
    }

    private data class PlaybackSnapshot(
        val prepared: Boolean = false,
        val durationMs: Int = 0,
        val currentPositionMs: Int = 0,
        val isPlaying: Boolean = false,
        val audioSessionId: Int = 0,
    )

    private data class PendingPlaybackState(
        val isPlaying: Boolean,
        val commandId: Long,
    )

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val workerThread = HandlerThread("RC-TextureVideoViewPlayer").apply { start() }
    private val workerHandler = Handler(workerThread.looper)

    @Volatile
    private var released = false

    @Volatile
    private var playbackSnapshot = PlaybackSnapshot()
    private val playbackCommandId = AtomicLong(0)

    @Volatile
    private var pendingPlaybackState: PendingPlaybackState? = null

    private var looping = false
    private var currentSurface: Surface? = null
    private var player: MediaPlayer? = null
    private var positionTickerScheduled = false

    private val positionTicker = object : Runnable {
        override fun run() {
            if (released) {
                positionTickerScheduled = false
                return
            }

            val mediaPlayer = player
            if (mediaPlayer == null || !playbackSnapshot.prepared) {
                positionTickerScheduled = false
                return
            }

            val isPlayingNow = getPlayerValue(mediaPlayer, false) { it.isPlaying }
            updatePlaybackSnapshot {
                it.copy(
                    isPlaying = isPlayingNow,
                    currentPositionMs = getPlayerValue(mediaPlayer, it.currentPositionMs) { currentPlayer ->
                        currentPlayer.currentPosition
                    },
                )
            }

            if (isPlayingNow) {
                workerHandler.postDelayed(this, POSITION_POLL_INTERVAL_MS)
            } else {
                positionTickerScheduled = false
            }
        }
    }

    fun setSurface(surface: Surface?) {
        post {
            setSurfaceInternal(surface)
        }
    }

    /**
     * Detaches the current [Surface] from [MediaPlayer] and optionally releases [surfaceToRelease]
     * once detach has run on the worker thread.
     *
     * This is primarily used by `TextureVideoView` to preserve ordering when a `SurfaceTexture`
     * is destroyed: the player must process `setSurface(null)` before the associated [Surface] is
     * released, or some devices can throw native rendering errors.
     *
     * The wait is bounded to avoid indefinite main-thread stalls. If the timeout is reached, this
     * method returns and the release will complete asynchronously on the worker thread.
     */
    fun clearSurfaceBlocking(surfaceToRelease: Surface? = null) {
        if (Looper.myLooper() == workerThread.looper) {
            detachAndReleaseSurface(surfaceToRelease)
            return
        }

        val detached = CountDownLatch(1)
        val posted = workerHandler.post {
            try {
                detachAndReleaseSurface(surfaceToRelease)
            } finally {
                detached.countDown()
            }
        }

        if (!posted) {
            // Looper is already shutting down. Detach can no longer be scheduled, but we still
            // own the provided surface and must release it to avoid leaking native resources.
            releaseSurface(surfaceToRelease)
            Logger.w(
                "TextureVideoView: Could not post surface detach to worker thread. " +
                    "Released provided surface locally.",
            )
            return
        }

        try {
            val detachedInTime = detached.await(
                DETACH_SURFACE_AWAIT_TIMEOUT_MS,
                TimeUnit.MILLISECONDS,
            )
            if (!detachedInTime) {
                Logger.w(
                    "TextureVideoView: Timed out waiting for surface detach on worker thread. " +
                        "Surface release will complete asynchronously on the worker thread.",
                )
            }
        } catch (_: InterruptedException) {
            Thread.currentThread().interrupt()
            Logger.w(
                "TextureVideoView: Interrupted while waiting for surface detach. " +
                    "Surface release will complete asynchronously on the worker thread.",
            )
        }
    }

    fun setLooping(loop: Boolean) {
        post {
            looping = loop
            player?.let { mediaPlayer ->
                safely(execute = {
                    mediaPlayer.isLooping = loop
                }, failureMessage = { e ->
                    "Could not set looping mode: ${e.message}"
                })
            } ?: run {
                Logger.w(
                    "TextureVideoView: Looping was set before media player initialization. Value cached for prepare.",
                )
            }
        }
    }

    @Suppress("LongMethod")
    fun prepare(
        uri: Uri,
        onPrepared: (videoWidth: Int, videoHeight: Int) -> Unit,
        onVideoSizeChanged: (videoWidth: Int, videoHeight: Int) -> Unit,
    ) {
        post {
            clearPendingPlaybackState()
            val mediaPlayer = ensurePlayer()
            updatePlaybackSnapshot {
                it.copy(
                    prepared = false,
                    isPlaying = false,
                    durationMs = 0,
                    currentPositionMs = 0,
                )
            }
            stopPositionTicker()

            safely(execute = {
                mediaPlayer.reset()
                val surface = currentSurface
                if (surface != null) {
                    mediaPlayer.setSurface(surface)
                } else {
                    Logger.w(
                        "TextureVideoView: Preparing media player without a surface." +
                            "Audio may play before video is attached.",
                    )
                }
                mediaPlayer.isLooping = looping
                if (muteAudio) {
                    mediaPlayer.setVolume(0f, 0f)
                }

                mediaPlayer.setOnPreparedListener { preparedPlayer ->
                    if (released) return@setOnPreparedListener
                    val activePlayer = preparedPlayer ?: return@setOnPreparedListener
                    val videoWidth = getPlayerValue(activePlayer, 0) {
                            currentPlayer ->
                        currentPlayer.videoWidth
                    }
                    val videoHeight = getPlayerValue(activePlayer, 0) {
                            currentPlayer ->
                        currentPlayer.videoHeight
                    }
                    updatePlaybackSnapshot {
                        it.copy(
                            prepared = true,
                            durationMs = getPlayerValue(activePlayer, 0) {
                                    currentPlayer ->
                                currentPlayer.duration
                            },
                            currentPositionMs = getPlayerValue(activePlayer, 0) { currentPlayer ->
                                currentPlayer.currentPosition
                            },
                            audioSessionId = getPlayerValue(activePlayer, it.audioSessionId) { currentPlayer ->
                                currentPlayer.audioSessionId
                            },
                        )
                    }
                    mainHandler.post {
                        if (!released) {
                            onPrepared(videoWidth, videoHeight)
                        }
                    }
                }

                mediaPlayer.setOnVideoSizeChangedListener { _, width, height ->
                    if (released) return@setOnVideoSizeChangedListener
                    mainHandler.post {
                        if (!released) {
                            onVideoSizeChanged(width, height)
                        }
                    }
                }

                mediaPlayer.setOnCompletionListener { completedPlayer ->
                    val activePlayer = completedPlayer ?: return@setOnCompletionListener
                    updatePlaybackSnapshot {
                        it.copy(
                            isPlaying = false,
                            currentPositionMs = getPlayerValue(activePlayer, it.durationMs) { currentPlayer ->
                                currentPlayer.currentPosition
                            },
                        )
                    }
                    clearPendingPlaybackState()
                    stopPositionTicker()
                }

                mediaPlayer.setDataSource(appContext, uri)
                mediaPlayer.prepareAsync()
            }, failureMessage = { e ->
                "Could not prepare media player: ${e.message}"
            })
        }
    }

    fun start() {
        if (released || !playbackSnapshot.prepared) return
        val commandId = markPendingPlaybackState(isPlaying = true)
        post {
            if (!playbackSnapshot.prepared) {
                clearPendingPlaybackState(commandId)
                return@post
            }
            val mediaPlayer = player
            if (mediaPlayer == null) {
                clearPendingPlaybackState(commandId)
                return@post
            }
            safely(execute = {
                mediaPlayer.start()
                updatePlaybackSnapshot { it.copy(isPlaying = true) }
                startPositionTicker()
            }, failureMessage = { e ->
                "Could not start media player: ${e.message}"
            })
            clearPendingPlaybackState(commandId)
        }
    }

    fun pause() {
        if (released || !playbackSnapshot.prepared) return
        val commandId = markPendingPlaybackState(isPlaying = false)
        post {
            if (!playbackSnapshot.prepared) {
                clearPendingPlaybackState(commandId)
                return@post
            }
            val mediaPlayer = player
            if (mediaPlayer == null) {
                clearPendingPlaybackState(commandId)
                return@post
            }
            safely(execute = {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                }
                updatePlaybackSnapshot {
                    it.copy(
                        isPlaying = false,
                        currentPositionMs = getPlayerValue(mediaPlayer, it.currentPositionMs) { currentPlayer ->
                            currentPlayer.currentPosition
                        },
                    )
                }
                stopPositionTicker()
            }, failureMessage = { e ->
                "Could not pause media player: ${e.message}"
            })
            clearPendingPlaybackState(commandId)
        }
    }

    fun seekTo(positionMs: Int) {
        post {
            if (!playbackSnapshot.prepared || positionMs < 0) return@post
            val mediaPlayer = player ?: return@post
            safely(execute = {
                mediaPlayer.seekTo(positionMs)
                updatePlaybackSnapshot { it.copy(currentPositionMs = positionMs) }
            }, failureMessage = { e ->
                "Could not seek media player: ${e.message}"
            })
        }
    }

    fun isPlaying(): Boolean = pendingPlaybackState?.isPlaying ?: playbackSnapshot.isPlaying

    fun getDuration(): Int = playbackSnapshot.durationMs

    fun getCurrentPosition(): Int = playbackSnapshot.currentPositionMs

    fun getAudioSessionId(): Int = playbackSnapshot.audioSessionId

    fun release(surfaceToRelease: Surface? = null) {
        if (released) {
            releaseSurface(surfaceToRelease)
            return
        }
        released = true
        clearPendingPlaybackState()
        workerHandler.post {
            try {
                updatePlaybackSnapshot {
                    it.copy(
                        prepared = false,
                        isPlaying = false,
                    )
                }
                val mediaPlayer = player
                player = null
                currentSurface = null
                stopPositionTicker()
                safely(execute = {
                    mediaPlayer?.setSurface(null)
                })
                releaseSurface(surfaceToRelease)
                safely(execute = {
                    mediaPlayer?.release()
                }, failureMessage = { e ->
                    "Could not release media player: ${e.message}"
                })
            } finally {
                workerThread.quitSafely()
            }
        }
    }

    private fun ensurePlayer(): MediaPlayer {
        return player ?: playerFactory().also { mediaPlayer ->
            player = mediaPlayer
            updatePlaybackSnapshot {
                it.copy(
                    audioSessionId = getPlayerValue(mediaPlayer, 0) {
                            currentPlayer ->
                        currentPlayer.audioSessionId
                    },
                )
            }
            currentSurface?.let { surface ->
                safely(execute = {
                    mediaPlayer.setSurface(surface)
                }, failureMessage = { e ->
                    "Could not attach media surface: ${e.message}"
                })
            }
        }
    }

    private fun setSurfaceInternal(surface: Surface?) {
        currentSurface = surface
        val mediaPlayer = player ?: return
        safely(execute = {
            mediaPlayer.setSurface(surface)
        }, failureMessage = { e ->
            "Could not set media surface: ${e.message}"
        })
    }

    private fun detachAndReleaseSurface(surfaceToRelease: Surface?) {
        setSurfaceInternal(null)
        releaseSurface(surfaceToRelease)
    }

    private fun releaseSurface(surfaceToRelease: Surface?) {
        safely(execute = {
            surfaceToRelease?.release()
        }, failureMessage = { e ->
            "Could not release media surface: ${e.message}"
        })
    }

    private fun post(operation: () -> Unit) {
        if (released) return
        workerHandler.post {
            if (!released) {
                operation()
            }
        }
    }

    private fun startPositionTicker() {
        if (positionTickerScheduled) return
        positionTickerScheduled = true
        workerHandler.post(positionTicker)
    }

    private fun stopPositionTicker() {
        positionTickerScheduled = false
        workerHandler.removeCallbacks(positionTicker)
    }

    private inline fun updatePlaybackSnapshot(transform: (PlaybackSnapshot) -> PlaybackSnapshot) {
        playbackSnapshot = transform(playbackSnapshot)
    }

    private fun markPendingPlaybackState(isPlaying: Boolean): Long {
        val commandId = playbackCommandId.incrementAndGet()
        pendingPlaybackState = PendingPlaybackState(isPlaying = isPlaying, commandId = commandId)
        return commandId
    }

    private fun clearPendingPlaybackState(commandId: Long? = null) {
        val currentPendingState = pendingPlaybackState ?: return
        if (commandId == null || currentPendingState.commandId == commandId) {
            pendingPlaybackState = null
        }
    }

    private inline fun <T> getPlayerValue(
        mediaPlayer: MediaPlayer,
        fallback: T,
        valueProvider: (MediaPlayer) -> T,
    ): T {
        return try {
            valueProvider(mediaPlayer)
        } catch (_: Exception) {
            fallback
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private inline fun safely(execute: () -> Unit, failureMessage: (Exception) -> String? = { null }) {
        try {
            execute()
        } catch (e: Exception) {
            failureMessage(e)?.run {
                Logger.e("TextureVideoView: $this", e)
            }
        }
    }
}
