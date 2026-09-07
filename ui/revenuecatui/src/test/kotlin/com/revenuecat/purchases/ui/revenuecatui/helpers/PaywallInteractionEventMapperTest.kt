@file:OptIn(InternalRevenueCatAPI::class, ExperimentalSerializationApi::class)

package com.revenuecat.purchases.ui.revenuecatui.helpers

import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.paywalls.events.PaywallComponentInteractionData
import com.revenuecat.purchases.paywalls.events.PaywallComponentType
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.paywalls.events.PaywallEventType
import com.revenuecat.purchases.ui.revenuecatui.PaywallInteractionEvent.ComponentTypes
import com.revenuecat.purchases.ui.revenuecatui.PaywallInteractionEvent.Keys
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.serializer
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.entry
import org.junit.Test
import java.util.Date
import java.util.UUID

internal class PaywallInteractionEventMapperTest {

    private val eventId = UUID.randomUUID()
    private val sessionId = UUID.randomUUID()
    private val date = Date(1_700_000_000_000L)

    private fun event(
        interaction: PaywallComponentInteractionData,
        paywallIdentifier: String? = "pw_123",
    ) = PaywallEvent(
        creationData = PaywallEvent.CreationData(id = eventId, date = date),
        data = PaywallEvent.Data(
            paywallIdentifier = paywallIdentifier,
            presentedOfferingContext = PresentedOfferingContext(offeringIdentifier = "default"),
            paywallRevision = 7,
            sessionIdentifier = sessionId,
            displayMode = "full_screen",
            localeIdentifier = "en_US",
            darkMode = true,
        ),
        type = PaywallEventType.COMPONENT_INTERACTION,
        componentInteraction = interaction,
    )

    @Test
    fun `fully populated interaction maps every documented key`() {
        val interaction = PaywallComponentInteractionData(
            componentType = PaywallComponentType.CAROUSEL,
            componentName = "hero",
            componentValue = "page_change",
            componentUrl = "https://example.com",
            originIndex = 0,
            destinationIndex = 1,
            originContextName = "first",
            destinationContextName = "second",
            defaultIndex = 0,
            originPackageIdentifier = "monthly",
            destinationPackageIdentifier = "annual",
            defaultPackageIdentifier = "monthly",
            originProductIdentifier = "com.app.monthly",
            destinationProductIdentifier = "com.app.annual",
            defaultProductIdentifier = "com.app.monthly",
            currentPackageIdentifier = "monthly",
            resultingPackageIdentifier = "annual",
            currentProductIdentifier = "com.app.monthly",
            resultingProductIdentifier = "com.app.annual",
        )

        val map = event(interaction).toInteractionEvent().rawProperties

        assertThat(map).containsOnly(
            entry(Keys.TIMESTAMP.name, date.time),
            entry(Keys.SESSION_ID.name, sessionId.toString()),
            entry(Keys.OFFERING_ID.name, "default"),
            entry(Keys.PAYWALL_ID.name, "pw_123"),
            entry(Keys.PAYWALL_REVISION.name, 7),
            entry(Keys.DISPLAY_MODE.name, "full_screen"),
            entry(Keys.DARK_MODE.name, true),
            entry(Keys.LOCALE.name, "en_US"),
            entry(Keys.COMPONENT_TYPE.name, ComponentTypes.CAROUSEL),
            entry(Keys.COMPONENT_VALUE.name, "page_change"),
            entry(Keys.COMPONENT_NAME.name, "hero"),
            entry(Keys.COMPONENT_URL.name, "https://example.com"),
            entry(Keys.ORIGIN_INDEX.name, 0),
            entry(Keys.DESTINATION_INDEX.name, 1),
            entry(Keys.ORIGIN_CONTEXT_NAME.name, "first"),
            entry(Keys.DESTINATION_CONTEXT_NAME.name, "second"),
            entry(Keys.DEFAULT_INDEX.name, 0),
            entry(Keys.ORIGIN_PACKAGE_ID.name, "monthly"),
            entry(Keys.DESTINATION_PACKAGE_ID.name, "annual"),
            entry(Keys.DEFAULT_PACKAGE_ID.name, "monthly"),
            entry(Keys.CURRENT_PACKAGE_ID.name, "monthly"),
            entry(Keys.RESULTING_PACKAGE_ID.name, "annual"),
            entry(Keys.ORIGIN_PRODUCT_ID.name, "com.app.monthly"),
            entry(Keys.DESTINATION_PRODUCT_ID.name, "com.app.annual"),
            entry(Keys.DEFAULT_PRODUCT_ID.name, "com.app.monthly"),
            entry(Keys.CURRENT_PRODUCT_ID.name, "com.app.monthly"),
            entry(Keys.RESULTING_PRODUCT_ID.name, "com.app.annual"),
        )
    }

    @Test
    fun `minimal interaction omits absent keys`() {
        val interaction = PaywallComponentInteractionData(
            componentType = PaywallComponentType.BUTTON,
            componentValue = "restore_purchases",
        )

        val map = event(interaction, paywallIdentifier = null).toInteractionEvent().rawProperties

        assertThat(map.keys).containsExactlyInAnyOrder(
            Keys.TIMESTAMP.name,
            Keys.SESSION_ID.name,
            Keys.OFFERING_ID.name,
            Keys.PAYWALL_REVISION.name,
            Keys.DISPLAY_MODE.name,
            Keys.DARK_MODE.name,
            Keys.LOCALE.name,
            Keys.COMPONENT_TYPE.name,
            Keys.COMPONENT_VALUE.name,
        )
    }

    @Test
    fun `typed keys read values and return null for absent keys`() {
        val interaction = PaywallComponentInteractionData(
            componentType = PaywallComponentType.TAB,
            componentValue = "annual",
            originIndex = 2,
        )

        val event = event(interaction).toInteractionEvent()

        assertThat(event.getProperty(Keys.COMPONENT_TYPE)).isEqualTo(ComponentTypes.TAB)
        assertThat(event.getProperty(Keys.ORIGIN_INDEX)).isEqualTo(2)
        assertThat(event.getProperty(Keys.TIMESTAMP)).isEqualTo(date.time)
        assertThat(event.getProperty(Keys.DARK_MODE)).isTrue()
        assertThat(event.getProperty(Keys.COMPONENT_NAME)).isNull()
        assertThat(event.getProperty(Keys.DESTINATION_INDEX)).isNull()
    }

    @Test
    fun `component type values match the wire serial names`() {
        val descriptor = serializer<PaywallComponentType>().descriptor
        PaywallComponentType.values().forEach { type ->
            assertThat(type.toInteractionEventValue()).isEqualTo(descriptor.getElementName(type.ordinal))
        }
    }
}
