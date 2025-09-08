package com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel

import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.PurchasesException
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterConfigTestData
import com.revenuecat.purchases.ui.revenuecatui.customercenter.views.VirtualCurrencyBalancesScreenViewState
import com.revenuecat.purchases.ui.revenuecatui.data.PurchasesType
import com.revenuecat.purchases.virtualcurrencies.VirtualCurrencies
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class VirtualCurrencyBalancesScreenViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mockPurchases: PurchasesType
    private lateinit var viewModel: VirtualCurrencyBalancesScreenViewModel

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockPurchases = mockk(relaxed = true)
        viewModel = VirtualCurrencyBalancesScreenViewModel(mockPurchases)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is loading`() {
        assertThat(viewModel.viewState.value).isEqualTo(VirtualCurrencyBalancesScreenViewState.Loading)
    }

    @Test
    fun `flow invalidates the virtual currencies cache when collected`() = runTest {
        coEvery { mockPurchases.awaitGetVirtualCurrencies() } returns CustomerCenterConfigTestData.fourVirtualCurrencies

        viewModel.viewState.take(2).toList()
        
        verify(exactly = 1) { mockPurchases.invalidateVirtualCurrenciesCache() }
    }

    @Test
    fun `flow success with VCs sets the state to Loaded`() = runTest {
        coEvery { mockPurchases.awaitGetVirtualCurrencies() } returns CustomerCenterConfigTestData.fourVirtualCurrencies
        
        val states = viewModel.viewState.take(2).toList()

        coVerify(exactly = 1) { mockPurchases.awaitGetVirtualCurrencies() }

        val finalState = states.last()
        assertThat(finalState).isInstanceOf(VirtualCurrencyBalancesScreenViewState.Loaded::class.java)
        val loadedState = finalState as VirtualCurrencyBalancesScreenViewState.Loaded
        assertThat(loadedState.virtualCurrencyBalanceData).hasSize(4)
    }

    @Test
    fun `flow success with no VCs sets the state to Loaded`() = runTest {
        val emptyVirtualCurrencies = createEmptyVirtualCurrencies()
        coEvery { mockPurchases.awaitGetVirtualCurrencies() } returns emptyVirtualCurrencies

        val states = viewModel.viewState.take(2).toList()

        coVerify(exactly = 1) { mockPurchases.awaitGetVirtualCurrencies() }

        val finalState = states.last()
        assertThat(finalState).isInstanceOf(VirtualCurrencyBalancesScreenViewState.Loaded::class.java)
        val loadedState = finalState as VirtualCurrencyBalancesScreenViewState.Loaded
        assertThat(loadedState.virtualCurrencyBalanceData).isEmpty()
    }

    @Test
    fun `flow failure sets error state`() = runTest {
        val error = PurchasesError(code = PurchasesErrorCode.UnknownError, underlyingErrorMessage = "Test error")
        coEvery { mockPurchases.awaitGetVirtualCurrencies() } throws PurchasesException(error)

        val states = viewModel.viewState.take(2).toList()

        coVerify(exactly = 1) { mockPurchases.awaitGetVirtualCurrencies() }
        
        val finalState = states.last()
        assertThat(finalState).isEqualTo(VirtualCurrencyBalancesScreenViewState.Error(error = error))
    }

    @Test
    fun `virtual currencies are sorted by balance descending`() = runTest {
        coEvery { mockPurchases.awaitGetVirtualCurrencies() } returns CustomerCenterConfigTestData.fourVirtualCurrencies

        val states = viewModel.viewState.take(2).toList()

        val finalState = states.last()
        assertThat(finalState).isInstanceOf(VirtualCurrencyBalancesScreenViewState.Loaded::class.java)
        val loadedState = finalState as VirtualCurrencyBalancesScreenViewState.Loaded
        val data = loadedState.virtualCurrencyBalanceData

        assertThat(data).hasSize(4)
        assertThat(data[0].balance).isEqualTo(400)
        assertThat(data[0].code).isEqualTo("PLTNM")
        assertThat(data[1].balance).isEqualTo(300)
        assertThat(data[1].code).isEqualTo("BRNZ")
        assertThat(data[2].balance).isEqualTo(200)
        assertThat(data[2].code).isEqualTo("SLV")
        assertThat(data[3].balance).isEqualTo(100)
        assertThat(data[3].code).isEqualTo("GLD")
    }

    @Test
    fun `flow does not execute until collected`() = runTest {
        coEvery { mockPurchases.awaitGetVirtualCurrencies() } returns CustomerCenterConfigTestData.fourVirtualCurrencies
        
        // Initializing viewModel shouldn't trigger VC loading
        val newViewModel = VirtualCurrencyBalancesScreenViewModel(mockPurchases)
        coVerify(exactly = 0) { mockPurchases.awaitGetVirtualCurrencies() }
        coVerify(exactly = 0) { mockPurchases.invalidateVirtualCurrenciesCache() }
        
        val states = newViewModel.viewState.take(2).toList()
        
        coVerify(exactly = 1) { mockPurchases.awaitGetVirtualCurrencies() }
        verify(exactly = 1) { mockPurchases.invalidateVirtualCurrenciesCache() }
        
        // Should have Loading -> Loaded states
        assertThat(states).hasSize(2)
        assertThat(states[0]).isEqualTo(VirtualCurrencyBalancesScreenViewState.Loading)
        assertThat(states[1]).isInstanceOf(VirtualCurrencyBalancesScreenViewState.Loaded::class.java)
    }

    @Test  
    fun `multiple collectors share the same flow execution`() = runTest {
        coEvery { mockPurchases.awaitGetVirtualCurrencies() } returns CustomerCenterConfigTestData.fourVirtualCurrencies
        
        // Start two collectors simultaneously
        val states1 = async { viewModel.viewState.take(2).toList() }
        val states2 = async { viewModel.viewState.take(2).toList() }
        
        val result1 = states1.await()
        val result2 = states2.await()
        
        // Should only call once, not twice
        coVerify(exactly = 1) { mockPurchases.awaitGetVirtualCurrencies() }
        verify(exactly = 1) { mockPurchases.invalidateVirtualCurrenciesCache() }
        
        assertThat(result1).isEqualTo(result2)
        assertThat(result1).hasSize(2)
        assertThat(result1[0]).isEqualTo(VirtualCurrencyBalancesScreenViewState.Loading)
        assertThat(result1[1]).isInstanceOf(VirtualCurrencyBalancesScreenViewState.Loaded::class.java)
    }

    @Test
    fun `flow caches result and does not restart immediately after completion`() = runTest {
        coEvery { mockPurchases.awaitGetVirtualCurrencies() } returns CustomerCenterConfigTestData.fourVirtualCurrencies
        
        val states1 = viewModel.viewState.take(2).toList()
        coVerify(exactly = 1) { mockPurchases.awaitGetVirtualCurrencies() }
        assertThat(states1.last()).isInstanceOf(VirtualCurrencyBalancesScreenViewState.Loaded::class.java)
        
        coEvery { mockPurchases.awaitGetVirtualCurrencies() } returns createEmptyVirtualCurrencies()
        
        // Second collection - should get cached result
        val currentState = viewModel.viewState.value
        
        assertThat(currentState).isInstanceOf(VirtualCurrencyBalancesScreenViewState.Loaded::class.java)
        val loadedState = currentState as VirtualCurrencyBalancesScreenViewState.Loaded
        
        assertThat(loadedState.virtualCurrencyBalanceData).hasSize(4)

        coVerify(exactly = 1) { mockPurchases.invalidateVirtualCurrenciesCache() }
        coVerify(exactly = 1) { mockPurchases.awaitGetVirtualCurrencies() }
    }

    private fun createEmptyVirtualCurrencies(): VirtualCurrencies {
        return mockk<VirtualCurrencies>().apply {
            every { all } returns emptyMap()
        }
    }
}