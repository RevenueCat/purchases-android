package com.revenuecat.purchases.ui.revenuecatui.components.video

import android.content.Context
import android.net.Uri
import android.view.Surface

internal interface MediaPlayerFacade {
    var isLooping: Boolean
    val isPlaying: Boolean
    val duration: Int
    val currentPosition: Int
    val audioSessionId: Int
    val videoWidth: Int
    val videoHeight: Int

    fun setSurface(surface: Surface?)
    fun setVolume(leftVolume: Float, rightVolume: Float)
    fun setDataSource(context: Context, uri: Uri)
    fun prepareAsync()
    fun reset()
    fun release()
    fun start()
    fun pause()
    fun seekTo(positionMs: Int)
    fun setOnPreparedListener(listener: ((MediaPlayerFacade) -> Unit)?)
    fun setOnVideoSizeChangedListener(listener: ((videoWidth: Int, videoHeight: Int) -> Unit)?)
    fun setOnCompletionListener(listener: ((MediaPlayerFacade) -> Unit)?)
}
