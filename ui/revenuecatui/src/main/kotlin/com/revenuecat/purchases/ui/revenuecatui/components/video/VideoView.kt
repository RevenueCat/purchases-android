@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.components.video

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Parcelable
import android.util.AttributeSet
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.MediaController
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import kotlinx.parcelize.Parcelize

@Suppress("LongParameterList")
@Composable
internal fun VideoView(
    videoUri: String,
    modifier: Modifier = Modifier,
    showControls: Boolean = true,
    autoPlay: Boolean = false,
    loop: Boolean = false,
    muteAudio: Boolean = false,
    contentScale: ContentScale = ContentScale.Fit,
    onReady: (() -> Unit)? = null,
) {
    Video(
        scaleType = if (contentScale == ContentScale.Fit) {
            TextureVideoView.ScaleType.FIT
        } else {
            TextureVideoView.ScaleType.FILL
        },
        videoUri = videoUri,
        showControls = showControls,
        autoPlay = autoPlay,
        loop = loop,
        muteAudio = muteAudio,
        modifier = modifier,
        onReady = onReady,
    )
}

/**
 * Texture-backed video view with stock MediaController controls.
 * Implements FIT (letterbox) and FILL (center-crop) by resizing the TextureView,
 * letting the FrameLayout clip any overflow. No transforms, no buffer-size tricks.
 */
@SuppressLint("ViewConstructor")
@Suppress("TooManyFunctions")
private class TextureVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val showControls: Boolean,
    private val muteAudio: Boolean,
) : FrameLayout(context, attrs), MediaController.MediaPlayerControl {

    enum class ScaleType { FIT, FILL }

    private val texture = TextureView(context)
    private val playerOwner = MediaPlayerThreadOwner(context = context, muteAudio = muteAudio)
    private var controller: MediaController? = null
    private var attachedSurface: Surface? = null

    private var videoWidth = 0
    private var videoHeight = 0
    private var prepared = false
    private var released = false
    private var uri: Uri? = null
    private var looping = false
    private var autoStart = true
    private var scaleType = ScaleType.FIT
    private var firstFrameRendered = false
    private var onReadyCallback: (() -> Unit)? = null

    // resume state across surface re-create (e.g., rotation)
    private var resumePosMs = 0
    private var resumePlayWhenReady = false

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener { applySizing() }
    private var viewTreeObserverListening = false
    private var prepareRequestId = 0

    init {
        clipToPadding = true
        clipChildren = true

        addView(
            texture,
            LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER),
        )

        texture.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                if (released) return
                attachedSurface = Surface(st)
                playerOwner.setSurface(attachedSurface)
                if (!viewTreeObserverListening) {
                    viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
                    viewTreeObserverListening = true
                }
                // If we were already prepared before, just resume; otherwise prepare
                if (prepared) {
                    safeSeekTo(resumePosMs)
                    if (resumePlayWhenReady) start()
                    applySizing()
                } else {
                    prepareIfNeeded()
                }
            }

            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) =
                applySizing()

            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                if (released) {
                    attachedSurface = null
                    return true
                }
                // snapshot play state & position to resume after recreation
                resumePlayWhenReady = isPlaying
                resumePosMs = currentPosition
                // pause before losing surface to avoid state exceptions
                if (prepared) {
                    pause()
                }
                playerOwner.setSurface(null)
                attachedSurface = null
                return true // we release the surface
            }

            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {
                if (!firstFrameRendered && prepared) {
                    firstFrameRendered = true
                    onReadyCallback?.invoke()
                }
            }
        }

        controller = MediaController(context).apply {
            setMediaPlayer(this@TextureVideoView)
            if (showControls) {
                setAnchorView(this@TextureVideoView)
                isEnabled = true
            }
        }
        setOnClickListener {
            if (showControls) {
                controller?.show()
            }
        }
    }

    // Inside TextureVideoView.kt
    class PlaybackState(val positionMs: Int, val playWhenReady: Boolean)

    fun getPlaybackState(): PlaybackState {
        // If not prepared yet, return last requested state (resumePosMs / resumePlayWhenReady)
        val position = if (prepared) {
            playerOwner.getCurrentPosition()
        } else {
            resumePosMs
        }
        val play = if (prepared) {
            playerOwner.isPlaying()
        } else {
            resumePlayWhenReady
        }
        return PlaybackState(position, play)
    }

    fun setPlaybackState(state: PlaybackState) {
        resumePosMs = state.positionMs
        resumePlayWhenReady = state.playWhenReady
        if (prepared) {
            safeSeekTo(resumePosMs)
            if (resumePlayWhenReady) start() else pause()
        }
    }

    fun setVideoURI(uri: Uri) {
        this.uri = uri
        prepared = false
        firstFrameRendered = false
        // Only reset position if we don't have a saved position
        if (resumePosMs == 0) {
            resumePlayWhenReady = autoStart
        }
        // Don't reset resumePosMs here - let setPlaybackState handle it
        if (texture.isAvailable && !released) prepareIfNeeded()
    }

    fun setScaleType(type: ScaleType) {
        scaleType = type
        applySizing()
    }

    fun setLooping(loop: Boolean) {
        looping = loop
        if (!released) {
            playerOwner.setLooping(loop)
        }
    }

    fun setAutoStart(enabled: Boolean) {
        autoStart = enabled
    }

    fun setOnReadyCallback(callback: (() -> Unit)?) {
        onReadyCallback = callback
        if (firstFrameRendered && callback != null) {
            callback.invoke()
        }
    }

    fun release() {
        if (released) return
        released = true
        prepared = false
        prepareRequestId += 1
        controller?.hide()
        controller = null
        playerOwner.setSurface(null)
        attachedSurface = null
        playerOwner.release()
        if (viewTreeObserverListening) {
            viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
            viewTreeObserverListening = false
        }
    }

    private fun prepareIfNeeded() {
        if (released) return

        val source = uri ?: return
        val requestId = ++prepareRequestId
        prepared = false
        playerOwner.prepare(
            uri = source,
            onPrepared = { width, height ->
                if (released || requestId != prepareRequestId) return@prepare
                prepared = true
                videoWidth = width
                videoHeight = height
                applySizing()
                if (resumePosMs > 0) {
                    safeSeekTo(resumePosMs)
                } else if (resumePosMs == 0) {
                    // Show first frame by seeking to 1ms if no saved position is found
                    safeSeekTo(1)
                }
                if (autoStart || resumePlayWhenReady) {
                    start()
                }
                // Always show controller so user can see play button when autoStart is false
                if (showControls) controller?.show()
            },
            onVideoSizeChanged = { width, height ->
                if (released || requestId != prepareRequestId) return@prepare
                videoWidth = width
                videoHeight = height
                applySizing()
            },
        )
    }

    private fun safeSeekTo(position: Int) {
        if (prepared && position > 0) {
            playerOwner.seekTo(position)
        }
    }

    private fun applySizing() {
        @Suppress("ComplexCondition")
        if (released || width == 0 || height == 0 || videoWidth == 0 || videoHeight == 0) return

        val parentW = width
        val parentH = height
        val parentAR = parentW.toFloat() / parentH
        val videoAR = videoWidth.toFloat() / videoHeight

        val lp = texture.layoutParams as LayoutParams
        if (scaleType == ScaleType.FIT) {
            if (videoAR > parentAR) {
                lp.width = parentW
                lp.height = (parentW / videoAR).toInt()
            } else {
                lp.height = parentH
                lp.width = (parentH * videoAR).toInt()
            }
        } else {
            if (videoAR > parentAR) {
                lp.height = parentH
                lp.width = (parentH * videoAR).toInt()
            } else {
                lp.width = parentW
                lp.height = (parentW / videoAR).toInt()
            }
        }
        lp.gravity = Gravity.CENTER
        texture.layoutParams = lp
        texture.visibility = VISIBLE
        texture.alpha = 1f
        invalidate()
    }

    /** MediaController.MediaPlayerControl **/
    override fun start() {
        if (prepared && !released) {
            playerOwner.start()
        }
    }

    override fun pause() {
        if (prepared && !released) {
            playerOwner.pause()
        }
    }

    override fun getDuration(): Int = if (prepared && !released) playerOwner.getDuration() else 0
    override fun getCurrentPosition(): Int =
        if (prepared && !released) playerOwner.getCurrentPosition() else resumePosMs

    override fun seekTo(pos: Int) {
        resumePosMs = pos
        safeSeekTo(pos)
    }

    override fun isPlaying(): Boolean = prepared && !released && playerOwner.isPlaying()
    override fun getBufferPercentage(): Int = 0
    override fun canPause(): Boolean = true
    override fun canSeekBackward(): Boolean = true
    override fun canSeekForward(): Boolean = true
    override fun getAudioSessionId(): Int = playerOwner.getAudioSessionId()

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        release()
    }

    @Suppress("ComplexCondition")
    fun startIfNeeded() {
        if (prepared && !released && !playerOwner.isPlaying() && autoStart) {
            start()
        }
    }
}

private class MediaPlayerThreadOwner(
    context: Context,
    private val muteAudio: Boolean,
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

    private var player: MediaPlayer? = null
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

                mediaPlayer.setOnVideoSizeChangedListener { _, width, height ->
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

    private fun ensurePlayer(): MediaPlayer {
        return player ?: MediaPlayer().also { mediaPlayer ->
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

@Suppress("LongMethod", "LongParameterList")
@Composable
private fun Video(
    scaleType: TextureVideoView.ScaleType,
    videoUri: String,
    showControls: Boolean,
    autoPlay: Boolean,
    loop: Boolean,
    muteAudio: Boolean,
    modifier: Modifier = Modifier,
    onReady: (() -> Unit)? = null,
) {
    val key = "video_${scaleType}_$videoUri"

    val savedState = rememberSaveable(key = key) {
        mutableMapOf<String, VideoPlaybackState>()
    }

    // Remember the TextureVideoView instance across recompositions
    val videoView = remember {
        mutableStateOf<TextureVideoView?>(null)
    }

    // To guarantee autoplay starts on view creation
    // The code in the factory is not as reliable for view creation but better
    // for rotation
    LaunchedEffect(videoView.value) {
        videoView.value?.run {
            setAutoStart(autoPlay)
            startIfNeeded()
        }
    }

    // Capture playback state before rotation/destruction
    DisposableEffect(videoView.value) {
        onDispose {
            videoView.value?.let { view ->
                val state = view.getPlaybackState()
                val playbackState =
                    VideoPlaybackState(positionMs = state.positionMs, playWhenReady = state.playWhenReady)
                savedState[key] = playbackState
            }
        }
    }
    Box(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { ctx ->
                val restoredState = savedState[key] ?: VideoPlaybackState(0, autoPlay)
                val usePosition = maxOf(restoredState.positionMs, restoredState.positionMs)

                // Rotation case: use the saved play state (respects if user paused)
                // If we have a saved position, always try to continue playing (rotation should resume)
                val usePlay = if (usePosition > 0) {
                    restoredState.playWhenReady
                } else {
                    autoPlay
                }

                val videoUri = videoUri.toUri()

                TextureVideoView(
                    context = ctx,
                    showControls = showControls,
                    muteAudio = muteAudio,
                ).apply {
                    videoView.value = this
                    setScaleType(scaleType)
                    setLooping(loop)
                    setAutoStart(usePlay)
                    setOnReadyCallback(onReady)
                    setVideoURI(videoUri)
                    // Set the resume position - this will be applied when video is prepared
                    if (usePosition > 0) {
                        setPlaybackState(
                            TextureVideoView.PlaybackState(
                                positionMs = usePosition,
                                playWhenReady = usePlay,
                            ),
                        )
                    }
                }
            },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                videoView.value = view
                view.setOnReadyCallback(onReady)
            },
            onRelease = { view ->
                // Capture playback state BEFORE releasing
                view.run {
                    val st = getPlaybackState()
                    val playbackState =
                        VideoPlaybackState(positionMs = st.positionMs, playWhenReady = st.playWhenReady)
                    savedState[key] = playbackState
                    release()
                }
                videoView.value = null
            },
        )
    }
}

@Suppress("TooGenericExceptionCaught")
private fun safely(execute: () -> Unit, failureMessage: (Exception) -> String? = { null }) {
    try {
        execute()
    } catch (e: Exception) {
        failureMessage(e)?.run {
            Logger.e("TextureVideoView: $this", e)
        }
    }
}

/**
 * Represents the playback state of a video at a point in time.
 *
 * @property positionMs Current playback position in milliseconds
 * @property playWhenReady Whether the video should be playing (true) or paused (false)
 */
@Parcelize
@Stable
internal data class VideoPlaybackState(
    val positionMs: Int,
    val playWhenReady: Boolean,
) : Parcelable
