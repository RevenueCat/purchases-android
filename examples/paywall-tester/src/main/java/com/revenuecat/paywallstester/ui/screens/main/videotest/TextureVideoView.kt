package com.revenuecat.paywallstester.ui.screens.main.videotest

import android.content.Context
import android.graphics.SurfaceTexture
import android.media.MediaPlayer
import android.net.Uri
import android.util.AttributeSet
import android.view.Gravity
import android.view.Surface
import android.view.TextureView
import android.view.View
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.MediaController

/**
 * Texture-backed video view with stock MediaController controls.
 * Implements FIT (letterbox) and FILL (center-crop) by resizing the TextureView,
 * letting the FrameLayout clip any overflow. No transforms, no buffer-size tricks.
 */
class TextureVideoView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs), MediaController.MediaPlayerControl {

    enum class ScaleType { FIT, FILL }

    private val texture = TextureView(context)
    private val player = MediaPlayer()
    private var controller: MediaController? = null

    private var videoW = 0
    private var videoH = 0
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
    private var vtoListening = false

    init {
        clipToPadding = true
        clipChildren = true

        addView(texture, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT, Gravity.CENTER))

        texture.surfaceTextureListener = object : TextureView.SurfaceTextureListener {
            override fun onSurfaceTextureAvailable(st: SurfaceTexture, w: Int, h: Int) {
                if (released) return
                player.setSurface(Surface(st))
                if (!vtoListening) {
                    viewTreeObserver.addOnGlobalLayoutListener(layoutListener)
                    vtoListening = true
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
            override fun onSurfaceTextureSizeChanged(st: SurfaceTexture, w: Int, h: Int) = applySizing()
            override fun onSurfaceTextureDestroyed(st: SurfaceTexture): Boolean {
                if (released) return true
                // snapshot play state & position to resume after recreation
                resumePlayWhenReady = isPlaying
                resumePosMs = currentPosition
                // pause before losing surface to avoid state exceptions
                if (prepared) try { player.pause() } catch (_: Throwable) {}
                player.setSurface(null)
                return true // we release the surface
            }
            override fun onSurfaceTextureUpdated(st: SurfaceTexture) {}
        }

        controller = MediaController(context).apply {
            setAnchorView(this@TextureVideoView)
            setMediaPlayer(this@TextureVideoView)
            isEnabled = true
        }
        setOnClickListener { controller?.show() }
    }

    // Inside TextureVideoView.kt
    data class PlaybackState(val positionMs: Int, val playWhenReady: Boolean)

    fun getPlaybackState(): PlaybackState {
        // If not prepared yet, return last requested state (resumePosMs / resumePlayWhenReady)
        val pos = if (prepared) try { player.currentPosition } catch (_: Throwable) { resumePosMs } else resumePosMs
        val play = if (prepared) (try { player.isPlaying } catch (_: Throwable) { false }) else resumePlayWhenReady
        return PlaybackState(pos, play)
    }

    fun setPlaybackState(state: PlaybackState) {
        resumePosMs = state.positionMs
        resumePlayWhenReady = state.playWhenReady
        if (prepared) {
            safeSeekTo(resumePosMs)
            if (resumePlayWhenReady) start() else pause()
        }
    }

    /** Public API **/
    fun setVideoURI(uri: Uri) {
        this.uri = uri
        prepared = false
        resumePosMs = 0
        resumePlayWhenReady = autoStart
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
    fun setAutoStart(enabled: Boolean) { autoStart = enabled }

    fun release() {
        if (released) return
        controller?.hide()
        try { player.stop() } catch (_: Throwable) {}
        try { player.reset() } catch (_: Throwable) {}
        player.release()
        released = true
        if (vtoListening) {
            viewTreeObserver.removeOnGlobalLayoutListener(layoutListener)
            vtoListening = false
        }
    }

    /** Internals **/
    private fun prepareIfNeeded() {
        if (released) return
        val u = uri ?: return

        prepared = false
        player.reset()
        player.setDataSource(context, u)

        player.setOnPreparedListener {
            if (released) return@setOnPreparedListener
            prepared = true
            videoW = it.videoWidth
            videoH = it.videoHeight
            it.isLooping = looping
            applySizing()
            if (resumePosMs > 0) safeSeekTo(resumePosMs)
            if (autoStart || resumePlayWhenReady) start()
            controller?.show()
        }

        player.setOnVideoSizeChangedListener { _, w, h ->
            if (released) return@setOnVideoSizeChangedListener
            videoW = w
            videoH = h
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
        if (released || width == 0 || height == 0 || videoW == 0 || videoH == 0) return

        val parentW = width
        val parentH = height
        val parentAR = parentW.toFloat() / parentH
        val videoAR = videoW.toFloat() / videoH

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
        texture.visibility = View.VISIBLE
        texture.alpha = 1f
        invalidate()
    }

    private fun safeSeekTo(pos: Int) {
        try { if (prepared && pos > 0) player.seekTo(pos) } catch (_: Throwable) {}
    }

    /** MediaController.MediaPlayerControl **/
    override fun start() { if (prepared && !released) try { player.start() } catch (_: Throwable) {} }
    override fun pause() { if (prepared && !released) try { player.pause() } catch (_: Throwable) {} }
    override fun getDuration(): Int = if (prepared && !released) player.duration else 0
    override fun getCurrentPosition(): Int = if (prepared && !released) player.currentPosition else resumePosMs
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
