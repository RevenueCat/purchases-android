@file:OptIn(ExperimentalCoroutinesApi::class)

package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.ui.revenuecatui.helpers.Logger
import io.mockk.every
import io.mockk.just
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.concurrent.thread

class ErrorPresentationQueueTest {

    private val scope = TestScope(UnconfinedTestDispatcher())
    private val queue = ErrorPresentationQueue(scope)
    private val presenter = RecordingPresenter()
    private val completions = mutableListOf<String>()

    @Before
    fun setup() {
        mockkObject(Logger)
        every { Logger.e(any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkObject(Logger)
    }

    @Test
    fun `the first error is presented right away`() {
        present("first")

        assertThat(presenter.presented).containsExactly("first")
        assertThat(completions).isEmpty()
    }

    @Test
    fun `a second error waits until the first completion completes`() {
        present("first")
        present("second")

        assertThat(presenter.presented).containsExactly("first")

        presenter.complete(0)

        assertThat(completions).containsExactly("first")
        assertThat(presenter.presented).containsExactly("first", "second")
    }

    @Test
    fun `errors from different presenter instances share the queue`() {
        val other = RecordingPresenter()
        present("first")
        queue.serialized(other).present(params("second")) { completions += "second" }

        assertThat(other.presented).isEmpty()

        presenter.complete(0)

        assertThat(other.presented).containsExactly("second")
    }

    @Test
    fun `only the first completion of a presentation counts`() {
        present("first")
        present("second")

        presenter.complete(0)
        presenter.complete(0)

        assertThat(completions).containsExactly("first")
        assertThat(presenter.presented).containsExactly("first", "second")
    }

    @Test
    fun `a presenter that throws is logged and the queue moves on`() {
        val throwing = ErrorPresenter { _, _ -> error("Simulated.") }
        queue.serialized(throwing).present(params("first")) { completions += "first" }
        present("second")

        assertThat(completions).containsExactly("first")
        assertThat(presenter.presented).containsExactly("second")
        verify(exactly = 1) { Logger.e(match { it.startsWith("Error presenter failed") }) }
    }

    @Test
    fun `a presenter completing inside present does not block the next error`() {
        val immediate = ErrorPresenter { _, completion -> completion.complete() }
        queue.serialized(immediate).present(params("first")) { completions += "first" }
        present("second")

        assertThat(completions).containsExactly("first")
        assertThat(presenter.presented).containsExactly("second")
    }

    @Test
    fun `an error enqueued from inside present is presented after the current one completes`() {
        val reentrant = ErrorPresenter { _, completion ->
            present("second")
            completion.complete()
        }
        queue.serialized(reentrant).present(params("first")) { completions += "first" }

        assertThat(completions).containsExactly("first")
        assertThat(presenter.presented).containsExactly("second")
    }

    @Test
    fun `a completion from another thread advances the queue on the scope's thread`() {
        val dispatcher = StandardTestDispatcher()
        val scope = TestScope(dispatcher)
        val queue = ErrorPresentationQueue(scope)
        val presenter = RecordingPresenter()
        queue.serialized(presenter).present(params("first")) { completions += "first" }
        queue.serialized(presenter).present(params("second")) { completions += "second" }

        thread { presenter.complete(0) }.join()

        assertThat(completions).containsExactly("first")
        assertThat(presenter.presented).containsExactly("first")
        scope.advanceUntilIdle()
        assertThat(presenter.presented).containsExactly("first", "second")
    }

    private fun present(name: String) = queue.serialized(presenter).present(params(name)) { completions += name }

    private fun params(name: String) = ErrorPresenter.Params(
        PurchasesError(PurchasesErrorCode.ConfigurationError, name),
        "test_checkpoint",
        emptyMap(),
        ErrorPresentationStage.BEFORE_FLOW_PRESENTATION,
    )

    private class RecordingPresenter : ErrorPresenter {
        val presented = mutableListOf<String>()
        private val completions = mutableListOf<ErrorPresenter.Completion>()

        override fun present(params: ErrorPresenter.Params, completion: ErrorPresenter.Completion) {
            presented += params.error.underlyingErrorMessage!!
            completions += completion
        }

        fun complete(index: Int) = completions[index].complete()
    }
}

