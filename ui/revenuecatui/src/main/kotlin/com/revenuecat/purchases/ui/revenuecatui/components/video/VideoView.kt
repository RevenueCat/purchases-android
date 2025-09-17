package com.revenuecat.purchases.ui.revenuecatui.components.video

import android.content.Context
import android.net.Uri
import android.widget.MediaController
import android.widget.VideoView
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

@Suppress("LongParameterList")
@Composable
fun VideoView(
    context: Context,
    videoUri: Uri,
    modifier: Modifier = Modifier,
    showControls: Boolean = true,
    autoPlay: Boolean = false,
    loop: Boolean = false,
    muteAudio: Boolean = false,
) {
    AndroidView(
        factory = { context ->
            VideoView(context).apply {
                if (showControls) {
                    val mediaController = MediaController(context)
                    mediaController.setAnchorView(this)
                    setMediaController(mediaController)
                }

                setVideoURI(videoUri)

                setOnCompletionListener { mediaPlayer ->
                    if (muteAudio) {
                        mediaPlayer.setVolume(0f, 0f)
                    }
                    if (loop) {
                        mediaPlayer.isLooping = true
                        start()
                    }
                }

                setOnPreparedListener { mediaPlayer ->
                    if (muteAudio) {
                        mediaPlayer.setVolume(0f, 0f)
                    }
                    mediaPlayer.isLooping = loop
                    if (autoPlay) {
                        start()
                    }
                }

//                // Handle errors gracefully
//                setOnErrorListener { one, two, three ->
//                    // Return true to indicate we handled the error
//                    false
//                }
            }
        },
        update = { it },
        modifier = modifier
    )
}
