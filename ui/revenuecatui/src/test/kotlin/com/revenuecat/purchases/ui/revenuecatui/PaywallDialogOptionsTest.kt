package com.revenuecat.purchases.ui.revenuecatui

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class PaywallDialogOptionsTest {

    @Test
    fun `setCustomVariables drops keys that cannot be addressed as a custom variable`() {
        val options = PaywallDialogOptions.Builder()
            .setCustomVariables(
                mapOf(
                    "my_property" to CustomVariableValue.String("kept"),
                    "my.property" to CustomVariableValue.String("dropped"),
                    "has space" to CustomVariableValue.String("dropped"),
                ),
            )
            .build()

        assertThat(options.customVariables)
            .isEqualTo(mapOf("my_property" to CustomVariableValue.String("kept")))
    }
}
