@file:OptIn(ExperimentalCoroutinesApi::class, InternalRevenueCatAPI::class, InviteOnlyCheckpointsAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.ui.revenuecatui.InviteOnlyCheckpointsAPI
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
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import kotlin.concurrent.thread

/**
 * Unit tests for [ErrorDialogFactory.asErrorPresenter]. The adapter is tested in isolation: [presenterFor] is a
 * recording lambda so no real Android UI is exercised. Integration with [CheckpointsManager] and the full
 * presenter precedence matrix are covered in [CheckpointsManagerFactoryTest].
 */
class ErrorDialogFactoryTest {

    private val scope = TestScope(UnconfinedTestDispatcher())

    // The dialogs passed to presenterFor for each present() call; null = SDK default dialog was requested.
    private val presenterDialogs = mutableListOf<CheckpointErrorDialog?>()

    // The presenters created by presenterFor, in order.
    private val presenters = mutableListOf<RecordingPresenter>()

    // Outer completion counter.
    private var outerCompletions = 0
    private val outerCompletion = ErrorPresenter.Completion { outerCompletions++ }

    private val params = ErrorPresenter.Params(
        PurchasesError(PurchasesErrorCode.StoreProblemError, "boom"),
        "test_checkpoint",
        emptyMap(),
        CheckpointFailureKind.FLOW_ACTION_FAILED,
    )

    @Before
    fun setup() {
        mockkObject(Logger)
        every { Logger.e(any()) } just runs
        every { Logger.w(any()) } just runs
        every { Logger.d(any()) } just runs
    }

    @After
    fun tearDown() {
        unmockkObject(Logger)
    }

    // region: content returned by factory

    @Test
    fun `factory providing a dialog routes it to the custom presenter`() {
        val dialog = CheckpointErrorDialog(message = "Custom error", buttonTitle = "Got it")
        val factory = ErrorDialogFactory { _, completion -> completion.complete(dialog) }

        factory.asPresenter().present(params, outerCompletion)

        assertThat(presenterDialogs).containsExactly(dialog)
    }

    @Test
    fun `completing the presenter after a factory-provided dialog completes the outer completion once`() {
        val dialog = CheckpointErrorDialog(message = "Custom", buttonTitle = "OK")
        val factory = ErrorDialogFactory { _, completion -> completion.complete(dialog) }

        factory.asPresenter().present(params, outerCompletion)
        presenters[0].complete()

        assertThat(outerCompletions).isEqualTo(1)
    }

    @Test
    fun `factory-provided Params carry all error params`() {
        var receivedParams: ErrorDialogFactory.Params? = null
        val factory = ErrorDialogFactory { p, completion ->
            receivedParams = p
            completion.complete(null)
        }

        factory.asPresenter().present(params, outerCompletion)

        assertThat(receivedParams!!.error).isEqualTo(params.error)
        assertThat(receivedParams!!.checkpointIdentifier).isEqualTo(params.checkpointIdentifier)
        assertThat(receivedParams!!.customVariables).isEqualTo(params.customVariables)
        assertThat(receivedParams!!.kind).isEqualTo(params.kind)
    }

    // endregion

    // region: null dialog → SDK default

    @Test
    fun `factory returning null requests the SDK default presenter`() {
        val factory = ErrorDialogFactory { _, completion -> completion.complete(null) }

        factory.asPresenter().present(params, outerCompletion)

        assertThat(presenterDialogs).containsExactly(null as CheckpointErrorDialog?)
    }

    @Test
    fun `completing the default presenter after null completes the outer completion once`() {
        val factory = ErrorDialogFactory { _, completion -> completion.complete(null) }

        factory.asPresenter().present(params, outerCompletion)
        presenters[0].complete()

        assertThat(outerCompletions).isEqualTo(1)
    }

    // endregion

    // region: factory throws

    @Test
    fun `a factory that throws falls back to the SDK default and logs a warning`() {
        val factory = ErrorDialogFactory { _, _ -> error("Simulated.") }

        factory.asPresenter().present(params, outerCompletion)

        assertThat(presenterDialogs).containsExactly(null as CheckpointErrorDialog?)
        verify(exactly = 1) { Logger.w(match { it.contains("factory threw") }) }
    }

    @Test
    fun `a late completion from a factory that threw is silently ignored`() {
        var lateCompletion: ErrorDialogFactory.Completion? = null
        val factory = ErrorDialogFactory { _, completion ->
            lateCompletion = completion
            error("Simulated.")
        }

        factory.asPresenter().present(params, outerCompletion)
        assertThat(presenterDialogs).hasSize(1) // default already chosen

        lateCompletion!!.complete(CheckpointErrorDialog("Late", "OK"))

        // No second presenter was created
        assertThat(presenterDialogs).hasSize(1)
    }

    @Test
    fun `a factory completing before throwing falls back without presenting its response`() {
        val dialog = CheckpointErrorDialog("Must not be shown", "OK")
        val factory = ErrorDialogFactory { _, completion ->
            completion.complete(dialog)
            error("Simulated.")
        }

        factory.asPresenter().present(params, outerCompletion)

        assertThat(presenterDialogs).containsExactly(null as CheckpointErrorDialog?)
        verify(exactly = 1) { Logger.w(match { it.contains("factory threw") }) }
    }

    @Test
    fun `a queued response is discarded when the factory throws and late responses are ignored`() {
        val queuedScope = TestScope(StandardTestDispatcher())
        var lateCompletion: ErrorDialogFactory.Completion? = null
        val factory = ErrorDialogFactory { _, completion ->
            lateCompletion = completion
            completion.complete(CheckpointErrorDialog("Must not be shown", "OK"))
            error("Simulated.")
        }

        factory.asPresenter(scope = queuedScope).present(params, outerCompletion)
        lateCompletion!!.complete(CheckpointErrorDialog("Late", "OK"))
        queuedScope.advanceUntilIdle()

        assertThat(presenterDialogs).containsExactly(null as CheckpointErrorDialog?)
        presenters.single().complete()
        assertThat(outerCompletions).isEqualTo(1)
    }

    // endregion

    // region: timeout

    @Test
    fun `factory not completing within the timeout falls back to SDK default`() {
        scope.run {
            val factory = ErrorDialogFactory { _, _ -> /* never completes */ }
            factory.asPresenter(timeoutMs = 100).present(params, outerCompletion)

            assertThat(presenterDialogs).isEmpty()

            advanceTimeBy(101)

            assertThat(presenterDialogs).containsExactly(null as CheckpointErrorDialog?)
            verify(exactly = 1) { Logger.w(match { it.contains("timed out") }) }
        }
    }

    @Test
    fun `a completion arriving after the timeout is silently ignored`() {
        scope.run {
            var lateCompletion: ErrorDialogFactory.Completion? = null
            val factory = ErrorDialogFactory { _, completion -> lateCompletion = completion }
            factory.asPresenter(timeoutMs = 100).present(params, outerCompletion)

            advanceTimeBy(101)
            assertThat(presenterDialogs).hasSize(1) // timeout fired

            lateCompletion!!.complete(CheckpointErrorDialog("Late", "OK"))

            assertThat(presenterDialogs).hasSize(1) // no second presenter
        }
    }

    @Test
    fun `a factory completing before the timeout cancels it`() {
        scope.run {
            val factory = ErrorDialogFactory { _, completion -> completion.complete(null) }

            factory.asPresenter(timeoutMs = 100).present(params, outerCompletion)

            advanceTimeBy(200)

            // Only one presenter (from the factory response, not from timeout)
            assertThat(presenterDialogs).hasSize(1)
            verify(exactly = 0) { Logger.w(match { it.contains("timed out") }) }
        }
    }

    // endregion

    // region: double completion

    @Test
    fun `only the first factory completion is used`() {
        var completion: ErrorDialogFactory.Completion? = null
        val factory = ErrorDialogFactory { _, c -> completion = c }

        factory.asPresenter().present(params, outerCompletion)
        completion!!.complete(CheckpointErrorDialog("First", "OK"))
        completion!!.complete(CheckpointErrorDialog("Second", "Ignored"))

        assertThat(presenterDialogs).containsExactly(CheckpointErrorDialog("First", "OK"))
    }

    @Test
    fun `only the first presenter completion reaches the outer completion`() {
        var factoryCompletion: ErrorDialogFactory.Completion? = null
        val factory = ErrorDialogFactory { _, c -> factoryCompletion = c }

        factory.asPresenter().present(params, outerCompletion)
        factoryCompletion!!.complete(null)
        presenters[0].complete()
        presenters[0].complete()

        assertThat(outerCompletions).isEqualTo(1)
    }

    // endregion

    // region: async completion from another thread

    @Test
    fun `a completion from another thread is delivered on the scope`() {
        val dispatcher = StandardTestDispatcher()
        val scopeWithDispatcher = TestScope(dispatcher)
        var factoryCompletion: ErrorDialogFactory.Completion? = null
        val factory = ErrorDialogFactory { _, c -> factoryCompletion = c }

        factory.asPresenter(scope = scopeWithDispatcher).present(params, outerCompletion)

        thread { factoryCompletion!!.complete(null) }.join()

        // Scope hasn't advanced yet; the scope.launch { present(...) } is pending
        assertThat(presenterDialogs).isEmpty()

        scopeWithDispatcher.advanceUntilIdle()

        assertThat(presenterDialogs).hasSize(1)
    }

    // endregion

    // region: several errors in order

    @Test
    fun `two sequential presentations through the adapter complete independently`() {
        val first = presentAndCaptureCompletion()
        val second = presentAndCaptureCompletion()

        assertThat(presenterDialogs).hasSize(2)
        assertThat(outerCompletions).isZero

        first.complete()
        assertThat(outerCompletions).isEqualTo(1)

        second.complete()
        assertThat(outerCompletions).isEqualTo(2)
    }

    // endregion

    // region: CheckpointParams wiring

    @Test
    fun `CheckpointParams carries the factory through the DSL builder`() {
        val factory = ErrorDialogFactory { _, _ -> }
        val params = CheckpointParams { errorDialogFactory(factory) }

        assertThat(params.errorDialogFactory).isSameAs(factory)
    }

    @Test
    fun `CheckpointParams factory can be set and cleared via the Java-style setter`() {
        val factory = ErrorDialogFactory { _, _ -> }
        val params = CheckpointParams.Builder().setErrorDialogFactory(factory).build()
        assertThat(params.errorDialogFactory).isSameAs(factory)

        val cleared = CheckpointParams.Builder().setErrorDialogFactory(null).build()
        assertThat(cleared.errorDialogFactory).isNull()
    }

    @Test
    fun `CheckpointParams with no factory has a null errorDialogFactory`() {
        val params = CheckpointParams { customVariables { "x" to "y" } }
        assertThat(params.errorDialogFactory).isNull()
    }

    @Test
    fun `CheckpointParams equals and hashCode include errorDialogFactory`() {
        val factory = ErrorDialogFactory { _, _ -> }
        val withFactory = CheckpointParams { errorDialogFactory(factory) }
        val withoutFactory = CheckpointParams {}

        assertThat(withFactory).isNotEqualTo(withoutFactory)
        assertThat(withFactory.hashCode()).isNotEqualTo(withoutFactory.hashCode())
    }

    @Test
    fun `two CheckpointParams with the same factory instance are equal`() {
        val factory = ErrorDialogFactory { _, _ -> }
        val a = CheckpointParams { errorDialogFactory(factory) }
        val b = CheckpointParams { errorDialogFactory(factory) }
        assertThat(a).isEqualTo(b)
    }

    // endregion

    // region: ErrorDialogFactory.Params

    @Test
    fun `ErrorDialogFactory Params equals and hashCode work for equal instances`() {
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "boom")
        val a = ErrorDialogFactory.Params(error, "cp", emptyMap(), CheckpointFailureKind.FLOW_ACTION_FAILED)
        val b = ErrorDialogFactory.Params(error, "cp", emptyMap(), CheckpointFailureKind.FLOW_ACTION_FAILED)
        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun `ErrorDialogFactory Params toString includes checkpoint identifier and kind`() {
        val error = PurchasesError(PurchasesErrorCode.StoreProblemError, "boom")
        val p = ErrorDialogFactory.Params(error, "my_cp", emptyMap(), CheckpointFailureKind.FLOW_RESULT_SYNC_FAILED)
        val str = p.toString()
        assertThat(str).contains("my_cp")
        assertThat(str).contains(CheckpointFailureKind.FLOW_RESULT_SYNC_FAILED.toString())
    }

    // endregion

    // region: CheckpointErrorDialog

    @Test
    fun `CheckpointErrorDialog equals and hashCode work`() {
        val a = CheckpointErrorDialog("Something went wrong.", "Dismiss")
        val b = CheckpointErrorDialog("Something went wrong.", "Dismiss")
        assertThat(a).isEqualTo(b)
        assertThat(a.hashCode()).isEqualTo(b.hashCode())
    }

    @Test
    fun `CheckpointErrorDialog with different message or buttonTitle are not equal`() {
        assertThat(CheckpointErrorDialog("msg", "OK")).isNotEqualTo(CheckpointErrorDialog("other", "OK"))
        assertThat(CheckpointErrorDialog("msg", "OK")).isNotEqualTo(CheckpointErrorDialog("msg", "Cancel"))
    }

    @Test
    fun `CheckpointErrorDialog toString contains message and buttonTitle`() {
        val dialog = CheckpointErrorDialog("Something went wrong.", "Dismiss")
        val str = dialog.toString()
        assertThat(str).contains("Something went wrong.")
        assertThat(str).contains("Dismiss")
    }

    // endregion

    // ---- helpers ----

    /**
     * Presents [params] through the factory and returns the completion handed to the presenter,
     * so callers can drive the acknowledgement and observe [outerCompletions].
     */
    private fun presentAndCaptureCompletion(): RecordingPresenter {
        val factory = ErrorDialogFactory { _, c -> c.complete(null) }
        factory.asPresenter().present(params, outerCompletion)
        return presenters.last()
    }

    /**
     * Wraps this factory in [asErrorPresenter] wired to the shared recording state
     * ([presenterDialogs], [presenters]).
     */
    private fun ErrorDialogFactory.asPresenter(
        scope: TestScope = this@ErrorDialogFactoryTest.scope,
        timeoutMs: Long = ErrorDialogFactory.TIMEOUT_MS,
    ): ErrorPresenter = asErrorPresenter(
        scope = scope,
        timeoutMs = timeoutMs,
    ) { dialog ->
        presenterDialogs += dialog
        RecordingPresenter().also { presenters += it }
    }

    /** An [ErrorPresenter] that records the completion handed to it and lets tests complete it on demand. */
    private class RecordingPresenter : ErrorPresenter {
        private var heldCompletion: ErrorPresenter.Completion? = null

        override fun present(params: ErrorPresenter.Params, completion: ErrorPresenter.Completion) {
            heldCompletion = completion
        }

        fun complete() {
            heldCompletion!!.complete()
        }
    }
}
