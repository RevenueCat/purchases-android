package com.revenuecat.purchases.ui.revenuecatui.components.video

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface

internal class AndroidMediaPlayerFacade(
    private val mediaPlayer: MediaPlayer = MediaPlayer(),
) : MediaPlayerFacade {

    override var isLooping: Boolean
        get() = mediaPlayer.isLooping
        set(value) {
            mediaPlayer.isLooping = value
        }

    override val isPlaying: Boolean
        get() = mediaPlayer.isPlaying

    override val duration: Int
        get() = mediaPlayer.duration

    override val currentPosition: Int
        get() = mediaPlayer.currentPosition

    override val audioSessionId: Int
        get() = mediaPlayer.audioSessionId

    override val videoWidth: Int
        get() = mediaPlayer.videoWidth

    override val videoHeight: Int
        get() = mediaPlayer.videoHeight

    override fun setSurface(surface: Surface?) {
        mediaPlayer.setSurface(surface)
    }

    override fun setVolume(leftVolume: Float, rightVolume: Float) {
        mediaPlayer.setVolume(leftVolume, rightVolume)
    }

    override fun setDataSource(context: Context, uri: Uri) {
        mediaPlayer.setDataSource(context, uri)
    }

    override fun prepareAsync() {
        mediaPlayer.prepareAsync()
    }

    override fun reset() {
        mediaPlayer.reset()
    }

    override fun release() {
        mediaPlayer.release()
    }

    override fun start() {
        mediaPlayer.start()
    }

    override fun pause() {
        mediaPlayer.pause()
    }

    override fun seekTo(positionMs: Int) {
        mediaPlayer.seekTo(positionMs)
    }

    override fun setOnPreparedListener(listener: ((MediaPlayerFacade) -> Unit)?) {
        mediaPlayer.setOnPreparedListener { listener?.invoke(this) }
    }

    override fun setOnVideoSizeChangedListener(listener: ((videoWidth: Int, videoHeight: Int) -> Unit)?) {
        mediaPlayer.setOnVideoSizeChangedListener { _, width, height ->
            listener?.invoke(width, height)
        }
    }

    override fun setOnCompletionListener(listener: ((MediaPlayerFacade) -> Unit)?) {
        mediaPlayer.setOnCompletionListener { listener?.invoke(this) }
    }
}
