package com.revenuecat.purchases.backend_integration_tests

import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Delay
import com.revenuecat.purchases.common.events.BackendEvent
import com.revenuecat.purchases.common.events.EventsRequest
import com.revenuecat.purchases.common.offlineentitlements.ProductEntitlementMapping
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.events.CustomerCenterDisplayMode
import com.revenuecat.purchases.customercenter.events.CustomerCenterEventType
import com.revenuecat.purchases.paywalls.events.PaywallEventType
import kotlinx.serialization.SerialName
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Test
import java.net.URL
import java.util.Date
import java.util.UUID

internal class ProductionBackendEventsTest: BaseBackendIntegrationTest() {
    override fun apiKey() = Constants.apiKey


    @Test
    fun `can post events without errors`() {
        ensureBlockFinishes { latch ->
            backend.postEvents(
                paywallEventRequest = EventsRequest(
                    events = listOf(
                        BackendEvent.Paywalls(
                            id = UUID.randomUUID().toString(),
                            version = 1,
                            type = PaywallEventType.CANCEL.value,
                            appUserID = UUID.randomUUID().toString(),
                            sessionID = UUID.randomUUID().toString(),
                            offeringID = UUID.randomUUID().toString(),
                            paywallID = "paywall_id",
                            paywallRevision = 1,
                            timestamp = Date().time,
                            displayMode = "FULL_SCREEN",
                            darkMode = true,
                            localeIdentifier = "en_US"
                        ),
                        BackendEvent.CustomerCenter(
                            id = UUID.randomUUID().toString(),
                            revisionID = 1,
                            type = CustomerCenterEventType.IMPRESSION,
                            appUserID = UUID.randomUUID().toString(),
                            appSessionID = UUID.randomUUID().toString(),
                            timestamp = Date().time,
                            darkMode = true,
                            displayMode = CustomerCenterDisplayMode.FULL_SCREEN,
                            locale = "en_US",
                            path = null,
                            url = null,
                            surveyOptionID = null
                        ),
                        BackendEvent.CustomerCenter(
                            id = UUID.randomUUID().toString(),
                            revisionID = 1,
                            type = CustomerCenterEventType.SURVEY_OPTION_CHOSEN,
                            appUserID = UUID.randomUUID().toString(),
                            appSessionID = UUID.randomUUID().toString(),
                            timestamp = Date().time,
                            darkMode = true,
                            displayMode = CustomerCenterDisplayMode.FULL_SCREEN,
                            path = CustomerCenterConfigData.HelpPath.PathType.CANCEL,
                            url = null,
                            surveyOptionID = "surveyOptionID",
                            locale = "en_US",
                        )
                    )
                ),
                baseURL = AppConfig.paywallEventsURL,
                delay = Delay.NONE,
                onSuccessHandler = {
                    latch.countDown()
                },
                onErrorHandler = { error,_ ->
                    fail("Expected success but got error: $error")
                }
            )
        }
    }
}
