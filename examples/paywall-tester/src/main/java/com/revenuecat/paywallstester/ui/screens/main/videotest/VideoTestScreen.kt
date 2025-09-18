package com.revenuecat.paywallstester.ui.screens.main.videotest

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun VideoTestScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(1.dp),
    ) {
        VideoCard(scaleType = TextureVideoView.ScaleType.FIT, containerAspect = 16f / 9f)
        VideoCard(scaleType = TextureVideoView.ScaleType.FILL, containerAspect = 1f)
    }
}

@Stable
private data class SavedPlayback(val pos: Int, val play: Boolean)

private val SavedPlaybackSaver = Saver<MutableState<SavedPlayback>, List<Any>>(
    save = { listOf(it.value.pos, it.value.play) },
    restore = { mutableStateOf(SavedPlayback(it[0] as Int, it[1] as Boolean)) },
)

@Composable
private fun VideoCard(
    scaleType: TextureVideoView.ScaleType,
    containerAspect: Float = 16f / 16f,
) {
    val saved = rememberSaveable(saver = SavedPlaybackSaver) {
        mutableStateOf(SavedPlayback(pos = 0, play = true)) // Start with autoplay = true
    }

    // Remember the TextureVideoView instance across recompositions
    val videoView = remember {
        mutableStateOf<TextureVideoView?>(null)
    }

    // Capture playback state before rotation/destruction
    DisposableEffect(Unit) {
        onDispose {
            videoView.value?.let { view ->
                val state = view.getPlaybackState()
                saved.value = SavedPlayback(pos = state.positionMs, play = state.playWhenReady)
            }
        }
    }
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(4.dp),
    ) {
        Column(
            modifier = Modifier.padding(2.dp),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth() // <- full width of padded area (can be 0 padding)
                    .aspectRatio(containerAspect)
                    .clipToBounds(),
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    factory = { ctx ->
                        TextureVideoView(ctx).apply {
                            videoView.value = this // Store reference
                            setScaleType(scaleType)
                            setLooping(true)
                            setVideoURI(
                                Uri.parse(
                                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                                ),
                            )
                            // Restore saved state after creation
                            setPlaybackState(
                                TextureVideoView.PlaybackState(
                                    positionMs = saved.value.pos,
                                    playWhenReady = saved.value.play,
                                ),
                            )
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
                        (view as? TextureVideoView)?.let { v ->
                            val st = v.getPlaybackState()
                            saved.value = SavedPlayback(pos = st.positionMs, play = st.playWhenReady)
                            v.release()
                        }
                        videoView.value = null // Clear reference
                    },
                )
            }
        }
    }
}
