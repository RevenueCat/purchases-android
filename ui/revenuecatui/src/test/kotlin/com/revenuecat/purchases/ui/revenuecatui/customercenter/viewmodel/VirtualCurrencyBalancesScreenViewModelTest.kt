package com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel

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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
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
    fun `loadData invalidates the virtual currencies cache`() = runTest {
        coEvery { mockPurchases.awaitGetVirtualCurrencies() } returns CustomerCenterConfigTestData.fourVirtualCurrencies

        viewModel.onViewAppeared()
        advanceUntilIdle()

        verify(exactly = 1) { mockPurchases.invalidateVirtualCurrenciesCache() }
        coVerify(exactly = 1) { mockPurchases.awaitGetVirtualCurrencies() }
    }

    @Test
    fun `loadData success with VCs sets the state to Loaded`() = runTest {
        coEvery { mockPurchases.awaitGetVirtualCurrencies() } returns CustomerCenterConfigTestData.fourVirtualCurrencies
        val expectedVCList = CustomerCenterConfigTestData.fourVirtualCurrencies.all.values.sortedByDescending { it.balance }

        viewModel.onViewAppeared()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockPurchases.awaitGetVirtualCurrencies() }

        val state = viewModel.viewState.value
        assertThat(state).isInstanceOf(VirtualCurrencyBalancesScreenViewState.Loaded::class.java)
        val loadedState = state as VirtualCurrencyBalancesScreenViewState.Loaded
        assertThat(loadedState.virtualCurrencyBalanceData).hasSize(4)
    }

    @Test
    fun `loadData success with no VCs sets the state to Loaded`() = runTest {
        val emptyVirtualCurrencies = createEmptyVirtualCurrencies()
        coEvery { mockPurchases.awaitGetVirtualCurrencies() } returns emptyVirtualCurrencies

        viewModel.onViewAppeared()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockPurchases.awaitGetVirtualCurrencies() }

        val state = viewModel.viewState.value
        assertThat(state).isInstanceOf(VirtualCurrencyBalancesScreenViewState.Loaded::class.java)
        val loadedState = state as VirtualCurrencyBalancesScreenViewState.Loaded
        assertThat(loadedState.virtualCurrencyBalanceData).isEmpty()
    }

    @Test
    fun `loadData failure sets error state`() = runTest {
        coEvery { mockPurchases.awaitGetVirtualCurrencies() } throws RuntimeException("An error occurred")

        viewModel.onViewAppeared()
        advanceUntilIdle()

        coVerify(exactly = 1) { mockPurchases.awaitGetVirtualCurrencies() }
        assertThat(viewModel.viewState.value).isEqualTo(VirtualCurrencyBalancesScreenViewState.Error)
    }

    @Test
    fun `virtual currencies are sorted by balance descending`() = runTest {
        coEvery { mockPurchases.awaitGetVirtualCurrencies() } returns CustomerCenterConfigTestData.fourVirtualCurrencies

        viewModel.onViewAppeared()
        advanceUntilIdle()

        val state = viewModel.viewState.value
        assertThat(state).isInstanceOf(VirtualCurrencyBalancesScreenViewState.Loaded::class.java)
        val loadedState = state as VirtualCurrencyBalancesScreenViewState.Loaded
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
    fun `onViewAppeared sets loading state initially`() = runTest {
        coEvery { mockPurchases.awaitGetVirtualCurrencies() } returns CustomerCenterConfigTestData.fourVirtualCurrencies

        assertThat(viewModel.viewState.value).isEqualTo(VirtualCurrencyBalancesScreenViewState.Loading)

        viewModel.onViewAppeared()

        assertThat(viewModel.viewState.value).isEqualTo(VirtualCurrencyBalancesScreenViewState.Loading)
    }

    @Test
    fun `multiple calls to onViewAppeared work correctly`() = runTest {
        coEvery { mockPurchases.awaitGetVirtualCurrencies() } returns CustomerCenterConfigTestData.fourVirtualCurrencies

        viewModel.onViewAppeared()
        advanceUntilIdle()
        viewModel.onViewAppeared()
        advanceUntilIdle()

        verify(exactly = 2) { mockPurchases.invalidateVirtualCurrenciesCache() }
        coVerify(exactly = 2) { mockPurchases.awaitGetVirtualCurrencies() }
    }

    private fun createEmptyVirtualCurrencies(): VirtualCurrencies {
        return mockk<VirtualCurrencies>().apply {
            every { all } returns emptyMap()
        }
    }
}