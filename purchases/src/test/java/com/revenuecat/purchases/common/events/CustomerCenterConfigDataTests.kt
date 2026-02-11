package com.revenuecat.purchases.common.events

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.CustomerCenterRoot
import com.revenuecat.purchases.customercenter.RCColor
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import java.io.File

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
public class CustomerCenterConfigDataTest {

    @Test
    fun `CustomerCenterConfigData creation and equality`() {
        val configData1 = createSampleConfigData()
        val configData2 = createSampleConfigData()
        val configData3 = CustomerCenterConfigData(
            appearance = CustomerCenterConfigData.Appearance(
                light = CustomerCenterConfigData.Appearance.ColorInformation(
                    accentColor = RCColor("#FF0000"),
                    textColor = RCColor("#000000")
                ),
                dark = CustomerCenterConfigData.Appearance.ColorInformation(
                    accentColor = RCColor("#00FF00"),
                    textColor = RCColor("#FFFFFF")
                )
            ),
            screens = mapOf(
                CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT to CustomerCenterConfigData.Screen(
                    type = CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT,
                    title = "Management Screen",
                    subtitle = "Manage your subscription",
                    paths = listOf(
                        CustomerCenterConfigData.HelpPath(
                            id = "path1",
                            title = "Path 1",
                            type = CustomerCenterConfigData.HelpPath.PathType.CANCEL
                        )
                    )
                )
            ),
            support = CustomerCenterConfigData.Support(
                email = "test@revenuecat.com",
                supportTickets = CustomerCenterConfigData.Support.SupportTickets(),
            ),
            localization = CustomerCenterConfigData.Localization(
                locale = "en",
                localizedStrings = mapOf(
                    "no_thanks" to "No, thanks",
                    "cancel" to "Cancel"
                )
            )
        )

        assertThat(configData1).isEqualTo(configData2)
        assertThat(configData1.hashCode()).isEqualTo(configData2.hashCode())
        assertThat(configData1).isNotEqualTo(configData3)
        assertThat(configData1.hashCode()).isNotEqualTo(configData3.hashCode())
    }

    @Test
    fun `Localization returns correct localized strings and default for those not found`() {
        val localization = CustomerCenterConfigData.Localization(
            locale = "en",
            localizedStrings = mapOf(
                "no_thanks" to "No, thanks",
                "cancel" to "Cancel"
            )
        )

        assertThat(localization.commonLocalizedString(CustomerCenterConfigData.Localization.CommonLocalizedString.NO_THANKS))
            .isEqualTo("No, thanks")
        assertThat(localization.commonLocalizedString(CustomerCenterConfigData.Localization.CommonLocalizedString.CANCEL))
            .isEqualTo("Cancel")
        assertThat(localization.commonLocalizedString(CustomerCenterConfigData.Localization.CommonLocalizedString.RESTORE_PURCHASES))
            .isEqualTo("Restore purchases")
    }

    @Test
    fun `HelpPath properties are correctly set`() {
        val helpPath = CustomerCenterConfigData.HelpPath(
            id = "test_id",
            title = "Test Title",
            type = CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE,
            promotionalOffer = CustomerCenterConfigData.HelpPath.PathDetail.PromotionalOffer(
                androidOfferId = "offer_id",
                eligible = true,
                title = "Offer Title",
                subtitle = "Offer Subtitle",
                productMapping = mapOf("monthly_subscription" to "rc-refund-offer")
            )
        )

        assertThat(helpPath.id).isEqualTo("test_id")
        assertThat(helpPath.title).isEqualTo("Test Title")
        assertThat(helpPath.type).isEqualTo(CustomerCenterConfigData.HelpPath.PathType.MISSING_PURCHASE)
        assertThat(helpPath.promotionalOffer).isNotNull
        assertThat(helpPath.feedbackSurvey).isNull()
    }

    @Test
    fun `Appearance color information is correctly set`() {
        val appearance = CustomerCenterConfigData.Appearance(
            light = CustomerCenterConfigData.Appearance.ColorInformation(
                accentColor = RCColor("#FF0000"),
                textColor = RCColor("#000000")
            ),
            dark = CustomerCenterConfigData.Appearance.ColorInformation(
                accentColor = RCColor("#00FF00"),
                textColor = RCColor("#FFFFFF")
            )
        )

        assertThat(appearance.light).isNotNull
        assertThat(appearance.dark).isNotNull
        assertThat(appearance.light?.accentColor).isEqualTo(RCColor("#FF0000"))
        assertThat(appearance.dark?.accentColor).isEqualTo(RCColor("#00FF00"))
    }

    @Test
    fun `Screen properties are correctly set`() {
        val screen = CustomerCenterConfigData.Screen(
            type = CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT,
            title = "Management Screen",
            subtitle = "Manage your subscription",
            paths = listOf(
                CustomerCenterConfigData.HelpPath(
                    id = "path1",
                    title = "Path 1",
                    type = CustomerCenterConfigData.HelpPath.PathType.CANCEL
                )
            )
        )

        assertThat(screen.type).isEqualTo(CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT)
        assertThat(screen.title).isEqualTo("Management Screen")
        assertThat(screen.subtitle).isEqualTo("Manage your subscription")
        assertThat(screen.paths).hasSize(1)
    }

    @Test
    fun `Support email is correctly set`() {
        val support = CustomerCenterConfigData.Support(
            email = "support@example.com",
            supportTickets = CustomerCenterConfigData.Support.SupportTickets(),
        )
        assertThat(support.email).isEqualTo("support@example.com")
    }

    @Test
    fun `can parse json with unknown screen types`() {
        val json = JSONObject(loadTestJSON())
        val screens = json.getJSONObject("customer_center").getJSONObject("screens")
        screens.put("random_screen_id", screens.getJSONObject("MANAGEMENT"))
        val configData = createSampleConfigData(json.toString())
        assertThat(configData.screens).hasSize(2)
    }

    @Test
    fun `can parse json with unknown path types`() {
        val json = JSONObject(loadTestJSON())
        val managementScreenPaths = json
            .getJSONObject("customer_center")
            .getJSONObject("screens")
            .getJSONObject("MANAGEMENT")
            .getJSONArray("paths")
        val firstPathClone = JSONObject(managementScreenPaths.getJSONObject(0).toString())
        managementScreenPaths.put(firstPathClone.put("type", "UNKNOWN_PATH_TYPE"))
        val configData = createSampleConfigData(json.toString())
        assertThat(configData.screens[CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT]!!.paths).hasSize(5)
    }

    @Test
    fun `can parse json with unknown open method type`() {
        val json = JSONObject(loadTestJSON())
        val managementScreenPaths = json
            .getJSONObject("customer_center")
            .getJSONObject("screens")
            .getJSONObject("MANAGEMENT")
            .getJSONArray("paths")

        val firstPathClone = JSONObject(managementScreenPaths.getJSONObject(0).toString())
        managementScreenPaths.put(firstPathClone.put("open_method", "UNKNOWN_OPEN_METHOD"))

        val configData = createSampleConfigData(json.toString())
        assertThat(configData.screens[CustomerCenterConfigData.Screen.ScreenType.MANAGEMENT]!!.paths).hasSize(5)
    }

    @Test
    fun `can parse json without display_virtual_currencies`() {
        val json = JSONObject(loadTestJSON())
        val support = json.getJSONObject("customer_center").getJSONObject("support")
        support.remove("display_virtual_currencies")
        val configData = createSampleConfigData(json.toString())
        assertThat(configData.support.displayVirtualCurrencies).isNull()
    }

    @Test
    fun `can parse json with display_virtual_currencies true`() {
        val json = JSONObject(loadTestJSON())
        val support = json.getJSONObject("customer_center").getJSONObject("support")
        support.put("display_virtual_currencies", true)
        val configData = createSampleConfigData(json.toString())
        assertThat(configData.support.displayVirtualCurrencies).isTrue()
    }

    @Test
    fun `can parse json with display_virtual_currencies false`() {
        val json = JSONObject(loadTestJSON())
        val support = json.getJSONObject("customer_center").getJSONObject("support")
        support.put("display_virtual_currencies", false)
        val configData = createSampleConfigData(json.toString())
        assertThat(configData.support.displayVirtualCurrencies).isFalse()
    }

    private fun createSampleConfigData(json: String = loadTestJSON()): CustomerCenterConfigData {
        return Backend.json.decodeFromString(
            CustomerCenterRoot.serializer(),
            json,
        ).customerCenter
    }

    private fun loadTestJSON() =
        File(javaClass.classLoader!!.getResource("get_customer_center_config_success.json").file).readText()
}