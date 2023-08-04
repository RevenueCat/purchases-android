package com.revenuecat.purchases.debugview

import android.app.Activity
import app.cash.paparazzi.Paparazzi
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.debugview.models.SettingGroupState
import com.revenuecat.purchases.debugview.models.SettingScreenState
import com.revenuecat.purchases.debugview.models.SettingState
import com.revenuecat.purchases.debugview.models.testOffering
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Before
import org.junit.Rule
import org.junit.Test

@Suppress("TooManyFunctions")
class DebugViewSnapshotTest {

    @get:Rule
    val paparazzi = Paparazzi()

    private lateinit var notConfiguredStateFlow: MutableStateFlow<SettingScreenState>
    private lateinit var notConfiguredViewModel: DebugRevenueCatViewModel
    private lateinit var configuredStateFlow: MutableStateFlow<SettingScreenState>
    private lateinit var configuredViewModel: DebugRevenueCatViewModel

    private var toastDisplayedCallCount = 0
    private var purchasePackageCallCount = 0
    private var purchaseProductCallCount = 0
    private var purchaseOptionCallCount = 0

    @Before
    fun setUp() {
        toastDisplayedCallCount = 0
        purchasePackageCallCount = 0
        purchaseProductCallCount = 0
        purchaseOptionCallCount = 0
        setupTestViewModels()
    }

    @Test
    fun notConfiguredStateDisplaysCorrectly() {
        paparazzi.snapshot {
            InternalDebugRevenueCatScreen(
                onPurchaseCompleted = {},
                onPurchaseErrored = {},
                screenViewModel = notConfiguredViewModel,
                activity = mockk()
            )
        }
    }

    @Suppress("MagicNumber")
    @Test
    fun configuredStateDisplaysCorrectly() {
        paparazzi.snapshot {
            InternalDebugRevenueCatScreen(
                onPurchaseCompleted = {},
                onPurchaseErrored = {},
                screenViewModel = configuredViewModel,
                activity = mockk(),
            )
        }
    }

    private fun setupTestViewModels() {
        setupNotConfiguredViewModel()
        setupConfiguredViewModel()
    }

    private fun setupNotConfiguredViewModel() {
        notConfiguredStateFlow = MutableStateFlow(
            SettingScreenState.NotConfigured(
                SettingGroupState(
                    "Configuration",
                    listOf(
                        SettingState.Text("Title", "Value"),
                        SettingState.Text("Title2", "Value2"),
                    ),
                ),
            ),
        )
        notConfiguredViewModel = object : DebugRevenueCatViewModel {
            override val state: StateFlow<SettingScreenState>
                get() = notConfiguredStateFlow

            override fun toastDisplayed() {
                error("Not expected to be called")
            }

            override fun purchasePackage(activity: Activity, rcPackage: Package) {
                error("Not expected to be called")
            }

            override fun purchaseProduct(activity: Activity, storeProduct: StoreProduct) {
                error("Not expected to be called")
            }

            override fun purchaseSubscriptionOption(activity: Activity, subscriptionOption: SubscriptionOption) {
                error("Not expected to be called")
            }
        }
    }

    private fun setupConfiguredViewModel() {
        configuredStateFlow = MutableStateFlow(
            SettingScreenState.Configured(
                SettingGroupState(
                    "Configuration",
                    listOf(
                        SettingState.Text("Title", "Value"),
                        SettingState.Text("Title2", "Value2"),
                    ),
                ),
                SettingGroupState(
                    "Customer Info",
                    listOf(
                        SettingState.Text("Title3", "Value3"),
                        SettingState.Text("Title4", "Value4"),
                    ),
                ),
                SettingGroupState(
                    "Offerings",
                    listOf(
                        SettingState.OfferingSetting(testOffering),
                        SettingState.OfferingSetting(testOffering),
                    ),
                ),
            ),
        )
        configuredViewModel = object : DebugRevenueCatViewModel {
            override val state: StateFlow<SettingScreenState>
                get() = configuredStateFlow

            override fun toastDisplayed() {
                toastDisplayedCallCount++
            }

            override fun purchasePackage(activity: Activity, rcPackage: Package) {
                purchasePackageCallCount++
            }

            override fun purchaseProduct(activity: Activity, storeProduct: StoreProduct) {
                purchaseProductCallCount++
            }

            override fun purchaseSubscriptionOption(activity: Activity, subscriptionOption: SubscriptionOption) {
                purchaseOptionCallCount++
            }
        }
    }
}
