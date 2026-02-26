package com.revenuecat.purchases.ui.revenuecatui.components.video

import android.content.Context
import android.net.Uri
import android.view.Surface
import androidx.test.core.app.ApplicationProvider
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

        val fakePlayer = FakeMediaPlayerFacade(
            onSetDataSource = { _, _ -> setDataSourceCalled.countDown() },
            onRelease = {
                releaseStarted.countDown()
                allowReleaseToFinish.await(1000, TimeUnit.MILLISECONDS)
                releaseCompleted.countDown()
            },
            onPrepareAsync = { player ->
                player.triggerPrepared()
            },
        )

        val owner = MediaPlayerThreadOwner(
            context = appContext,
            muteAudio = false,
            playerFactory = { fakePlayer },
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

        val fakePlayer = FakeMediaPlayerFacade(
            operationThreadIds = operationThreadIds,
            onPrepareAsync = { player ->
                player.triggerPrepared()
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
            playerFactory = { fakePlayer },
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
}

private class FakeMediaPlayerFacade(
    private val operationThreadIds: MutableList<Long>? = null,
    private val onSetSurface: ((Surface?) -> Unit)? = null,
    private val onSetDataSource: ((Context, Uri) -> Unit)? = null,
    private val onPrepareAsync: ((FakeMediaPlayerFacade) -> Unit)? = null,
    private val onSetLooping: ((Boolean) -> Unit)? = null,
    private val onStart: (() -> Unit)? = null,
    private val onPause: (() -> Unit)? = null,
    private val onSeekTo: ((Int) -> Unit)? = null,
    private val onRelease: (() -> Unit)? = null,
) : MediaPlayerFacade {

    private var onPreparedListener: ((MediaPlayerFacade) -> Unit)? = null
    private var onVideoSizeChangedListener: ((Int, Int) -> Unit)? = null
    private var onCompletionListener: ((MediaPlayerFacade) -> Unit)? = null

    private var looping = false
    private var playing = false
    private var positionMs = 0

    override var isLooping: Boolean
        get() = looping
        set(value) {
            recordThread()
            looping = value
            onSetLooping?.invoke(value)
        }

    override val isPlaying: Boolean
        get() = playing

    override val duration: Int
        get() = 1_000

    override val currentPosition: Int
        get() = positionMs

    override val audioSessionId: Int
        get() = 1

    override val videoWidth: Int
        get() = 1280

    override val videoHeight: Int
        get() = 720

    override fun setSurface(surface: Surface?) {
        recordThread()
        onSetSurface?.invoke(surface)
    }

    override fun setVolume(leftVolume: Float, rightVolume: Float) {
        recordThread()
    }

    override fun setDataSource(context: Context, uri: Uri) {
        recordThread()
        onSetDataSource?.invoke(context, uri)
    }

    override fun prepareAsync() {
        recordThread()
        onPrepareAsync?.invoke(this) ?: triggerPrepared()
    }

    override fun reset() {
        recordThread()
        playing = false
        positionMs = 0
    }

    override fun release() {
        recordThread()
        onRelease?.invoke()
    }

    override fun start() {
        recordThread()
        playing = true
        onStart?.invoke()
    }

    override fun pause() {
        recordThread()
        playing = false
        onPause?.invoke()
    }

    override fun seekTo(positionMs: Int) {
        recordThread()
        this.positionMs = positionMs
        onSeekTo?.invoke(positionMs)
    }

    override fun setOnPreparedListener(listener: ((MediaPlayerFacade) -> Unit)?) {
        recordThread()
        onPreparedListener = listener
    }

    override fun setOnVideoSizeChangedListener(listener: ((videoWidth: Int, videoHeight: Int) -> Unit)?) {
        recordThread()
        onVideoSizeChangedListener = listener
    }

    override fun setOnCompletionListener(listener: ((MediaPlayerFacade) -> Unit)?) {
        recordThread()
        onCompletionListener = listener
    }

    fun triggerPrepared() {
        onPreparedListener?.invoke(this)
        onVideoSizeChangedListener?.invoke(videoWidth, videoHeight)
    }

    @Suppress("unused")
    fun triggerCompletion() {
        onCompletionListener?.invoke(this)
    }

    private fun recordThread() {
        operationThreadIds?.add(Thread.currentThread().id)
    }
}
