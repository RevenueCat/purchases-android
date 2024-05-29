package com.revenuecat.purchases.attributes

import android.app.Application
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CacheFetchPolicy
import com.revenuecat.purchases.CustomerInfoHelper
import com.revenuecat.purchases.CustomerInfoUpdateHandler
import com.revenuecat.purchases.PostPendingTransactionsHelper
import com.revenuecat.purchases.PostReceiptHelper
import com.revenuecat.purchases.PostTransactionWithProductDetailsHelper
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesOrchestrator
import com.revenuecat.purchases.PurchasesState
import com.revenuecat.purchases.PurchasesStateCache
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.BillingAbstract
import com.revenuecat.purchases.common.PlatformInfo
import com.revenuecat.purchases.common.caching.DeviceCache
import com.revenuecat.purchases.common.offerings.OfferingsManager
import com.revenuecat.purchases.common.offlineentitlements.OfflineEntitlementsManager
import com.revenuecat.purchases.common.subscriberattributes.SubscriberAttributeKey
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.paywalls.PaywallPresentedCache
import com.revenuecat.purchases.subscriberattributes.SubscriberAttributesManager
import com.revenuecat.purchases.utils.SyncDispatcher
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.verify
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class SubscriberAttributesPurchasesTests {

    private lateinit var underTest: Purchases
    private val appUserId = "juan"
    private val subscriberAttributesManagerMock = mockk<SubscriberAttributesManager>()
    private val backendMock = mockk<Backend>(relaxed = true)
    private val billingWrapperMock = mockk<BillingAbstract>(relaxed = true)
    private val customerInfoHelperMock = mockk<CustomerInfoHelper>()
    private val customerInfoUpdateHandlerMock = mockk<CustomerInfoUpdateHandler>()
    private val offlineEntitlementsManagerMock = mockk<OfflineEntitlementsManager>()
    private val postReceiptHelperMock = mockk<PostReceiptHelper>()
    private val offeringsManagerMock = mockk<OfferingsManager>()
    private lateinit var applicationMock: Application

    @Before
    fun setup() {
        every {
            offlineEntitlementsManagerMock.updateProductEntitlementMappingCacheIfStale()
        } just runs

        val cache: DeviceCache = mockk(relaxed = true)

        val appConfig = AppConfig(
            context = mockk(relaxed = true),
            finishTransactions = true,
            showInAppMessagesAutomatically = false,
            platformInfo = PlatformInfo(flavor = "native", version = "3.2.0"),
            proxyURL = null,
            store = Store.PLAY_STORE
        )
        val identityManager = mockk<IdentityManager>(relaxed = true).apply {
            every { currentAppUserID } returns appUserId
        }
        val postTransactionHelper = PostTransactionWithProductDetailsHelper(billingWrapperMock, postReceiptHelperMock)
        val postPendingTransactionsHelper = PostPendingTransactionsHelper(
            appConfig,
            cache,
            billingWrapperMock,
            SyncDispatcher(),
            identityManager,
            postTransactionHelper,
        )

        val purchasesOrchestrator = PurchasesOrchestrator(
            application = mockk<Application>(relaxed = true).also { applicationMock = it },
            backingFieldAppUserID = appUserId,
            backend = backendMock,
            billing = billingWrapperMock,
            deviceCache = cache,
            identityManager = identityManager,
            subscriberAttributesManager = subscriberAttributesManagerMock,
            appConfig = appConfig,
            customerInfoHelper = customerInfoHelperMock,
            customerInfoUpdateHandler = customerInfoUpdateHandlerMock,
            diagnosticsSynchronizer = null,
            offlineEntitlementsManager = offlineEntitlementsManagerMock,
            postReceiptHelper = postReceiptHelperMock,
            postTransactionWithProductDetailsHelper = postTransactionHelper,
            postPendingTransactionsHelper = postPendingTransactionsHelper,
            syncPurchasesHelper = mockk(),
            offeringsManager = offeringsManagerMock,
            paywallEventsManager = null,
            paywallPresentedCache = PaywallPresentedCache(),
            purchasesStateCache = PurchasesStateCache(PurchasesState()),
        )

        underTest = Purchases(purchasesOrchestrator)
    }

    @After
    fun tearDown() {
        clearMocks(customerInfoHelperMock, customerInfoUpdateHandlerMock, offeringsManagerMock)
    }

    @Test
    fun `setting email attribute`() {
        every {
            subscriberAttributesManagerMock.setAttribute(any(), any(), appUserId)
        } just Runs

        val email = "email"
        underTest.setEmail(email)

        verify {
            subscriberAttributesManagerMock.setAttribute(
                SubscriberAttributeKey.Email,
                email,
                appUserId,
            )
        }
    }

    @Test
    fun `setting phone number attribute`() {
        val phoneNumber = "3154589485"

        every {
            subscriberAttributesManagerMock.setAttribute(SubscriberAttributeKey.PhoneNumber, phoneNumber, appUserId)
        } just Runs

        underTest.setPhoneNumber(phoneNumber)

        verify {
            subscriberAttributesManagerMock.setAttribute(
                SubscriberAttributeKey.PhoneNumber,
                phoneNumber,
                appUserId,
            )
        }
    }

    @Test
    fun `setting display name attribute`() {
        every {
            subscriberAttributesManagerMock.setAttribute(any(), any(), appUserId)
        } just Runs

        val displayName = "Cesar"
        underTest.setDisplayName(displayName)

        verify {
            subscriberAttributesManagerMock.setAttribute(
                SubscriberAttributeKey.DisplayName,
                displayName,
                appUserId,
            )
        }
    }

    @Test
    fun `setting push token attribute`() {
        every {
            subscriberAttributesManagerMock.setAttribute(any(), any(), appUserId)
        } just Runs

        val pushToken = "ajdjfh30203"
        underTest.setPushToken(pushToken)

        verify {
            subscriberAttributesManagerMock.setAttribute(
                SubscriberAttributeKey.FCMTokens,
                pushToken,
                appUserId,
            )
        }
    }

    @Test
    fun `on app foregrounded attributes are synced`() {
        every {
            subscriberAttributesManagerMock.synchronizeSubscriberAttributesForAllUsers(appUserId)
        } just Runs
        every {
            customerInfoHelperMock.retrieveCustomerInfo(appUserId, CacheFetchPolicy.FETCH_CURRENT, false, any())
        } just Runs
        every {
            offeringsManagerMock.onAppForeground(appUserId)
        } just Runs
        underTest.purchasesOrchestrator.onAppForegrounded()
        verify(exactly = 1) {
            subscriberAttributesManagerMock.synchronizeSubscriberAttributesForAllUsers(appUserId)
        }
    }

    @Test
    fun `on app backgrounded attributes are synced`() {
        every {
            subscriberAttributesManagerMock.synchronizeSubscriberAttributesForAllUsers(appUserId)
        } just Runs
        underTest.purchasesOrchestrator.onAppBackgrounded()
        verify(exactly = 1) {
            subscriberAttributesManagerMock.synchronizeSubscriberAttributesForAllUsers(appUserId)
        }
    }

    // region Attribution IDs

    @Test
    fun `collectDeviceIdentifiers`() {
        every {
            subscriberAttributesManagerMock.collectDeviceIdentifiers(appUserId, applicationMock)
        } just Runs

        underTest.collectDeviceIdentifiers()

        verify {
            subscriberAttributesManagerMock.collectDeviceIdentifiers(appUserId, applicationMock)
        }
    }

    @Test
    fun `setAdjustID`() {
        attributionIDTest(SubscriberAttributeKey.AttributionIds.Adjust) { id ->
            underTest.setAdjustID(id)
        }
    }

    @Test
    fun `setAppsflyerID`() {
        attributionIDTest(SubscriberAttributeKey.AttributionIds.AppsFlyer) { id ->
            underTest.setAppsflyerID(id)
        }
    }

    @Test
    fun `setFBAnonymousID`() {
        attributionIDTest(SubscriberAttributeKey.AttributionIds.Facebook) { id ->
            underTest.setFBAnonymousID(id)
        }
    }

    @Test
    fun `setMparticleID`() {
        attributionIDTest(SubscriberAttributeKey.AttributionIds.Mparticle) { id ->
            underTest.setMparticleID(id)
        }
    }

    @Test
    fun `setCleverTapID`() {
        attributionIDTest(SubscriberAttributeKey.AttributionIds.CleverTap) { parameter ->
            underTest.setCleverTapID(parameter)
        }
    }

    // endregion

    // region Integration IDs

    @Test
    fun `setMixpanelDistinctID`() {
        integrationIDTest(SubscriberAttributeKey.IntegrationIds.MixpanelDistinctId) { parameter ->
            underTest.setMixpanelDistinctID(parameter)
        }
    }

    @Test
    fun `setOnesignalID`() {
        integrationIDTest(SubscriberAttributeKey.IntegrationIds.OneSignal) { id ->
            underTest.setOnesignalID(id)
        }
    }

    @Test
    fun `setOnesignalUserID`() {
        integrationIDTest(SubscriberAttributeKey.IntegrationIds.OneSignalUserId) { id ->
            underTest.setOnesignalUserID(id)
        }
    }

    @Test
    fun `setAirshipChannelID`() {
        integrationIDTest(SubscriberAttributeKey.IntegrationIds.Airship) { id ->
            underTest.setAirshipChannelID(id)
        }
    }

    @Test
    fun `setFirebaseAppInstanceID`() {
        integrationIDTest(SubscriberAttributeKey.IntegrationIds.FirebaseAppInstanceId) { parameter ->
            underTest.setFirebaseAppInstanceID(parameter)
        }
    }

    // endregion

    // region Campaign parameters

    @Test
    fun `setMediaSource`() {
        campaignParameterTest(SubscriberAttributeKey.CampaignParameters.MediaSource) { parameter ->
            underTest.setMediaSource(parameter)
        }
    }

    @Test
    fun `setCampaign`() {
        campaignParameterTest(SubscriberAttributeKey.CampaignParameters.Campaign) { parameter ->
            underTest.setCampaign(parameter)
        }
    }

    @Test
    fun `setAdGroup`() {
        campaignParameterTest(SubscriberAttributeKey.CampaignParameters.AdGroup) { parameter ->
            underTest.setAdGroup(parameter)
        }
    }

    @Test
    fun `setAd`() {
        campaignParameterTest(SubscriberAttributeKey.CampaignParameters.Ad) { parameter ->
            underTest.setAd(parameter)
        }
    }

    @Test
    fun `setKeyword`() {
        campaignParameterTest(SubscriberAttributeKey.CampaignParameters.Keyword) { parameter ->
            underTest.setKeyword(parameter)
        }
    }

    @Test
    fun `setCreative`() {
        campaignParameterTest(SubscriberAttributeKey.CampaignParameters.Creative) { parameter ->
            underTest.setCreative(parameter)
        }
    }

    // endregion

    private fun attributionIDTest(
        network: SubscriberAttributeKey.AttributionIds,
        functionToTest: (String) -> Unit,
    ) {
        val id = "12345"

        every {
            subscriberAttributesManagerMock.setAttributionID(
                network,
                id,
                appUserId,
                applicationMock,
            )
        } just Runs

        functionToTest(id)

        verify {
            subscriberAttributesManagerMock.setAttributionID(
                network,
                id,
                appUserId,
                applicationMock,
            )
        }
    }

    private fun integrationIDTest(
        parameter: SubscriberAttributeKey.IntegrationIds,
        functionToTest: (String) -> Unit,
    ) {
        val parameterValue = "parametervalue"

        every {
            subscriberAttributesManagerMock.setAttribute(any(), parameterValue, appUserId)
        } just Runs

        functionToTest(parameterValue)

        verify {
            subscriberAttributesManagerMock.setAttribute(
                parameter,
                parameterValue,
                appUserId,
            )
        }
    }

    private fun campaignParameterTest(
        parameter: SubscriberAttributeKey.CampaignParameters,
        functionToTest: (String) -> Unit,
    ) {
        val parameterValue = "parametervalue"

        every {
            subscriberAttributesManagerMock.setAttribute(any(), parameterValue, appUserId)
        } just Runs

        functionToTest(parameterValue)

        verify {
            subscriberAttributesManagerMock.setAttribute(
                parameter,
                parameterValue,
                appUserId,
            )
        }
    }
}
