package com.revenuecat.purchases.ui.revenuecatui.checkpoints

import android.app.Activity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.ui.revenuecatui.PaywallPurchaseLogic
import com.revenuecat.purchases.ui.revenuecatui.PaywallPurchaseLogicParams
import com.revenuecat.purchases.ui.revenuecatui.PurchaseLogicResult
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ObservedPurchaseLogicTest {

    private val activity = mockk<Activity>()
    private val params = mockk<PaywallPurchaseLogicParams>()
    private val customerInfo = mockk<CustomerInfo>()
    private val delegate = mockk<PaywallPurchaseLogic>()
    private var completions = 0
    private val logic = ObservedPurchaseLogic(delegate) { completions++ }

    @Test
    fun `a successful purchase is forwarded and reported`() = runTest {
        coEvery { delegate.performPurchase(activity, params) } returns PurchaseLogicResult.Success

        val result = logic.performPurchase(activity, params)

        assertThat(result).isEqualTo(PurchaseLogicResult.Success)
        assertThat(completions).isEqualTo(1)
    }

    @Test
    fun `a successful restore is forwarded and reported`() = runTest {
        coEvery { delegate.performRestore(customerInfo) } returns PurchaseLogicResult.Success

        val result = logic.performRestore(customerInfo)

        assertThat(result).isEqualTo(PurchaseLogicResult.Success)
        assertThat(completions).isEqualTo(1)
    }

    @Test
    fun `a cancelled or failed purchase is forwarded but not reported`() = runTest {
        val error = PurchaseLogicResult.Error(PurchasesError(PurchasesErrorCode.StoreProblemError))
        coEvery { delegate.performPurchase(activity, params) } returnsMany listOf(PurchaseLogicResult.Cancellation, error)

        assertThat(logic.performPurchase(activity, params)).isEqualTo(PurchaseLogicResult.Cancellation)
        assertThat(logic.performPurchase(activity, params)).isEqualTo(error)

        assertThat(completions).isZero
    }

    @Test
    fun `a cancelled or failed restore is forwarded but not reported`() = runTest {
        val error = PurchaseLogicResult.Error(PurchasesError(PurchasesErrorCode.StoreProblemError))
        coEvery { delegate.performRestore(customerInfo) } returnsMany listOf(PurchaseLogicResult.Cancellation, error)

        assertThat(logic.performRestore(customerInfo)).isEqualTo(PurchaseLogicResult.Cancellation)
        assertThat(logic.performRestore(customerInfo)).isEqualTo(error)

        assertThat(completions).isZero
    }
}
