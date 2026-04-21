package com.revenuecat.purchases.ui.revenuecatui.customercenter

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class ScreenOfferingExtensionsTest {

    private val localization = CustomerCenterConfigData.Localization(
        locale = "en_US",
        localizedStrings = mapOf("buy_subscription" to "Custom Buy Subscription Text")
    )

    @Test
    fun `resolveButtonText returns custom buttonText when available`() {
        val screenOffering = CustomerCenterConfigData.ScreenOffering(
            type = CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.CURRENT,
            buttonText = "Get Premium Now"
        )
        
        val screen = CustomerCenterConfigData.Screen(
            type = CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE,
            title = "Test Screen",
            paths = emptyList(),
            offering = screenOffering
        )

        val result = screen.resolveButtonText(localization)

        assertThat(result).isEqualTo("Get Premium Now")
    }

    @Test
    fun `resolveButtonText falls back to BUY_SUBSCRIPTION when buttonText is null`() {
        val screenOffering = CustomerCenterConfigData.ScreenOffering(
            type = CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.CURRENT,
            buttonText = null
        )
        
        val screen = CustomerCenterConfigData.Screen(
            type = CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE,
            title = "Test Screen",
            paths = emptyList(),
            offering = screenOffering
        )

        val result = screen.resolveButtonText(localization)

        assertThat(result).isEqualTo("Custom Buy Subscription Text")
    }

    @Test
    fun `resolveButtonText falls back to BUY_SUBSCRIPTION when offering is null`() {
        val screen = CustomerCenterConfigData.Screen(
            type = CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE,
            title = "Test Screen",
            paths = emptyList(),
            offering = null
        )

        val result = screen.resolveButtonText(localization)

        assertThat(result).isEqualTo("Custom Buy Subscription Text")
    }

    @Test
    fun `resolveButtonText uses default BUY_SUBSCRIPTION text when not in localization`() {
        val emptyLocalization = CustomerCenterConfigData.Localization(
            locale = "en_US",
            localizedStrings = emptyMap()
        )

        val screenOffering = CustomerCenterConfigData.ScreenOffering(
            type = CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.CURRENT,
            buttonText = null
        )
        
        val screen = CustomerCenterConfigData.Screen(
            type = CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE,
            title = "Test Screen",
            paths = emptyList(),
            offering = screenOffering
        )

        val result = screen.resolveButtonText(emptyLocalization)

        // Should fall back to the default value defined in CommonLocalizedString.BUY_SUBSCRIPTION
        assertThat(result).isEqualTo("Buy Subscription")
    }

    @Test
    fun `resolveButtonText works with empty buttonText string`() {
        val screenOffering = CustomerCenterConfigData.ScreenOffering(
            type = CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.CURRENT,
            buttonText = ""
        )
        
        val screen = CustomerCenterConfigData.Screen(
            type = CustomerCenterConfigData.Screen.ScreenType.NO_ACTIVE,
            title = "Test Screen",
            paths = emptyList(),
            offering = screenOffering
        )

        val result = screen.resolveButtonText(localization)

        assertThat(result).isEqualTo("")
    }

    @Test
    fun `resolveButtonText works with different screen types`() {
        val screenOffering = CustomerCenterConfigData.ScreenOffering(
            type = CustomerCenterConfigData.ScreenOffering.ScreenOfferingType.SPECIFIC,
            offeringId = "premium_monthly",
            buttonText = "Upgrade to Monthly"
        )
        
        val managementScreen = CustomerCenterConfigData.Screen(
            type = CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT,
            title = "Management Screen",
            paths = emptyList(),
            offering = screenOffering
        )

        val result = managementScreen.resolveButtonText(localization)

        assertThat(result).isEqualTo("Upgrade to Monthly")
    }
}