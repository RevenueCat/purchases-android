package com.revenuecat.purchases.ui.revenuecatui.activity

import androidx.activity.result.ActivityResultCallback
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ui.revenuecatui.CustomVariableValue
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PaywallActivityLauncherTest {

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
}
