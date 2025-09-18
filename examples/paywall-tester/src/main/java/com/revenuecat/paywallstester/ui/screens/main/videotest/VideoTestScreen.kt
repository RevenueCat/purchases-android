package com.revenuecat.paywallstester.ui.screens.main.videotest

import android.net.Uri
import android.view.Gravity
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.VideoView
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView

@Composable
fun VideoTestScreen() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Text(
            text = "VideoView Content Scale",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 8.dp),
        )

        VideoCard(
            title = "FIT",
            isAspectFill = false,
        )

        VideoCard(
            title = "FILL",
            isAspectFill = true,
        )
    }
}

@Composable
private fun VideoCard(
    title: String,
    isAspectFill: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 4.dp),
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(6f / 3f) // Using a different aspect ratio to show the difference
                    .clipToBounds(), // This ensures content is clipped to the container bounds
                contentAlignment = Alignment.Center,
            ) {
                AndroidView(
                    factory = { ctx ->
                        if (isAspectFill) {
                            // For aspect fill, we use a FrameLayout container to control sizing
                            FrameLayout(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )

                                val videoView = VideoView(ctx).apply {
                                    val videoUri = Uri.parse(
                                        "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                                    )
                                    setVideoURI(videoUri)
                                    setMediaController(android.widget.MediaController(ctx))

                                    setOnPreparedListener { mediaPlayer ->
                                        mediaPlayer.isLooping = true

                                        // Get video dimensions
                                        val videoWidth = mediaPlayer.videoWidth.toFloat()
                                        val videoHeight = mediaPlayer.videoHeight.toFloat()
                                        val videoAspectRatio = videoWidth / videoHeight

                                        // Get container dimensions (we'll use post to ensure layout is complete)
                                        post {
                                            val containerWidth = this@apply.parent?.let { (it as ViewGroup).width } ?: width
                                            val containerHeight = this@apply.parent?.let { (it as ViewGroup).height } ?: height
                                            val containerAspectRatio = containerWidth.toFloat() / containerHeight.toFloat()

                                            if (containerWidth > 0 && containerHeight > 0) {
                                                val params = FrameLayout.LayoutParams(
                                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                                    FrameLayout.LayoutParams.MATCH_PARENT,
                                                )

                                                // For aspect fill: scale video to fill container, may crop
                                                if (videoAspectRatio > containerAspectRatio) {
                                                    // Video is wider than container, scale by height and crop width
                                                    params.width = (containerHeight * videoAspectRatio).toInt()
                                                    params.height = containerHeight
                                                } else {
                                                    // Video is taller than container, scale by width and crop height
                                                    params.width = containerWidth
                                                    params.height = (containerWidth / videoAspectRatio).toInt()
                                                }

                                                params.gravity = Gravity.CENTER
                                                layoutParams = params
                                            }
                                        }

                                        start()
                                    }
                                }

                                addView(videoView)
                            }
                        } else {
                            // For fit mode, use regular VideoView (default behavior)
                            VideoView(ctx).apply {
                                layoutParams = ViewGroup.LayoutParams(
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                    ViewGroup.LayoutParams.MATCH_PARENT,
                                )

                                val videoUri = Uri.parse(
                                    "https://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4",
                                )
                                setVideoURI(videoUri)
                                setMediaController(android.widget.MediaController(ctx))

                                setOnPreparedListener { mediaPlayer ->
                                    mediaPlayer.isLooping = true
                                    start()
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun VideoTestScreenPreview() {
    MaterialTheme {
        VideoTestScreen()
    }
}
