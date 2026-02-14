package com.revenuecat.purchasetester.ui.screens.configure

import app.cash.turbine.test
import com.revenuecat.purchases.EntitlementVerificationMode
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchasetester.DataStoreUtils
import com.revenuecat.purchasetester.MainApplication
import com.revenuecat.purchasetester.SdkConfiguration
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ConfigureScreenViewModelTest {

    companion object {
        // Test API Keys
        private const val TEST_API_KEY = "test_api_key"
        private const val TEST_KEY = "test_key"
        private const val NEW_API_KEY = "new_api_key"
        private const val INITIAL_KEY = "initial_key"
        private const val KEY = "key"
        private const val KEY1 = "key1"
        private const val INITIAL = "initial"

        // Test Proxy URLs
        private const val PROXY_URL_EXAMPLE = "https://proxy.example.com"
        private const val PROXY_URL_NEW = "https://new-proxy.com"
        private const val PROXY_URL_1 = "https://proxy1.com"
        private const val PROXY_URL_INVALID = "not-a-valid-url"
        private const val PROXY_URL_OLD = "old"

        // Empty/Whitespace Values
        private const val EMPTY_STRING = ""
        private const val WHITESPACE_ONLY = "   "

        // Validation Messages
        private const val ERROR_API_KEY_EMPTY = "API Key cannot be empty"
        private const val ERROR_INVALID_PROXY_URL = "Invalid proxy URL format"

        // Error Messages
        private const val ERROR_CONFIGURATION_STATE_NOT_READY = "Configuration state not ready"
        private const val ERROR_SDK_CONFIGURATION_FAILED_PREFIX = "SDK configuration failed: "
        private const val ERROR_TEST_ERROR = "Test error"

        // Test Configuration Values
        private const val SKIP_ITEMS_COUNT = 1
    }

    private lateinit var dataStoreUtils: DataStoreUtils
    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        dataStoreUtils = mockk(relaxed = true)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state loads data from DataStore`() = runTest {
        // Given
        coEvery { dataStoreUtils.getSdkConfig() } returns flowOf(
            SdkConfiguration(
                apiKey = TEST_API_KEY,
                proxyUrl = PROXY_URL_EXAMPLE,
                useAmazon = false
            )
        )

        val viewModel = ConfigureScreenViewModelImpl(dataStoreUtils)

        // When
        viewModel.state.test {
            val dataState = awaitItem()

            // Then
            assertTrue(dataState is ConfigureScreenState.ConfigureScreenData)

            val data = dataState as ConfigureScreenState.ConfigureScreenData
            assertEquals(TEST_API_KEY, data.apiKey)
            assertEquals(PROXY_URL_EXAMPLE, data.proxyUrl)
            assertEquals(StoreType.GOOGLE, data.selectedStoreType)
            assertEquals(EntitlementVerificationMode.INFORMATIONAL, data.entitlementVerificationMode)
            assertEquals(PurchasesAreCompletedBy.REVENUECAT, data.purchasesAreCompletedBy)
        }
    }

    @Test
    fun `initial state loads Amazon store when useAmazon is true`() = runTest {
        // Given
        coEvery { dataStoreUtils.getSdkConfig() } returns flowOf(
            SdkConfiguration(
                apiKey = TEST_KEY,
                proxyUrl = null,
                useAmazon = true
            )
        )

        val viewModel = ConfigureScreenViewModelImpl(dataStoreUtils)

        // When
        viewModel.state.test {
            val dataState = awaitItem()

            // Then
            assertTrue(dataState is ConfigureScreenState.ConfigureScreenData)

            val data = dataState as ConfigureScreenState.ConfigureScreenData
            assertEquals(StoreType.AMAZON, data.selectedStoreType)
        }
    }

    @Test
    fun `initial state handles empty proxyUrl from DataStore`() = runTest {
        // Given
        coEvery { dataStoreUtils.getSdkConfig() } returns flowOf(
            SdkConfiguration(
                apiKey = TEST_KEY,
                proxyUrl = null,
                useAmazon = false
            )
        )

        val viewModel = ConfigureScreenViewModelImpl(dataStoreUtils)

        // When
        viewModel.state.test {
            val dataState = awaitItem() as ConfigureScreenState.ConfigureScreenData

            // Then
            assertEquals(EMPTY_STRING, dataState.proxyUrl)
        }
    }

    @Test
    fun `validateInputs returns Invalid when apiKey is blank`() = runTest {
        // Given
        coEvery { dataStoreUtils.getSdkConfig() } returns flowOf(
            SdkConfiguration(apiKey = EMPTY_STRING, proxyUrl = null, useAmazon = false)
        )
        val viewModel = ConfigureScreenViewModelImpl(dataStoreUtils)

        // When
        val result = viewModel.validateInputs(EMPTY_STRING, PROXY_URL_EXAMPLE)

        // Then
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(ERROR_API_KEY_EMPTY, (result as ValidationResult.Invalid).message)
    }

    @Test
    fun `validateInputs returns Invalid when apiKey is whitespace only`() = runTest {
        // Given
        coEvery { dataStoreUtils.getSdkConfig() } returns flowOf(
            SdkConfiguration(apiKey = EMPTY_STRING, proxyUrl = null, useAmazon = false)
        )
        val viewModel = ConfigureScreenViewModelImpl(dataStoreUtils)

        // When
        val result = viewModel.validateInputs(WHITESPACE_ONLY, PROXY_URL_EXAMPLE)

        // Then
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(ERROR_API_KEY_EMPTY, (result as ValidationResult.Invalid).message)
    }

    @Test
    fun `validateInputs returns Valid when apiKey is provided and proxyUrl is empty`() = runTest {
        // Given
        coEvery { dataStoreUtils.getSdkConfig() } returns flowOf(
            SdkConfiguration(apiKey = EMPTY_STRING, proxyUrl = null, useAmazon = false)
        )
        val viewModel = ConfigureScreenViewModelImpl(dataStoreUtils)

        // When
        val result = viewModel.validateInputs(TEST_KEY, EMPTY_STRING)

        // Then
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `validateInputs returns Invalid when proxyUrl is malformed`() = runTest {
        // Given
        coEvery { dataStoreUtils.getSdkConfig() } returns flowOf(
            SdkConfiguration(apiKey = EMPTY_STRING, proxyUrl = null, useAmazon = false)
        )
        val viewModel = ConfigureScreenViewModelImpl(dataStoreUtils)

        // When
        val result = viewModel.validateInputs(TEST_KEY, PROXY_URL_INVALID)

        // Then
        assertTrue(result is ValidationResult.Invalid)
        assertEquals(ERROR_INVALID_PROXY_URL, (result as ValidationResult.Invalid).message)
    }

    @Test
    fun `validateInputs returns Valid when both apiKey and proxyUrl are valid`() = runTest {
        // Given
        coEvery { dataStoreUtils.getSdkConfig() } returns flowOf(
            SdkConfiguration(apiKey = EMPTY_STRING, proxyUrl = null, useAmazon = false)
        )
        val viewModel = ConfigureScreenViewModelImpl(dataStoreUtils)

        // When
        val result = viewModel.validateInputs(TEST_KEY, PROXY_URL_EXAMPLE)

        // Then
        assertTrue(result is ValidationResult.Valid)
    }

    @Test
    fun `saveApiKey updates state with trimmed value`() = runTest {
        // Given
        coEvery { dataStoreUtils.getSdkConfig() } returns flowOf(
            SdkConfiguration(apiKey = INITIAL_KEY, proxyUrl = null, useAmazon = false)
        )
        val viewModel = ConfigureScreenViewModelImpl(dataStoreUtils)

        // When
        viewModel.state.test {
            skipItems(SKIP_ITEMS_COUNT)

            viewModel.saveApiKey("  $NEW_API_KEY  ")

            val updatedState = awaitItem() as ConfigureScreenState.ConfigureScreenData

            // Then
            assertEquals(NEW_API_KEY, updatedState.apiKey)
        }
    }

    @Test
    fun `saveProxyUrl updates state with trimmed value`() = runTest {
        // Given
        coEvery { dataStoreUtils.getSdkConfig() } returns flowOf(
            SdkConfiguration(apiKey = KEY, proxyUrl = PROXY_URL_OLD, useAmazon = false)
        )
        val viewModel = ConfigureScreenViewModelImpl(dataStoreUtils)

        // When
        viewModel.state.test {
            skipItems(SKIP_ITEMS_COUNT)

            viewModel.saveProxyUrl("  $PROXY_URL_NEW  ")

            val updatedState = awaitItem() as ConfigureScreenState.ConfigureScreenData

            // Then
            assertEquals(PROXY_URL_NEW, updatedState.proxyUrl)
        }
    }

    @Test
    fun `saveEntitlementVerificationMode updates state`() = runTest {
        // Given
        coEvery { dataStoreUtils.getSdkConfig() } returns flowOf(
            SdkConfiguration(apiKey = KEY, proxyUrl = null, useAmazon = false)
        )
        val viewModel = ConfigureScreenViewModelImpl(dataStoreUtils)

        // When
        viewModel.state.test {
            skipItems(SKIP_ITEMS_COUNT)

            viewModel.saveEntitlementVerificationMode(EntitlementVerificationMode.DISABLED)

            val updatedState = awaitItem() as ConfigureScreenState.ConfigureScreenData

            // Then
            assertEquals(EntitlementVerificationMode.DISABLED, updatedState.entitlementVerificationMode)
        }
    }

    @Test
    fun `saveStoreType updates state`() = runTest {
        // Given
        coEvery { dataStoreUtils.getSdkConfig() } returns flowOf(
            SdkConfiguration(apiKey = KEY, proxyUrl = null, useAmazon = false)
        )
        val viewModel = ConfigureScreenViewModelImpl(dataStoreUtils)

        // When
        viewModel.state.test {
            skipItems(SKIP_ITEMS_COUNT)

            viewModel.saveStoreType(StoreType.AMAZON)

            val updatedState = awaitItem() as ConfigureScreenState.ConfigureScreenData

            // Then
            assertEquals(StoreType.AMAZON, updatedState.selectedStoreType)
        }
    }

    @Test
    fun `savePurchasesAreCompletedBy updates state`() = runTest {
        // Given
        coEvery { dataStoreUtils.getSdkConfig() } returns flowOf(
            SdkConfiguration(apiKey = KEY, proxyUrl = null, useAmazon = false)
        )
        val viewModel = ConfigureScreenViewModelImpl(dataStoreUtils)

        // When
        viewModel.state.test {
            skipItems(SKIP_ITEMS_COUNT)

            viewModel.savePurchasesAreCompletedBy(PurchasesAreCompletedBy.MY_APP)

            val updatedState = awaitItem() as ConfigureScreenState.ConfigureScreenData

            // Then
            assertEquals(PurchasesAreCompletedBy.MY_APP, updatedState.purchasesAreCompletedBy)
        }
    }

    @Test
    fun `configureSDK emits error when state is Loading`() = runTest {
        // Given
        coEvery { dataStoreUtils.getSdkConfig() } returns flowOf()
        val viewModel = ConfigureScreenViewModelImpl(dataStoreUtils)
        val mockApp = mockk<MainApplication>(relaxed = true)

        // When
        viewModel.events.test {
            viewModel.configureSDK(mockApp)

            val event = awaitItem()

            // Then
            assertTrue(event is ConfigureUiEvent.Error)
            assertEquals(ERROR_CONFIGURATION_STATE_NOT_READY, (event as ConfigureUiEvent.Error).message)
        }
    }

    @Test
    fun `multiple state updates work correctly`() = runTest {
        // Given
        coEvery { dataStoreUtils.getSdkConfig() } returns flowOf(
            SdkConfiguration(apiKey = INITIAL, proxyUrl = null, useAmazon = false)
        )
        val viewModel = ConfigureScreenViewModelImpl(dataStoreUtils)

        // When
        viewModel.state.test {
            skipItems(SKIP_ITEMS_COUNT)

            viewModel.saveApiKey(KEY1)
            val state1 = awaitItem() as ConfigureScreenState.ConfigureScreenData

            viewModel.saveProxyUrl(PROXY_URL_1)
            val state2 = awaitItem() as ConfigureScreenState.ConfigureScreenData

            viewModel.saveStoreType(StoreType.AMAZON)
            val state3 = awaitItem() as ConfigureScreenState.ConfigureScreenData

            // Then
            assertEquals(KEY1, state1.apiKey)
            assertEquals(PROXY_URL_1, state2.proxyUrl)
            assertEquals(KEY1, state2.apiKey)
            assertEquals(StoreType.AMAZON, state3.selectedStoreType)
            assertEquals(PROXY_URL_1, state3.proxyUrl)
            assertEquals(KEY1, state3.apiKey)
        }
    }

    @Test
    fun `configureSDK handles empty proxy URL correctly`() = runTest {
        // Given
        coEvery { dataStoreUtils.getSdkConfig() } returns flowOf(
            SdkConfiguration(apiKey = KEY, proxyUrl = EMPTY_STRING, useAmazon = false)
        )
        val viewModel = ConfigureScreenViewModelImpl(dataStoreUtils)
        val mockApp = mockk<MainApplication>(relaxed = true)
        every { mockApp.logHandler } returns mockk(relaxed = true)

        coEvery { dataStoreUtils.saveSdkConfig(any()) } returns Unit

        // When
        viewModel.events.test {
            viewModel.configureSDK(mockApp)

            val event = awaitItem()

            // Then
            assertTrue(event is ConfigureUiEvent.Error)
        }
    }

    @Test
    fun `validateInputs handles whitespace in proxyUrl`() = runTest {
        // Given
        coEvery { dataStoreUtils.getSdkConfig() } returns flowOf(
            SdkConfiguration(apiKey = KEY, proxyUrl = null, useAmazon = false)
        )
        val viewModel = ConfigureScreenViewModelImpl(dataStoreUtils)

        // When
        val result = viewModel.validateInputs(TEST_KEY, WHITESPACE_ONLY)

        // Then
        assertTrue(result is ValidationResult.Valid)
    }
}
