package com.revenuecat.purchases.ui.revenuecatui.activity

import android.app.Activity
import android.content.Intent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ui.revenuecatui.PaywallListener
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
class PaywallContractTest {

    private val contract = PaywallContract()

    @After
    fun tearDown() {
        PaywallActivityNonSerializableArgsStore.clear()
    }

    @Test
    @Config(sdk = [24, 33, 34])
    fun `parses result on every API level`() {
        val intent = Intent().putExtra(PaywallActivity.RESULT_EXTRA, PaywallResult.Cancelled)

        val result = contract.parseResult(Activity.RESULT_OK, intent)

        assertThat(result).isEqualTo(PaywallResult.Cancelled)
    }

    @Test
    @Config(sdk = [24, 33, 34])
    fun `returns error when result extra is another Parcelable type`() {
        val intent = Intent().putExtra(
            PaywallActivity.RESULT_EXTRA,
            PaywallActivityArgs(requiredEntitlementIdentifier = "pro"),
        )

        val result = contract.parseResult(Activity.RESULT_OK, intent)

        assertThat(result).isInstanceOf(PaywallResult.Error::class.java)
    }

    @Test
    fun `returns cancelled when result code is not OK`() {
        val intent = Intent().putExtra(PaywallActivity.RESULT_EXTRA, PaywallResult.Cancelled)

        val result = contract.parseResult(Activity.RESULT_CANCELED, intent)

        assertThat(result).isEqualTo(PaywallResult.Cancelled)
    }

    @Test
    fun `removes stored non serializable args referenced by the result`() {
        val key = PaywallActivityNonSerializableArgsStore.store(
            PaywallActivityNonSerializableArgs(listener = mockk<PaywallListener>()),
        )
        val otherArgs = PaywallActivityNonSerializableArgs(listener = mockk<PaywallListener>())
        val otherKey = PaywallActivityNonSerializableArgsStore.store(otherArgs)

        val result = contract.parseResult(
            Activity.RESULT_OK,
            PaywallActivity.createResultIntent(PaywallResult.Cancelled, key),
        )

        assertThat(result).isEqualTo(PaywallResult.Cancelled)
        assertThat(PaywallActivityNonSerializableArgsStore.get(key)).isNull()
        assertThat(PaywallActivityNonSerializableArgsStore.get(otherKey)).isSameAs(otherArgs)
    }

    @Test
    fun `removes stored non serializable args even when result code is not OK`() {
        val key = PaywallActivityNonSerializableArgsStore.store(
            PaywallActivityNonSerializableArgs(listener = mockk<PaywallListener>()),
        )

        val result = contract.parseResult(
            Activity.RESULT_CANCELED,
            PaywallActivity.createResultIntent(PaywallResult.Cancelled, key),
        )

        assertThat(result).isEqualTo(PaywallResult.Cancelled)
        assertThat(PaywallActivityNonSerializableArgsStore.get(key)).isNull()
    }
}
