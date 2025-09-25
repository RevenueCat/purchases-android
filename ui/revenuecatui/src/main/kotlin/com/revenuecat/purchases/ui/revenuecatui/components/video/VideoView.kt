@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.components.video

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
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
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.net.toUri
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger

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
) {
    VideoCard(
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
    )
}

/**
 * Texture-backed video view with stock MediaController controls.
 * Implements FIT (letterbox) and FILL (center-crop) by resizing the TextureView,
 * letting the FrameLayout clip any overflow. No transforms, no buffer-size tricks.
 */
@SuppressLint("ViewConstructor")
@Suppress("TooManyFunctions")
internal class TextureVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    private val showControls: Boolean,
    private val muteAudio: Boolean,
) : FrameLayout(context, attrs), MediaController.MediaPlayerControl {

    enum class ScaleType { FIT, FILL }

    private val texture = TextureView(context)
    private val player = MediaPlayer()
    private var controller: MediaController? = null

    private var videoWidth = 0
    private var videoHeight = 0
    private var prepared = false
    private var released = false
    private var uri: Uri? = null
    private var looping = false
    private var autoStart = true
    private var scaleType = ScaleType.FIT

    // resume state across surface re-create (e.g., rotation)
    private var resumePosMs = 0
    private var resumePlayWhenReady = false

    private val layoutListener = ViewTreeObserver.OnGlobalLayoutListener { applySizing() }
    private var viewTreeObserverListening = false

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
                player.setSurface(Surface(st))
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
                if (released) return true
                // snapshot play state & position to resume after recreation
                resumePlayWhenReady = isPlaying
                resumePosMs = currentPosition
                // pause before losing surface to avoid state exceptions
                if (prepared) {
                    safely(execute = {
                        player.pause()
                    })
                }
                player.setSurface(null)
                return true // we release the surface
            }

            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {
                // No-op
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
            try {
                player.currentPosition
            } catch (_: Throwable) {
                resumePosMs
            }
        } else {
            resumePosMs
        }
        val play = if (prepared) {
            (
                try {
                    player.isPlaying
                } catch (_: Throwable) {
                    false
                }
                )
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
        if (!released) player.isLooping = loop
    }

    fun setAutoStart(enabled: Boolean) {
        autoStart = enabled
    }

    fun release() {
        if (released) return
        controller?.hide()
        safely(execute = {
            player.stop()
        })
        safely(execute = {
            player.reset()
        })
        player.release()
        released = true
        if (viewTreeObserverListening) {
            viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
            viewTreeObserverListening = false
        }
    }

    private fun prepareIfNeeded() {
        if (released) return

        uri?.let {
            prepared = false
            player.reset()
            player.setDataSource(context, it)
        } ?: return

        player.setOnPreparedListener {
            if (released) return@setOnPreparedListener
            prepared = true
            videoWidth = it.videoWidth
            videoHeight = it.videoHeight
            it.isLooping = looping
            if (muteAudio) {
                // Simple approach: just try to set volume to 0, catch any exceptions
                safely(execute = {
                    it.setVolume(0f, 0f)
                }, failureMessage = { e ->
                    "Could not mute audio: ${e.message}"
                })
            }
            applySizing()
            if (resumePosMs > 0) {
                safeSeekTo(resumePosMs)
            } else {
                // Show first frame by seeking to 1ms if no saved position is found
                if (resumePosMs == 0) safeSeekTo(1)
            }
            if (autoStart || resumePlayWhenReady) {
                start()
            }
            // Always show controller so user can see play button when autoStart is false
            if (showControls) controller?.show()
        }

        player.setOnVideoSizeChangedListener { _, w, h ->
            if (released) return@setOnVideoSizeChangedListener
            videoWidth = w
            videoHeight = h
            applySizing()
        }

        try {
            player.prepareAsync()
        } catch (e: IllegalStateException) {
            // Can happen if rotation tears down while preparing; ignore if released
            if (!released) throw e
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

    private fun safeSeekTo(position: Int) {
        safely(execute = {
            if (prepared && position > 0) player.seekTo(position)
        })
    }

    /** MediaController.MediaPlayerControl **/
    override fun start() {
        if (prepared && !released) {
            safely(execute = {
                player.start()
            })
        }
    }

    override fun pause() {
        if (prepared && !released) {
            safely(execute = {
                player.pause()
            })
        }
    }

    override fun getDuration(): Int = if (prepared && !released) player.duration else 0
    override fun getCurrentPosition(): Int =
        if (prepared && !released) player.currentPosition else resumePosMs

    override fun seekTo(pos: Int) {
        resumePosMs = pos
        safeSeekTo(pos)
    }

    override fun isPlaying(): Boolean = prepared && !released && player.isPlaying
    override fun getBufferPercentage(): Int = 0
    override fun canPause(): Boolean = true
    override fun canSeekBackward(): Boolean = true
    override fun canSeekForward(): Boolean = true
    override fun getAudioSessionId(): Int = player.audioSessionId

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        release()
    }
}

@Stable
private data class SavedPlayback(val position: Int, val play: Boolean)

// Simple static state holder as fallback when rememberSaveable fails
private object VideoStateHolder {
    private val states = mutableMapOf<String, SavedPlayback>()

    fun save(key: String, state: SavedPlayback) {
        states[key] = state
    }

    fun restore(key: String): SavedPlayback {
        val state = states[key] ?: SavedPlayback(position = 0, play = true)
        return state
    }
}

private val SavedPlaybackSaver = Saver<MutableState<SavedPlayback>, List<Any>>(
    save = {
        listOf(it.value.position, it.value.play)
    },
    restore = {
        val restored = SavedPlayback(it[0] as Int, it[1] as Boolean)
        mutableStateOf(restored)
    },
)

@Suppress("LongMethod", "LongParameterList")
@Composable
private fun VideoCard(
    scaleType: TextureVideoView.ScaleType,
    videoUri: String,
    showControls: Boolean,
    autoPlay: Boolean,
    loop: Boolean,
    muteAudio: Boolean,
    modifier: Modifier = Modifier,
) {
    val key = "video_${scaleType}_$videoUri"

    val saved = rememberSaveable(
        key = key,
        saver = SavedPlaybackSaver,
    ) {
        // Try to restore from static holder first, fallback to default
        val restored = VideoStateHolder.restore(key)
        mutableStateOf(restored)
    }

    // Remember the TextureVideoView instance across recompositions
    val videoView = remember {
        mutableStateOf<TextureVideoView?>(null)
    }

    // Capture playback state before rotation/destruction
    DisposableEffect(videoView.value) {
        onDispose {
            videoView.value?.let { view ->
                val state = view.getPlaybackState()
                val playbackState =
                    SavedPlayback(position = state.positionMs, play = state.playWhenReady)
                saved.value = playbackState
                VideoStateHolder.save(key, playbackState)
            }
        }
    }
    Box(
        modifier = modifier.clipToBounds(),
        contentAlignment = Alignment.Center,
    ) {
        AndroidView(
            factory = { ctx ->
                // Always check both saved state and static holder
                val restoredState = VideoStateHolder.restore(key)
                val usePosition = maxOf(saved.value.position, restoredState.position)
                // If we have a saved position, always try to continue playing (rotation should resume)
                val usePlay = if (usePosition > 0) {
                    // Rotation case: use the saved play state (respects if user paused)
                    saved.value.play || restoredState.play // Either source might have the correct state
                } else {
                    // Initial load case: use autoPlay parameter
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
                videoView.value = view // Update reference
                // Minimal update - avoid operations that cause restarts
                // Scale type is set in factory, no need to update here
            },
            onRelease = { view ->
                // Capture playback state BEFORE releasing
                view.run {
                    val st = getPlaybackState()
                    val playbackState =
                        SavedPlayback(position = st.positionMs, play = st.playWhenReady)
                    saved.value = playbackState
                    VideoStateHolder.save(key, playbackState)
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
