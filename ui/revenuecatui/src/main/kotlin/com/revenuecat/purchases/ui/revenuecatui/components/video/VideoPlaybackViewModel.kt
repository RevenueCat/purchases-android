@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.components.video

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import com.revenuecat.purchases.InternalRevenueCatAPI

/**
 * ViewModel that manages video playback state across activity recreation.
 * Survives configuration changes and activity recreation while maintaining
 * playback position and play/pause state for each video.
 */
internal class VideoPlaybackViewModel : ViewModel() {

    private val playbackStates = mutableMapOf<String, VideoPlaybackState>()

    /**
     * Save playback state for a specific video key.
     *
     * @param key Unique identifier for the video (typically based on URI and scale type)
     * @param state Current playback state to save
     */
    fun savePlaybackState(key: String, state: VideoPlaybackState) {
        playbackStates[key] = state
    }

    /**
     * Restore playback state for a specific video key.
     *
     * @param key Unique identifier for the video
     * @return Saved playback state, or default state if none exists
     */
    fun restorePlaybackState(key: String): VideoPlaybackState {
        return playbackStates[key] ?: VideoPlaybackState(positionMs = 0, playWhenReady = true)
    }

    override fun onCleared() {
        super.onCleared()
        playbackStates.clear()
    }
}

/**
 * Represents the playback state of a video at a point in time.
 *
 * @property positionMs Current playback position in milliseconds
 * @property playWhenReady Whether the video should be playing (true) or paused (false)
 */
@Stable
internal data class VideoPlaybackState(
    val positionMs: Int,
    val playWhenReady: Boolean,
)
