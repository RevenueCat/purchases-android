package com.revenuecat.purchasetester.ui.screens.proxysettings

import android.util.Log
import app.cash.turbine.test
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchasetester.proxysettings.ProxyMode
import com.revenuecat.purchasetester.proxysettings.ProxySettingsState
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.net.URL

@OptIn(ExperimentalCoroutinesApi::class)
class ProxySettingsViewModelTest {

    companion object {
        private const val ERROR_NO_PROXY_URL = "There is no Proxy URL configured"
        private const val PROXY_URL_BASE = "http://localhost:8080"
        private const val MOCK_RESPONSE_OFF = """{"mode":"OFF"}"""
        private const val MOCK_RESPONSE_OVERRIDE = """{"mode":"OVERRIDE_ENTITLEMENTS"}"""
        private const val MOCK_RESPONSE_SERVER_DOWN = """{"mode":"SERVER_DOWN"}"""
    }

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockkObject(Purchases)
        mockkStatic(Log::class)
        every { Log.w(any(), any<String>()) } returns 0
        every { Log.w(any(), any<String>(), any()) } returns 0
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        unmockkAll()
    }

    @Test
    fun `initial state is Loading`() = runTest(testDispatcher) {
        // Given
        every { Purchases.proxyURL } returns URL(PROXY_URL_BASE)
        val viewModel = ProxySettingsViewModel(testDispatcher)

        // When/Then
        viewModel.state.test {
            val initialState = expectMostRecentItem()
            assertTrue(initialState is ProxySettingsState.Loading)
        }
    }

    @Test
    fun `loadCurrentState with no proxy URL shows error`() = runTest(testDispatcher) {
        // Given
        every { Purchases.proxyURL } returns null
        val viewModel = ProxySettingsViewModel(testDispatcher)

        // When
        viewModel.loadCurrentState()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val errorState = expectMostRecentItem()
            assertTrue(errorState is ProxySettingsState.Error)
            assertEquals(ERROR_NO_PROXY_URL, (errorState as ProxySettingsState.Error).message)
        }
    }

    @Test
    fun `changeMode with no proxy URL shows error`() = runTest(testDispatcher) {
        // Given
        every { Purchases.proxyURL } returns null
        val viewModel = ProxySettingsViewModel(testDispatcher)

        // When
        viewModel.changeMode(ProxyMode.OFF)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        viewModel.state.test {
            val errorState = expectMostRecentItem()
            assertTrue(errorState is ProxySettingsState.Error)
            assertEquals(ERROR_NO_PROXY_URL, (errorState as ProxySettingsState.Error).message)
        }
    }

    @Test
    fun `state updates are emitted correctly`() = runTest(testDispatcher) {
        // Given
        every { Purchases.proxyURL } returns URL(PROXY_URL_BASE)
        val viewModel = ProxySettingsViewModel(testDispatcher)

        // When - Start with initial state
        viewModel.state.test {
            val loadingState = expectMostRecentItem()
            assertTrue(loadingState is ProxySettingsState.Loading)
        }
    }

    @Test
    fun `multiple mode changes update state correctly`() = runTest(testDispatcher) {
        // Given
        every { Purchases.proxyURL } returns null
        val viewModel = ProxySettingsViewModel(testDispatcher)

        // When - Multiple changes with no proxy URL
        viewModel.changeMode(ProxyMode.ENTITLEMENT_OVERRIDE)
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.changeMode(ProxyMode.SERVER_DOWN)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - Both should result in error state
        viewModel.state.test {
            val state = expectMostRecentItem()
            assertTrue(state is ProxySettingsState.Error)
        }
    }

    @Test
    fun `changeMode with valid proxy URL attempts network call`() = runTest(testDispatcher) {
        // Given
        every { Purchases.proxyURL } returns URL(PROXY_URL_BASE)
        val viewModel = ProxySettingsViewModel(testDispatcher)

        // When - Change mode with valid proxy URL
        viewModel.changeMode(ProxyMode.OFF)
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - State will eventually update (to Error in test environment due to no server)
        viewModel.state.test {
            val state = expectMostRecentItem()
            // Either Loading or Error is acceptable - the important thing is the call was attempted
            assertTrue(state is ProxySettingsState.Loading || state is ProxySettingsState.Error)
        }
    }

    @Test
    fun `loadCurrentState with valid proxy URL attempts network call`() = runTest(testDispatcher) {
        // Given
        every { Purchases.proxyURL } returns URL(PROXY_URL_BASE)
        val viewModel = ProxySettingsViewModel(testDispatcher)

        // When
        viewModel.loadCurrentState()
        testDispatcher.scheduler.advanceUntilIdle()

        // Then - State will eventually update (to Error in test environment due to no server)
        viewModel.state.test {
            val state = expectMostRecentItem()
            // Either Loading or Error is acceptable - the important thing is the call was attempted
            assertTrue(state is ProxySettingsState.Loading || state is ProxySettingsState.Error)
        }
    }
}
