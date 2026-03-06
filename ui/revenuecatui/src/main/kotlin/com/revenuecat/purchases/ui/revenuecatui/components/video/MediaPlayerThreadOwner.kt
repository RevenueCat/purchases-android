package com.revenuecat.purchases.ui.revenuecatui.components.video

import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Surface

internal class MediaPlayerThreadOwner(
    context: Context,
    private val muteAudio: Boolean,
    private val playerFactory: () -> MediaPlayerFacade = { AndroidMediaPlayerFacade() },
) {

    private companion object {
        const val POSITION_POLL_INTERVAL_MS = 250L
    }

    private val appContext = context.applicationContext
    private val mainHandler = Handler(Looper.getMainLooper())
    private val workerThread = HandlerThread("RC-TextureVideoViewPlayer").apply { start() }
    private val workerHandler = Handler(workerThread.looper)

    @Volatile
    private var released = false
    @Volatile
    private var prepared = false
    @Volatile
    private var looping = false
    @Volatile
    private var cachedDurationMs = 0
    @Volatile
    private var cachedCurrentPositionMs = 0
    @Volatile
    private var cachedIsPlaying = false
    @Volatile
    private var cachedAudioSessionId = 0
    @Volatile
    private var currentSurface: Surface? = null

    private var player: MediaPlayerFacade? = null
    private var positionTickerScheduled = false

    private val positionTicker = object : Runnable {
        override fun run() {
            if (released) {
                positionTickerScheduled = false
                return
            }
            val mediaPlayer = player
            if (mediaPlayer == null || !prepared) {
                positionTickerScheduled = false
                return
            }
            val isPlayingNow = getPlayerValue(mediaPlayer, false) { it.isPlaying }
            cachedIsPlaying = isPlayingNow
            cachedCurrentPositionMs = getPlayerValue(mediaPlayer, cachedCurrentPositionMs) { it.currentPosition }
            if (isPlayingNow) {
                workerHandler.postDelayed(this, POSITION_POLL_INTERVAL_MS)
            } else {
                positionTickerScheduled = false
            }
        }
    }

    fun setSurface(surface: Surface?) {
        currentSurface = surface
        post {
            val mediaPlayer = player ?: return@post
            safely(execute = {
                mediaPlayer.setSurface(surface)
            }, failureMessage = { e ->
                "Could not set media surface: ${e.message}"
            })
        }
    }

    fun setLooping(loop: Boolean) {
        looping = loop
        post {
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
            prepared = false
            cachedIsPlaying = false
            cachedDurationMs = 0
            stopPositionTicker()

            safely(execute = {
                mediaPlayer.reset()
                currentSurface?.let { mediaPlayer.setSurface(it) }
                mediaPlayer.isLooping = looping
                if (muteAudio) {
                    mediaPlayer.setVolume(0f, 0f)
                }

                mediaPlayer.setOnPreparedListener { player ->
                    if (released) return@setOnPreparedListener
                    prepared = true
                    cachedDurationMs = getPlayerValue(player, 0) { it.duration }
                    cachedCurrentPositionMs = getPlayerValue(player, 0) { it.currentPosition }
                    cachedAudioSessionId = getPlayerValue(player, cachedAudioSessionId) { it.audioSessionId }
                    mainHandler.post {
                        if (!released) {
                            onPrepared(player.videoWidth, player.videoHeight)
                        }
                    }
                }

                mediaPlayer.setOnVideoSizeChangedListener { width, height ->
                    if (released) return@setOnVideoSizeChangedListener
                    mainHandler.post {
                        if (!released) {
                            onVideoSizeChanged(width, height)
                        }
                    }
                }

                mediaPlayer.setOnCompletionListener { player ->
                    cachedIsPlaying = false
                    cachedCurrentPositionMs = getPlayerValue(player, cachedDurationMs) { it.currentPosition }
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
            if (!prepared) return@post
            val mediaPlayer = player ?: return@post
            safely(execute = {
                mediaPlayer.start()
                cachedIsPlaying = true
                startPositionTicker()
            }, failureMessage = { e ->
                "Could not start media player: ${e.message}"
            })
        }
    }

    fun pause() {
        post {
            if (!prepared) return@post
            val mediaPlayer = player ?: return@post
            safely(execute = {
                if (mediaPlayer.isPlaying) {
                    mediaPlayer.pause()
                }
                cachedIsPlaying = false
                cachedCurrentPositionMs = mediaPlayer.currentPosition
                stopPositionTicker()
            }, failureMessage = { e ->
                "Could not pause media player: ${e.message}"
            })
        }
    }

    fun seekTo(positionMs: Int) {
        cachedCurrentPositionMs = positionMs
        post {
            if (!prepared || positionMs < 0) return@post
            val mediaPlayer = player ?: return@post
            safely(execute = {
                mediaPlayer.seekTo(positionMs)
                cachedCurrentPositionMs = positionMs
            }, failureMessage = { e ->
                "Could not seek media player: ${e.message}"
            })
        }
    }

    fun isPlaying(): Boolean = cachedIsPlaying

    fun getDuration(): Int = cachedDurationMs

    fun getCurrentPosition(): Int = cachedCurrentPositionMs

    fun getAudioSessionId(): Int = cachedAudioSessionId

    fun release() {
        if (released) return
        released = true
        cachedIsPlaying = false
        prepared = false
        currentSurface = null
        workerHandler.removeCallbacksAndMessages(null)
        workerHandler.post {
            val mediaPlayer = player
            player = null
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

    private fun ensurePlayer(): MediaPlayerFacade {
        return player ?: playerFactory().also { mediaPlayer ->
            player = mediaPlayer
            cachedAudioSessionId = getPlayerValue(mediaPlayer, 0) { it.audioSessionId }
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

    private inline fun <T> getPlayerValue(
        mediaPlayer: MediaPlayerFacade,
        fallback: T,
        valueProvider: (MediaPlayerFacade) -> T,
    ): T {
        return try {
            valueProvider(mediaPlayer)
        } catch (_: Exception) {
            fallback
        }
    }
}
