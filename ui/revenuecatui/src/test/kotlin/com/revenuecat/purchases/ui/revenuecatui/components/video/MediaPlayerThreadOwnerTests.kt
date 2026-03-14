package com.revenuecat.purchases.ui.revenuecatui.components.video

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.view.Surface
import androidx.test.core.app.ApplicationProvider
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Collections
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.system.measureTimeMillis

@RunWith(RobolectricTestRunner::class)
class MediaPlayerThreadOwnerTests {

    private val appContext: Context = ApplicationProvider.getApplicationContext()
    private val uri = Uri.parse("https://example.com/video.mp4")

    @Test
    fun `release does not block caller when player release blocks`() {
        val setDataSourceCalled = CountDownLatch(1)
        val releaseStarted = CountDownLatch(1)
        val allowReleaseToFinish = CountDownLatch(1)
        val releaseCompleted = CountDownLatch(1)

        val mediaPlayer = createMockMediaPlayer(
            onSetDataSource = { _, _ -> setDataSourceCalled.countDown() },
            onRelease = {
                releaseStarted.countDown()
                allowReleaseToFinish.await(1000, TimeUnit.MILLISECONDS)
                releaseCompleted.countDown()
            },
            onPrepareAsync = { player, preparedListener, _, _ ->
                preparedListener?.onPrepared(player)
            },
        )

        val owner = MediaPlayerThreadOwner(
            context = appContext,
            muteAudio = false,
            playerFactory = { mediaPlayer },
        )

        owner.prepare(
            uri = uri,
            onPrepared = { _, _ -> },
            onVideoSizeChanged = { _, _ -> },
        )

        assertThat(setDataSourceCalled.await(200, TimeUnit.MILLISECONDS)).isTrue()

        val elapsedMs = measureTimeMillis {
            owner.release()
        }

        assertThat(elapsedMs).isLessThan(100L)
        assertThat(releaseStarted.await(200, TimeUnit.MILLISECONDS)).isTrue()

        allowReleaseToFinish.countDown()
        assertThat(releaseCompleted.await(200, TimeUnit.MILLISECONDS)).isTrue()
    }

    @Test
    fun `all player calls are serialized on a single worker thread`() {
        val operationThreadIds = Collections.synchronizedList(mutableListOf<Long>())
        val prepareCompleted = CountDownLatch(1)
        val startCalled = CountDownLatch(1)
        val seekCalled = CountDownLatch(1)
        val pauseCalled = CountDownLatch(1)
        val setLoopingCalled = CountDownLatch(1)
        val setSurfaceCalled = CountDownLatch(1)
        val releaseCalled = CountDownLatch(1)

        val mediaPlayer = createMockMediaPlayer(
            operationThreadIds = operationThreadIds,
            onPrepareAsync = { player, preparedListener, videoSizeChangedListener, _ ->
                preparedListener?.onPrepared(player)
                videoSizeChangedListener?.onVideoSizeChanged(player, 1280, 720)
                prepareCompleted.countDown()
            },
            onStart = { startCalled.countDown() },
            onSeekTo = { seekCalled.countDown() },
            onPause = { pauseCalled.countDown() },
            onSetLooping = { setLoopingCalled.countDown() },
            onSetSurface = { setSurfaceCalled.countDown() },
            onRelease = { releaseCalled.countDown() },
        )

        val owner = MediaPlayerThreadOwner(
            context = appContext,
            muteAudio = false,
            playerFactory = { mediaPlayer },
        )

        owner.prepare(
            uri = uri,
            onPrepared = { _, _ -> },
            onVideoSizeChanged = { _, _ -> },
        )
        assertThat(prepareCompleted.await(200, TimeUnit.MILLISECONDS)).isTrue()

        owner.start()
        assertThat(startCalled.await(200, TimeUnit.MILLISECONDS)).isTrue()

        owner.seekTo(250)
        assertThat(seekCalled.await(200, TimeUnit.MILLISECONDS)).isTrue()

        owner.pause()
        assertThat(pauseCalled.await(200, TimeUnit.MILLISECONDS)).isTrue()

        owner.setLooping(true)
        assertThat(setLoopingCalled.await(200, TimeUnit.MILLISECONDS)).isTrue()

        owner.setSurface(null)
        assertThat(setSurfaceCalled.await(200, TimeUnit.MILLISECONDS)).isTrue()

        owner.release()
        assertThat(releaseCalled.await(200, TimeUnit.MILLISECONDS)).isTrue()

        assertThat(operationThreadIds).isNotEmpty
        assertThat(operationThreadIds.toSet()).hasSize(1)
        assertThat(operationThreadIds.first()).isNotEqualTo(Thread.currentThread().id)
    }

    @Test
    fun `clearSurfaceBlocking waits for worker thread detach before returning`() {
        val prepareCompleted = CountDownLatch(1)
        val detachStarted = CountDownLatch(1)
        val allowDetachToFinish = CountDownLatch(1)
        val clearSurfaceReturned = CountDownLatch(1)
        val events = Collections.synchronizedList(mutableListOf<String>())

        val mediaPlayer = createMockMediaPlayer(
            onPrepareAsync = { player, preparedListener, _, _ ->
                preparedListener?.onPrepared(player)
                prepareCompleted.countDown()
            },
            onSetSurface = { surface ->
                if (surface == null) {
                    events.add("detach-started")
                    detachStarted.countDown()
                    allowDetachToFinish.await(1000, TimeUnit.MILLISECONDS)
                    events.add("detach-finished")
                }
            },
        )

        val owner = MediaPlayerThreadOwner(
            context = appContext,
            muteAudio = false,
            playerFactory = { mediaPlayer },
        )

        owner.prepare(
            uri = uri,
            onPrepared = { _, _ -> },
            onVideoSizeChanged = { _, _ -> },
        )
        assertThat(prepareCompleted.await(200, TimeUnit.MILLISECONDS)).isTrue()

        val detachCaller = Thread {
            owner.clearSurfaceBlocking()
            events.add("caller-returned")
            clearSurfaceReturned.countDown()
        }

        detachCaller.start()

        assertThat(detachStarted.await(200, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(clearSurfaceReturned.await(100, TimeUnit.MILLISECONDS)).isFalse()

        allowDetachToFinish.countDown()

        assertThat(clearSurfaceReturned.await(200, TimeUnit.MILLISECONDS)).isTrue()
        assertThat(events).containsExactly("detach-started", "detach-finished", "caller-returned")

        owner.release()
    }
}

@Suppress("LongMethod", "LongParameterList")
private fun createMockMediaPlayer(
    operationThreadIds: MutableList<Long>? = null,
    onSetSurface: ((Surface?) -> Unit)? = null,
    onSetDataSource: ((Context, Uri) -> Unit)? = null,
    onPrepareAsync: ((
        MediaPlayer,
        MediaPlayer.OnPreparedListener?,
        MediaPlayer.OnVideoSizeChangedListener?,
        MediaPlayer.OnCompletionListener?,
    ) -> Unit)? = null,
    onSetLooping: ((Boolean) -> Unit)? = null,
    onStart: (() -> Unit)? = null,
    onPause: (() -> Unit)? = null,
    onSeekTo: ((Int) -> Unit)? = null,
    onRelease: (() -> Unit)? = null,
): MediaPlayer {
    val mediaPlayer = mockk<MediaPlayer>(relaxed = true)
    val preparedListenerSlot = slot<MediaPlayer.OnPreparedListener>()
    val videoSizeChangedListenerSlot = slot<MediaPlayer.OnVideoSizeChangedListener>()
    val completionListenerSlot = slot<MediaPlayer.OnCompletionListener>()

    var playing = false
    var positionMs = 0

    every { mediaPlayer.isLooping = any() } answers {
        recordThread(operationThreadIds)
        onSetLooping?.invoke(firstArg())
    }
    every { mediaPlayer.isPlaying } answers {
        recordThread(operationThreadIds)
        playing
    }
    every { mediaPlayer.duration } answers {
        recordThread(operationThreadIds)
        1_000
    }
    every { mediaPlayer.currentPosition } answers {
        recordThread(operationThreadIds)
        positionMs
    }
    every { mediaPlayer.audioSessionId } answers {
        recordThread(operationThreadIds)
        1
    }
    every { mediaPlayer.videoWidth } answers {
        recordThread(operationThreadIds)
        1280
    }
    every { mediaPlayer.videoHeight } answers {
        recordThread(operationThreadIds)
        720
    }
    every { mediaPlayer.setSurface(any()) } answers {
        recordThread(operationThreadIds)
        onSetSurface?.invoke(firstArg())
    }
    every { mediaPlayer.setVolume(any(), any()) } answers {
        recordThread(operationThreadIds)
    }
    every { mediaPlayer.setDataSource(any(), any<Uri>()) } answers {
        recordThread(operationThreadIds)
        onSetDataSource?.invoke(firstArg(), secondArg())
    }
    every { mediaPlayer.prepareAsync() } answers {
        recordThread(operationThreadIds)
        onPrepareAsync?.invoke(
            mediaPlayer,
            capturedOrNull(preparedListenerSlot),
            capturedOrNull(videoSizeChangedListenerSlot),
            capturedOrNull(completionListenerSlot),
        )
    }
    every { mediaPlayer.reset() } answers {
        recordThread(operationThreadIds)
        playing = false
        positionMs = 0
    }
    every { mediaPlayer.release() } answers {
        recordThread(operationThreadIds)
        onRelease?.invoke()
    }
    every { mediaPlayer.start() } answers {
        recordThread(operationThreadIds)
        playing = true
        onStart?.invoke()
    }
    every { mediaPlayer.pause() } answers {
        recordThread(operationThreadIds)
        playing = false
        onPause?.invoke()
    }
    every { mediaPlayer.seekTo(any<Int>()) } answers {
        recordThread(operationThreadIds)
        positionMs = firstArg()
        onSeekTo?.invoke(positionMs)
    }
    every { mediaPlayer.setOnPreparedListener(capture(preparedListenerSlot)) } answers {
        recordThread(operationThreadIds)
    }
    every { mediaPlayer.setOnVideoSizeChangedListener(capture(videoSizeChangedListenerSlot)) } answers {
        recordThread(operationThreadIds)
    }
    every { mediaPlayer.setOnCompletionListener(capture(completionListenerSlot)) } answers {
        recordThread(operationThreadIds)
    }

    return mediaPlayer
}

private fun <T> capturedOrNull(slot: CapturingSlot<T>): T? {
    return if (slot.isCaptured) slot.captured else null
}

private fun recordThread(operationThreadIds: MutableList<Long>?) {
    operationThreadIds?.add(Thread.currentThread().id)
}
