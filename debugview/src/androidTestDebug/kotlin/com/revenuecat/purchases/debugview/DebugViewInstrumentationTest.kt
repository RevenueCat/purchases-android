package com.revenuecat.purchases.debugview

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.debugview.models.SettingGroupState
import com.revenuecat.purchases.debugview.models.SettingScreenState
import com.revenuecat.purchases.debugview.models.SettingState
import com.revenuecat.purchases.debugview.models.testOffering
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@Suppress("TooManyFunctions")
@RunWith(AndroidJUnit4::class)
class DebugViewInstrumentationTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

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
        composeTestRule.setContent {
            InternalDebugRevenueCatBottomSheet(
                onPurchaseCompleted = {},
                onPurchaseErrored = {},
                isVisible = true,
                onDismissCallback = null,
                viewModel = notConfiguredViewModel,
            )
        }

        composeTestRule.onAllNodesWithTag("SettingGroup").assertCountEquals(1)
        composeTestRule.onAllNodesWithTag("SettingText").assertCountEquals(2)
        composeTestRule.onAllNodesWithTag("SettingOffering").assertCountEquals(0)
        composeTestRule.onNodeWithText("Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Title2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Value").assertIsDisplayed()
        composeTestRule.onNodeWithText("Value2").assertIsDisplayed()
    }

    @Test
    fun notConfiguredStateDoesNotDisplayIfNotVisible() {
        composeTestRule.setContent {
            InternalDebugRevenueCatBottomSheet(
                onPurchaseCompleted = {},
                onPurchaseErrored = {},
                isVisible = false,
                onDismissCallback = null,
                viewModel = notConfiguredViewModel,
            )
        }

        composeTestRule.onAllNodesWithTag("SettingGroup").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("SettingText").assertCountEquals(0)
        composeTestRule.onAllNodesWithTag("SettingOffering").assertCountEquals(0)
        composeTestRule.onNodeWithText("Title").assertDoesNotExist()
        composeTestRule.onNodeWithText("Title2").assertDoesNotExist()
        composeTestRule.onNodeWithText("Value").assertDoesNotExist()
        composeTestRule.onNodeWithText("Value2").assertDoesNotExist()
    }

    @Suppress("MagicNumber")
    @Test
    fun configuredStateDisplaysCorrectly() {
        composeTestRule.setContent {
            InternalDebugRevenueCatBottomSheet(
                onPurchaseCompleted = {},
                onPurchaseErrored = {},
                isVisible = true,
                onDismissCallback = null,
                viewModel = configuredViewModel,
            )
        }

        composeTestRule.onAllNodesWithTag("SettingGroup").assertCountEquals(3)
        composeTestRule.onAllNodesWithTag("SettingText").assertCountEquals(4)
        composeTestRule.onAllNodesWithTag("SettingOffering").assertCountEquals(2)
        composeTestRule.onNodeWithText("Title").assertIsDisplayed()
        composeTestRule.onNodeWithText("Title2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Title3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Title4").assertIsDisplayed()
        composeTestRule.onNodeWithText("Value").assertIsDisplayed()
        composeTestRule.onNodeWithText("Value2").assertIsDisplayed()
        composeTestRule.onNodeWithText("Value3").assertIsDisplayed()
        composeTestRule.onNodeWithText("Value4").assertIsDisplayed()
    }

    @Test
    fun tappingOnPurchasePackageCallsCorrectModelInViewModel() {
        composeTestRule.setContent {
            InternalDebugRevenueCatBottomSheet(
                onPurchaseCompleted = {},
                onPurchaseErrored = {},
                isVisible = true,
                onDismissCallback = null,
                viewModel = configuredViewModel,
            )
        }

        assertThat(purchasePackageCallCount).isEqualTo(0)
        composeTestRule.onAllNodesWithText("Buy package").assertCountEquals(2).onFirst().performClick()
        assertThat(purchasePackageCallCount).isEqualTo(1)
    }

    @Test
    fun tappingOnPurchaseProductCallsCorrectModelInViewModel() {
        composeTestRule.setContent {
            InternalDebugRevenueCatBottomSheet(
                onPurchaseCompleted = {},
                onPurchaseErrored = {},
                isVisible = true,
                onDismissCallback = null,
                viewModel = configuredViewModel,
            )
        }

        assertThat(purchaseProductCallCount).isEqualTo(0)
        composeTestRule.onAllNodesWithText("Buy product").assertCountEquals(2).onFirst().performClick()
        assertThat(purchaseProductCallCount).isEqualTo(1)
    }

    @Test
    fun tappingOnPurchaseOptionCallsCorrectModelInViewModel() {
        composeTestRule.setContent {
            InternalDebugRevenueCatBottomSheet(
                onPurchaseCompleted = {},
                onPurchaseErrored = {},
                isVisible = true,
                onDismissCallback = null,
                viewModel = configuredViewModel,
            )
        }

        scrollToNodeWithText("Buy option (default)")

        assertThat(purchaseOptionCallCount).isEqualTo(0)
        composeTestRule.onAllNodesWithText("Buy option (default)").assertCountEquals(2).onFirst().performClick()
        assertThat(purchaseOptionCallCount).isEqualTo(1)
    }

    @Suppress("SameParameterValue")
    private fun scrollToNodeWithText(text: String) {
        composeTestRule.onNodeWithTag("DebugRevenueCatScreen").performScrollToNode(hasText(text))
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
