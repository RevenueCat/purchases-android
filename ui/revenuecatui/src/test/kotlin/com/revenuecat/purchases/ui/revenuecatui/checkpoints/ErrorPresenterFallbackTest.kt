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
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.concurrent.thread

class ErrorPresenterFallbackTest {

    private val params = ErrorPresenter.Params(
        PurchasesError(PurchasesErrorCode.StoreProblemError, "Simulated."),
        "test_checkpoint",
        emptyMap(),
        CheckpointFailureKind.FLOW_ACTION_FAILED,
    )
    private val fallback = RecordingPresenter()
    private var fallbacksCreated = 0
    private var completions = 0

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
    fun `a presenter that throws is replaced by the fallback for the same error`() {
        ErrorPresenter { _, _ -> error("Simulated.") }.presentWithFallback()

        assertThat(fallback.presented).containsExactly(params)
        assertThat(completions).isEqualTo(0)
        verify(exactly = 1) { Logger.e(match { it.startsWith("Error presenter failed") }) }

        fallback.complete(0)

        assertThat(completions).isEqualTo(1)
    }

    @Test
    fun `a completion made before the presenter throws is dropped`() {
        ErrorPresenter { _, completion ->
            completion.complete()
            error("Simulated.")
        }.presentWithFallback()

        assertThat(completions).isEqualTo(0)

        fallback.complete(0)

        assertThat(completions).isEqualTo(1)
    }

    @Test
    fun `a completion made after the presenter threw is ignored`() {
        var late: ErrorPresenter.Completion? = null
        ErrorPresenter { _, completion ->
            late = completion
            error("Simulated.")
        }.presentWithFallback()

        late!!.complete()

        assertThat(completions).isEqualTo(0)

        fallback.complete(0)

        assertThat(completions).isEqualTo(1)
    }

    @Test
    fun `a presenter that does not throw keeps its completion and never creates the fallback`() {
        var own: ErrorPresenter.Completion? = null
        ErrorPresenter { _, completion -> own = completion }.presentWithFallback()

        assertThat(fallbacksCreated).isEqualTo(0)
        assertThat(completions).isEqualTo(0)

        own!!.complete()
        own!!.complete()

        assertThat(completions).isEqualTo(1)
        assertThat(fallback.presented).isEmpty()
    }

    @Test
    fun `a presenter completing inside present completes once`() {
        ErrorPresenter { _, completion ->
            completion.complete()
            completion.complete()
        }.presentWithFallback()

        assertThat(completions).isEqualTo(1)
        assertThat(fallbacksCreated).isEqualTo(0)
    }

    @Test
    fun `a completion from another thread after present returned is delivered`() {
        var own: ErrorPresenter.Completion? = null
        ErrorPresenter { _, completion -> own = completion }.presentWithFallback()

        thread { own!!.complete() }.join()

        assertThat(completions).isEqualTo(1)
    }

    @Test
    fun `a fallback that throws propagates`() {
        val throwing = ErrorPresenter { _, _ -> error("Simulated.") }

        assertThatThrownBy { throwing.withFallback { throwing }.present(params) { completions++ } }
            .hasMessage("Simulated.")
        assertThat(completions).isEqualTo(0)
    }

    private fun ErrorPresenter.presentWithFallback() =
        withFallback {
            fallbacksCreated++
            fallback
        }.present(params) { completions++ }

    private class RecordingPresenter : ErrorPresenter {
        val presented = mutableListOf<ErrorPresenter.Params>()
        private val completions = mutableListOf<ErrorPresenter.Completion>()

        override fun present(params: ErrorPresenter.Params, completion: ErrorPresenter.Completion) {
            presented += params
            completions += completion
        }

        fun complete(index: Int) = completions[index].complete()
    }
}
