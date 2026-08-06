package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.checkpoints.CheckpointPresenter
import com.revenuecat.purchases.checkpoints.CheckpointPresenterDelegate
import com.revenuecat.purchases.checkpoints.CheckpointWorkflowPresentation
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.ServiceLoader

@RunWith(AndroidJUnit4::class)
class CheckpointPresenterImplTest {

    private val callId = "test-call-id"

    private lateinit var mockActivity: Activity
    private lateinit var mockDelegate: CheckpointPresenterDelegate
    private lateinit var mockPresentation: CheckpointWorkflowPresentation
    private val startedIntent = slot<Intent>()

    private lateinit var presenter: CheckpointPresenterImpl

    @Before
    fun setup() {
        mockActivity = mockk {
            every { packageName } returns "com.revenuecat.test"
            every { startActivity(capture(startedIntent)) } just runs
        }
        mockDelegate = mockk()
        mockPresentation = mockk()
        presenter = CheckpointPresenterImpl()
    }

    @After
    fun tearDown() {
        CheckpointCallStore.clear()
    }

    @Test
    fun `present stores the delegate and presentation keyed by callId`() {
        presenter.present(mockActivity, callId, mockPresentation, mockDelegate)

        val entry = CheckpointCallStore.get(callId)
        assertThat(entry?.delegate).isSameAs(mockDelegate)
        assertThat(entry?.presentation).isSameAs(mockPresentation)
    }

    @Test
    fun `present starts the checkpoint workflow activity carrying only the callId`() {
        presenter.present(mockActivity, callId, mockPresentation, mockDelegate)

        val intent = startedIntent.captured
        assertThat(intent.component?.className).isEqualTo(CheckpointWorkflowActivity::class.java.name)
        assertThat(intent.getStringExtra(CheckpointWorkflowActivity.EXTRA_CALL_ID)).isEqualTo(callId)
    }

    @Test
    fun `presenter is discoverable through ServiceLoader`() {
        val loadedPresenter = ServiceLoader.load(
            CheckpointPresenter::class.java,
            CheckpointPresenter::class.java.classLoader,
        ).firstOrNull()

        assertThat(loadedPresenter).isInstanceOf(CheckpointPresenterImpl::class.java)
    }
}
