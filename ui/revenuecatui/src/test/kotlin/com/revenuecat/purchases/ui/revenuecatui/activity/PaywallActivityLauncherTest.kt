package com.revenuecat.purchases.ui.revenuecatui.activity

import android.app.Activity
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.core.content.IntentCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import com.revenuecat.purchases.ui.revenuecatui.ExperimentalPreviewRevenueCatUIPurchasesAPI
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PaywallActivityLauncherTest {

    @After
    fun tearDown() {
        PaywallActivityNonSerializableArgsStore.clear()
        ListenerHostActivity.receivedResults.clear()
    }

    @Test
    fun `launch drops custom variable keys that cannot be addressed as a custom variable`() {
        val activityResultLauncher = mockk<ActivityResultLauncher<PaywallActivityArgs>>()
        val resultCaller = mockk<ActivityResultCaller> {
            every {
                registerForActivityResult(
                    any<ActivityResultContract<PaywallActivityArgs, PaywallResult>>(),
                    any<ActivityResultCallback<PaywallResult>>(),
                )
            } returns activityResultLauncher
        }
        val args = slot<PaywallActivityArgs>()
        every { activityResultLauncher.launch(capture(args)) } just runs
        val launcher = PaywallActivityLauncher(resultCaller, resultHandler = mockk())

        launcher.launch(
            customVariables = mapOf(
                "my_property" to CustomVariableValue.String("kept"),
                "my.property" to CustomVariableValue.String("dropped"),
                "has space" to CustomVariableValue.String("dropped"),
            ),
        )

        assertThat(args.captured.customVariables)
            .isEqualTo(mapOf("my_property" to CustomVariableValue.String("kept")))
    }

    @Test
    fun `stored listener is removed when result arrives after the host activity was recreated`() {
        val controller = Robolectric.buildActivity(ListenerHostActivity::class.java).setup()
        val launchedPaywall = shadowOf(controller.get()).nextStartedActivityForResult
        val launchedArgs = IntentCompat.getParcelableExtra(
            launchedPaywall.intent,
            PaywallActivity.ARGS_EXTRA,
            PaywallActivityArgs::class.java,
        )
        val key = requireNotNull(launchedArgs?.nonSerializableArgsKey)
        assertThat(PaywallActivityNonSerializableArgsStore.get(key)?.listener)
            .isSameAs(ListenerHostActivity.listener)

        controller.recreate()
        val dispatched = controller.get().activityResultRegistry.dispatchResult(
            launchedPaywall.requestCode,
            Activity.RESULT_OK,
            PaywallActivity.createResultIntent(PaywallResult.Cancelled, key),
        )

        assertThat(dispatched).isTrue()
        assertThat(ListenerHostActivity.receivedResults).containsExactly(PaywallResult.Cancelled)
        assertThat(PaywallActivityNonSerializableArgsStore.get(key)).isNull()
    }
}

internal class ListenerHostActivity : ComponentActivity() {
    companion object {
        val listener: PaywallListener = object : PaywallListener {}
        val receivedResults = mutableListOf<PaywallResult>()
    }

    @OptIn(ExperimentalPreviewRevenueCatUIPurchasesAPI::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val launcher = PaywallActivityLauncher(
            this,
            object : PaywallResultHandler {
                override fun onActivityResult(result: PaywallResult) {
                    receivedResults.add(result)
                }
            },
        )
        if (savedInstanceState == null) {
            launcher.launchWithOptions(
                PaywallActivityLaunchOptions.Builder().setListener(listener).build(),
            )
        }
    }
}
