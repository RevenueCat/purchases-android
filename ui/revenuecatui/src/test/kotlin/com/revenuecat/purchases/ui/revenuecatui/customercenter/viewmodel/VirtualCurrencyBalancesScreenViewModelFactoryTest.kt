package com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel

import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test

class VirtualCurrencyBalancesScreenViewModelFactoryTest {

    private val mockPurchases = mockk<PurchasesType>()
    private val factory = VirtualCurrencyBalancesScreenViewModelFactory(mockPurchases)

    @Test
    fun `create returns new instance each time`() {
        val viewModel1 = factory.create(VirtualCurrencyBalancesScreenViewModel::class.java)
        val viewModel2 = factory.create(VirtualCurrencyBalancesScreenViewModel::class.java)

        assertThat(viewModel1).isInstanceOf(VirtualCurrencyBalancesScreenViewModel::class.java)
        assertThat(viewModel2).isInstanceOf(VirtualCurrencyBalancesScreenViewModel::class.java)
        assertThat(viewModel1).isNotSameAs(viewModel2)
    }
}
