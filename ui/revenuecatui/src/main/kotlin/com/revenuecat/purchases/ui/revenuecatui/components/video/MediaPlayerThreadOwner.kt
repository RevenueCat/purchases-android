package com.revenuecat.purchases.ui.revenuecatui.components.video

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Surface

internal class MediaPlayerThreadOwner(
    context: Context,
    private val muteAudio: Boolean,
    private val playerFactory: () -> MediaPlayer = { MediaPlayer() },
) {

    private companion object {
        const val POSITION_POLL_INTERVAL_MS = 250L
    }

    private data class PlaybackSnapshot(
        val prepared: Boolean = false,
        val durationMs: Int = 0,
        val currentPositionMs: Int = 0,
        val isPlaying: Boolean = false,
        val audioSessionId: Int = 0,
    )

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val workerThread = HandlerThread("RC-TextureVideoViewPlayer").apply { start() }
    private val workerHandler = Handler(workerThread.looper)

    @Volatile
    private var released = false

    @Volatile
    private var playbackSnapshot = PlaybackSnapshot()

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
            currentSurface = surface
            val mediaPlayer = player ?: return@post
            safely(execute = {
                mediaPlayer.setSurface(surface)
            }, failureMessage = { e ->
                "Could not set media surface: ${e.message}"
            })
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
            }
        }
    }

    fun prepare(
        uri: Uri,
        onPrepared: (videoWidth: Int, videoHeight: Int) -> Unit,
        onVideoSizeChanged: (videoWidth: Int, videoHeight: Int) -> Unit,
    ) {
        post {
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
                currentSurface?.let { mediaPlayer.setSurface(it) }
                mediaPlayer.isLooping = looping
                if (muteAudio) {
                    mediaPlayer.setVolume(0f, 0f)
                }

                mediaPlayer.setOnPreparedListener { preparedPlayer ->
                    if (released) return@setOnPreparedListener
                    val activePlayer = preparedPlayer ?: return@setOnPreparedListener
                    val videoWidth = getPlayerValue(activePlayer, 0) { currentPlayer -> currentPlayer.videoWidth }
                    val videoHeight = getPlayerValue(activePlayer, 0) { currentPlayer -> currentPlayer.videoHeight }
                    updatePlaybackSnapshot {
                        it.copy(
                            prepared = true,
                            durationMs = getPlayerValue(activePlayer, 0) { currentPlayer -> currentPlayer.duration },
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
        post {
            if (!playbackSnapshot.prepared) return@post
            val mediaPlayer = player ?: return@post
            safely(execute = {
                mediaPlayer.start()
                updatePlaybackSnapshot { it.copy(isPlaying = true) }
                startPositionTicker()
            }, failureMessage = { e ->
                "Could not start media player: ${e.message}"
            })
        }
    }

    fun pause() {
        post {
            if (!playbackSnapshot.prepared) return@post
            val mediaPlayer = player ?: return@post
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

    fun isPlaying(): Boolean = playbackSnapshot.isPlaying

    fun getDuration(): Int = playbackSnapshot.durationMs

    fun getCurrentPosition(): Int = playbackSnapshot.currentPositionMs

    fun getAudioSessionId(): Int = playbackSnapshot.audioSessionId

    fun release() {
        if (released) return
        released = true
        workerHandler.removeCallbacksAndMessages(null)
        workerHandler.post {
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
            safely(execute = {
                mediaPlayer?.release()
            }, failureMessage = { e ->
                "Could not release media player: ${e.message}"
            })
            workerThread.quitSafely()
        }
    }

    private fun ensurePlayer(): MediaPlayer {
        return player ?: playerFactory().also { mediaPlayer ->
            player = mediaPlayer
            updatePlaybackSnapshot {
                it.copy(audioSessionId = getPlayerValue(mediaPlayer, 0) { currentPlayer -> currentPlayer.audioSessionId })
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
}
