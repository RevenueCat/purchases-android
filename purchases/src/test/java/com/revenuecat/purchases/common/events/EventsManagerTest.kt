@file:OptIn(com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI::class)

package com.revenuecat.purchases.common.events

import android.annotation.SuppressLint
import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.DebugEvent
import com.revenuecat.purchases.DebugEventListener
import com.revenuecat.purchases.DebugEventName
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.PresentedOfferingContext
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.ads.events.AdEvent
import com.revenuecat.purchases.ads.events.types.AdFormat
import com.revenuecat.purchases.ads.events.types.AdMediatorName
import com.revenuecat.purchases.ads.events.types.AdRevenuePrecision
import com.revenuecat.purchases.common.AppConfig
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.FileHelper
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.customercenter.events.CustomerCenterImpressionEvent
import com.revenuecat.purchases.customercenter.events.CustomerCenterSurveyOptionChosenEvent
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.common.workflows.events.WorkflowEvent
import com.revenuecat.purchases.paywalls.events.CustomPaywallEvent
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.paywalls.events.PaywallEventType
import com.revenuecat.purchases.paywalls.events.PaywallStoredEvent
import com.revenuecat.purchases.utils.EventsFileHelper
import com.revenuecat.purchases.utils.RateLimiter
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Date
import java.util.UUID
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class EventsManagerTest {

    private val userID = "testAppUserId"
    private val paywallEvent = PaywallEvent(
        creationData = PaywallEvent.CreationData(
            id = UUID.fromString("298207f4-87af-4b57-a581-eb27bcc6e009"),
            date = Date(1699270688884)
        ),
        data = PaywallEvent.Data(
            paywallIdentifier = "paywallID",
            presentedOfferingContext = PresentedOfferingContext("offeringID"),
            paywallRevision = 5,
            sessionIdentifier = UUID.fromString("315107f4-98bf-4b68-a582-eb27bcb6e111"),
            displayMode = "footer",
            localeIdentifier = "es_ES",
            darkMode = true
        ),
        type = PaywallEventType.IMPRESSION,
    )
    private val customerCenterImpressionEvent = CustomerCenterImpressionEvent(
        creationData = CustomerCenterImpressionEvent.CreationData(
            id = UUID.fromString("298207f4-87af-4b57-a581-eb27bcc6e009"),
            date = Date(1699270688884)
        ),
        data = CustomerCenterImpressionEvent.Data(
            timestamp = Date(1699270688884),
            darkMode = true,
            locale = "es_ES"
        )
    )
    private val adEvent = AdEvent.Displayed(
        id = "ad-event-id",
        timestamp = 1699270688884,
        networkName = "Google AdMob",
        mediatorName = AdMediatorName.AD_MOB,
        adFormat = AdFormat.BANNER,
        placement = "banner_home",
        adUnitId = "ca-app-pub-123456",
        impressionId = "impression-id"
    )
    private val paywallStoredEvent = PaywallStoredEvent(paywallEvent, userID)
    private val disabledRateLimiter = mockk<RateLimiter>().apply {
        every { shouldProceed() } returns false
    }
    private var postedRequest: EventsRequest? = null

    private val testFolder = "temp_test_folder"

    private var appSessionID = UUID.randomUUID()
    private lateinit var legacyFileHelper: EventsFileHelper<PaywallStoredEvent>
    private lateinit var fileHelper: EventsFileHelper<BackendStoredEvent>
    private lateinit var adFileHelper: EventsFileHelper<BackendStoredEvent.Ad>

    private lateinit var identityManager: IdentityManager
    private lateinit var paywallEventsDispatcher: Dispatcher
    private lateinit var backend: Backend

    private lateinit var eventsManager: EventsManager

    @Before
    fun setUp() {
        val tempTestFolder = File(testFolder)
        if (tempTestFolder.exists()) {
            error("Temp test folder should not exist before starting tests")
        }
        tempTestFolder.mkdirs()

        val context = mockk<Context>().apply {
            every { filesDir } returns tempTestFolder
        }
        legacyFileHelper = EventsManager.paywalls(fileHelper = FileHelper(context))
        fileHelper = EventsManager.backendEvents(fileHelper = FileHelper(context))
        identityManager = mockk<IdentityManager>().apply {
            every { currentAppUserID } returns userID
        }
        paywallEventsDispatcher = SyncDispatcher()
        backend = mockk()

        eventsManager = EventsManager(
            appSessionID,
            legacyFileHelper,
            fileHelper,
            identityManager,
            paywallEventsDispatcher,
            postEvents = { request, delay, onSuccess, onError ->
                postedRequest = request
                backend.postEvents(
                    paywallEventRequest = request,
                    baseURL = AppConfig.paywallEventsURL,
                    delay = delay,
                    onSuccessHandler = onSuccess,
                    onErrorHandler = onError,
                )
            },
            priorityFlushRateLimiter = disabledRateLimiter,
        )
    }

    @After
    fun tearDown() {
        val tempTestFolder = File(testFolder)
        tempTestFolder.deleteRecursively()
    }

    @Test
    fun `tracking paywall events adds them to file`() {
        eventsManager.track(paywallEvent)

        checkFileContents(
            """{"type":"paywalls","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","version":1,"type":"paywall_impression","app_user_id":"testAppUserId","session_id":"315107f4-98bf-4b68-a582-eb27bcb6e111","offering_id":"offeringID","paywall_id":"paywallID","paywall_revision":5,"timestamp":1699270688884,"display_mode":"footer","dark_mode":true,"locale":"es_ES"}}""".trimIndent() + "\n"
        )

        eventsManager.track(paywallEvent.copy(type = PaywallEventType.CANCEL))
        checkFileContents(
            """{"type":"paywalls","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","version":1,"type":"paywall_impression","app_user_id":"testAppUserId","session_id":"315107f4-98bf-4b68-a582-eb27bcb6e111","offering_id":"offeringID","paywall_id":"paywallID","paywall_revision":5,"timestamp":1699270688884,"display_mode":"footer","dark_mode":true,"locale":"es_ES"}}""".trimIndent()
                + "\n"
                + """{"type":"paywalls","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","version":1,"type":"paywall_cancel","app_user_id":"testAppUserId","session_id":"315107f4-98bf-4b68-a582-eb27bcb6e111","offering_id":"offeringID","paywall_id":"paywallID","paywall_revision":5,"timestamp":1699270688884,"display_mode":"footer","dark_mode":true,"locale":"es_ES"}}""".trimIndent()
                + "\n"
        )
    }

    /**
     * We should remove this test once we support the purchase initiated event in the backend.
     */
    @Test
    fun `tracking paywall purchase initiated event does not add it to file`() {
        eventsManager.track(paywallEvent.copy(type = PaywallEventType.PURCHASE_INITIATED))

        checkFileExists(shouldExist = false)
    }

    /**
     * We should remove this test once we support the purchase error event in the backend.
     */
    @Test
    fun `tracking paywall purchase error event does not add it to file`() {
        eventsManager.track(paywallEvent.copy(type = PaywallEventType.PURCHASE_ERROR))

        checkFileExists(shouldExist = false)
    }

    @Test
    fun `tracking mixed events adds them to file`() {
        eventsManager.track(customerCenterImpressionEvent)
        eventsManager.track(paywallEvent)
        checkFileContents(
            """{"type":"customer_center","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","revision_id":1,"type":"customer_center_impression","app_user_id":"testAppUserId","app_session_id":"${appSessionID}","timestamp":1699270688884,"dark_mode":true,"locale":"es_ES","display_mode":"full_screen"}}""".trimIndent()
                + "\n"
                + """{"type":"paywalls","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","version":1,"type":"paywall_impression","app_user_id":"testAppUserId","session_id":"315107f4-98bf-4b68-a582-eb27bcb6e111","offering_id":"offeringID","paywall_id":"paywallID","paywall_revision":5,"timestamp":1699270688884,"display_mode":"footer","dark_mode":true,"locale":"es_ES"}}""".trimIndent()
                + "\n"
        )
    }

    @Test
    fun `tracking customer center events adds them to file`() {
        eventsManager.track(customerCenterImpressionEvent)

        checkFileContents(
            """{"type":"customer_center","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","revision_id":1,"type":"customer_center_impression","app_user_id":"testAppUserId","app_session_id":"${appSessionID}","timestamp":1699270688884,"dark_mode":true,"locale":"es_ES","display_mode":"full_screen"}}""".trimIndent() + "\n"
        )

        val surveyEvent = CustomerCenterSurveyOptionChosenEvent(
            creationData = CustomerCenterSurveyOptionChosenEvent.CreationData(
                id = UUID.fromString("298207f4-87af-4b57-a581-eb27bcc6e009"),
                date = Date(1699270688884)
            ),
            data = CustomerCenterSurveyOptionChosenEvent.Data(
                timestamp = Date(1699270688884),
                darkMode = true,
                locale = "es_ES",
                path = CustomerCenterConfigData.HelpPath.PathType.CANCEL,
                url = "PATH2",
                surveyOptionID = "surveyOptionID"
            )
        )
        eventsManager.track(surveyEvent)
        checkFileContents(
            """{"type":"customer_center","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","revision_id":1,"type":"customer_center_impression","app_user_id":"testAppUserId","app_session_id":"${appSessionID}","timestamp":1699270688884,"dark_mode":true,"locale":"es_ES","display_mode":"full_screen"}}""".trimIndent()
                + "\n"
                + """{"type":"customer_center","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","revision_id":1,"type":"customer_center_survey_option_chosen","app_user_id":"testAppUserId","app_session_id":"${appSessionID}","timestamp":1699270688884,"dark_mode":true,"locale":"es_ES","display_mode":"full_screen","path":"CANCEL","url":"PATH2","survey_option_id":"surveyOptionID"}}""".trimIndent()
                + "\n"
        )
    }

    @Test
    fun `flushEvents sends available events to backend`() {
        mockBackendResponse(success = true)
        eventsManager.track(paywallEvent)
        eventsManager.track(customerCenterImpressionEvent)
        eventsManager.flushEvents()
        checkFileContents("")
        val expectedRequest = EventsRequest(
            listOf(
                BackendStoredEvent.Paywalls(
                    paywallStoredEvent.toBackendEvent()
                ),
                BackendStoredEvent.CustomerCenter(
                    customerCenterImpressionEvent
                        .toBackendStoredEvent(
                            appUserID = userID.toString(),
                            appSessionID = appSessionID.toString(),
                        )
                        .toBackendEvent() as BackendEvent.CustomerCenter
                )
            ).map { it.toBackendEvent() }
        )
        verify(exactly = 1) {
            backend.postEvents(
                expectedRequest,
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    @Test
    fun `flushEvents without events, does not call backend`() {
        eventsManager.flushEvents()
        verify(exactly = 0) {
            backend.postEvents(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `if more than maximum events flushEvents, posts maximum events per batch`() {
        mockBackendResponse(success = true)
        for (i in 0..99) {
            eventsManager.track(paywallEvent)
        }
        checkFileNumberOfEvents(100)
        eventsManager.flushEvents()
        // With multi-batch flushing, all 100 events will be flushed (2 batches of 50)
        checkFileNumberOfEvents(0)
        // Verify backend was called twice (once for each batch)
        verify(exactly = 2) {
            backend.postEvents(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `if backend errors without marking events as synced, events are not deleted`() {
        mockBackendResponse(success = false, shouldMarkAsSyncedOnError = false)
        for (i in 0..99) {
            eventsManager.track(paywallEvent)
        }
        checkFileNumberOfEvents(100)
        eventsManager.flushEvents()
        checkFileNumberOfEvents(100)
    }

    @Test
    fun `if backend errors but marking events as synced, events are deleted`() {
        mockBackendResponse(success = false, shouldMarkAsSyncedOnError = true)
        for (i in 0..99) {
            eventsManager.track(paywallEvent)
        }
        checkFileNumberOfEvents(100)
        eventsManager.flushEvents()
        checkFileNumberOfEvents(50)
    }

    @Test
    fun `flushEvents multiple times only executes once`() {
        every {
            backend.postEvents(any(), any(), any(), any(), any())
        } just Runs
        eventsManager.track(paywallEvent)
        eventsManager.track(paywallEvent)
        eventsManager.flushEvents()
        eventsManager.flushEvents()
        eventsManager.flushEvents()
        verify(exactly = 1) {
            backend.postEvents(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `flushEvents multiple times, then finishing, adding events and flushing again works`() {
        val successSlot = slot<() -> Unit>()
        every {
            backend.postEvents(any(), any(), any(), capture(successSlot), any())
        } just Runs
        eventsManager.track(paywallEvent)
        eventsManager.track(paywallEvent)
        eventsManager.flushEvents()
        eventsManager.flushEvents()
        eventsManager.flushEvents()
        verify(exactly = 1) {
            backend.postEvents(any(), any(), any(), any(), any())
        }
        successSlot.captured()
        checkFileContents("")
        eventsManager.track(paywallEvent)
        eventsManager.track(paywallEvent)
        eventsManager.track(paywallEvent)
        eventsManager.flushEvents()
        eventsManager.flushEvents()
        verify(exactly = 2) {
            backend.postEvents(any(), any(), any(), any(), any())
        }
        successSlot.captured()
        checkFileContents("")
    }

    @Test
    fun `flushEvents with invalid events, flushes valid events`() {
        mockBackendResponse(success = true)
        eventsManager.track(paywallEvent)
        appendToFile("invalid event\n")
        eventsManager.track(paywallEvent)
        appendToFile("invalid event 2\n")
        checkFileNumberOfEvents(4)
        eventsManager.flushEvents()
        checkFileNumberOfEvents(0)
        expectNumberOfEventsSynced(2)
    }

    @Test
    fun `flushEvents with invalid events, flushes valid events when reaching max count per request`() {
        mockBackendResponse(success = true)
        for (i in 0..24) {
            eventsManager.track(paywallEvent)
        }
        appendToFile("invalid event\n")
        appendToFile("invalid event 2\n")
        for (i in 0..49) {
            eventsManager.track(paywallEvent)
        }
        appendToFile("invalid event 3\n")
        checkFileNumberOfEvents(78)
        eventsManager.flushEvents()
        // With multi-batch flushing, all events will be flushed across 2 batches
        checkFileNumberOfEvents(0)
        // Verify backend was called twice (once for each batch of 50 lines)
        verify(exactly = 2) {
            backend.postEvents(any(), any(), any(), any(), any())
        }
    }

    @SuppressLint("CheckResult")
    @Test
    fun `flushEvents with events stored with removed keys from model, successfully flushes them`() {
        mockBackendResponse(success = true)
        eventsManager.track(paywallEvent)
        appendToFile(
            """{"type":"customer_center","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","revision_id":1,"type":"customer_center_survey_option_chosen","app_user_id":"testAppUserId","app_session_id":"${appSessionID}","timestamp":1699270688884,"dark_mode":true,"locale":"es_ES","display_mode":"full_screen","path":"CANCEL","url":"PATH2","survey_option_id":"surveyOptionID","survey_option_title_key":"surveyOptionTitleKey"}}""".trimIndent()
        )
        eventsManager.flushEvents()
        assertThat(postedRequest?.events?.count()  == 2)
    }

    private fun checkFileNumberOfEvents(expectedNumberOfEvents: Int) {
        val file = File(testFolder, EventsManager.EVENTS_FILE_PATH_NEW)
        assertThat(file.readLines().size).isEqualTo(expectedNumberOfEvents)
    }

    private fun checkFileContents(expectedContents: String) {
        val file = File(testFolder, EventsManager.EVENTS_FILE_PATH_NEW)
        assertThat(file.readText()).isEqualTo(expectedContents)
    }

    private fun checkFileContentsAndReturn(): String {
        val file = File(testFolder, EventsManager.EVENTS_FILE_PATH_NEW)
        return file.readText()
    }

    private fun checkFileExists(shouldExist: Boolean) {
        val file = File(testFolder, EventsManager.EVENTS_FILE_PATH_NEW)
        assertThat(file.exists()).isEqualTo(shouldExist)
    }

    private fun mockBackendResponse(
        success: Boolean,
        shouldMarkAsSyncedOnError: Boolean = false,
        underlyingErrorMessage: String? = null,
    ) {
        val successSlot = slot<() -> Unit>()
        val errorSlot = slot<(PurchasesError, Boolean) -> Unit>()
        every {
            backend.postEvents(any(), any(), any(), capture(successSlot), capture(errorSlot))
        } answers {
            if (success) {
                successSlot.captured.invoke()
            } else {
                errorSlot.captured.invoke(
                    PurchasesError(PurchasesErrorCode.UnknownError, underlyingErrorMessage),
                    shouldMarkAsSyncedOnError,
                )
            }
        }
    }

    private fun expectNumberOfEventsSynced(eventsSynced: Int) {
        val expectedRequest = EventsRequest(
            List(eventsSynced) { BackendStoredEvent.Paywalls(paywallStoredEvent.toBackendEvent()) }.map { it.toBackendEvent() }
        )
        verify(exactly = 1) {
            backend.postEvents(
                expectedRequest,
                any(),
                any(),
                any(),
                any(),
            )
        }
    }

    private fun appendToFile(contents: String) {
        val file = File(testFolder, EventsManager.EVENTS_FILE_PATH_NEW)
        file.appendText(contents)
    }

    @Test
    fun `tracking a WorkflowEvent stores BackendStoredEvent_Workflows`() {
        val event = WorkflowEvent.StepStarted(
            creationData = WorkflowEvent.CreationData(UUID.randomUUID(), Date()),
            workflowId = "wfl_abc",
            stepId = "step-1",
            workflowType = "paywall",
            stepType = "screen",
            entryReason = "start",
            isFirstStep = true,
        )

        eventsManager.track(event)

        val storedContent = checkFileContentsAndReturn()
        assertThat(storedContent).contains("\"type\":\"workflows\"")
        assertThat(storedContent).contains("\"workflow_id\":\"wfl_abc\"")
        assertThat(storedContent).contains("workflows_step_started")
    }

    // Ad Events Tests

    @Test
    fun `tracking ad displayed events adds them to file`() {
        val adEvent = AdEvent.Displayed(
            id = "ad-event-id-123",
            timestamp = 1699270688884,
            networkName = "Google AdMob",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.BANNER,
            placement = "banner_home",
            adUnitId = "ca-app-pub-123456",
            impressionId = "impression-123"
        )

        eventsManager.track(adEvent)

        checkFileContents(
            """{"type":"ad","event":{"id":"ad-event-id-123","version":1,"type":"rc_ads_ad_displayed","timestamp_ms":1699270688884,"network_name":"Google AdMob","mediator_name":"AdMob","ad_format":"banner","placement":"banner_home","ad_unit_id":"ca-app-pub-123456","impression_id":"impression-123","app_user_id":"testAppUserId","app_session_id":"${appSessionID}"}}""".trimIndent() + "\n"
        )
    }

    @Test
    fun `tracking ad displayed event with null placement adds to file`() {
        val adEvent = AdEvent.Displayed(
            id = "ad-event-id-123",
            timestamp = 1699270688884,
            networkName = "Google AdMob",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.INTERSTITIAL,
            placement = null,
            adUnitId = "ca-app-pub-123456",
            impressionId = "impression-123"
        )

        eventsManager.track(adEvent)

        checkFileContents(
            """{"type":"ad","event":{"id":"ad-event-id-123","version":1,"type":"rc_ads_ad_displayed","timestamp_ms":1699270688884,"network_name":"Google AdMob","mediator_name":"AdMob","ad_format":"interstitial","ad_unit_id":"ca-app-pub-123456","impression_id":"impression-123","app_user_id":"testAppUserId","app_session_id":"${appSessionID}"}}""".trimIndent() + "\n"
        )
    }

    @Test
    fun `tracking ad opened events adds them to file`() {
        val adEvent = AdEvent.Open(
            id = "ad-event-id-456",
            timestamp = 1699270688885,
            networkName = "AppLovin",
            mediatorName = AdMediatorName.APP_LOVIN,
            adFormat = AdFormat.NATIVE,
            placement = "interstitial",
            adUnitId = "ad-unit-789",
            impressionId = "impression-456"
        )

        eventsManager.track(adEvent)

        checkFileContents(
            """{"type":"ad","event":{"id":"ad-event-id-456","version":1,"type":"rc_ads_ad_opened","timestamp_ms":1699270688885,"network_name":"AppLovin","mediator_name":"AppLovin","ad_format":"native","placement":"interstitial","ad_unit_id":"ad-unit-789","impression_id":"impression-456","app_user_id":"testAppUserId","app_session_id":"${appSessionID}"}}""".trimIndent() + "\n"
        )
    }

    @Test
    fun `tracking ad revenue events adds them to file`() {
        val adEvent = AdEvent.Revenue(
            id = "ad-event-id-789",
            timestamp = 1699270688886,
            networkName = "Google AdMob",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.REWARDED,
            placement = "rewarded_video",
            adUnitId = "ad-unit-999",
            impressionId = "impression-789",
            revenueMicros = 1500000,
            currency = "USD",
            precision = AdRevenuePrecision.EXACT
        )

        eventsManager.track(adEvent)

        checkFileContents(
            """{"type":"ad","event":{"id":"ad-event-id-789","version":1,"type":"rc_ads_ad_revenue","timestamp_ms":1699270688886,"network_name":"Google AdMob","mediator_name":"AdMob","ad_format":"rewarded","placement":"rewarded_video","ad_unit_id":"ad-unit-999","impression_id":"impression-789","app_user_id":"testAppUserId","app_session_id":"${appSessionID}","revenue_micros":1500000,"currency":"USD","precision":"exact"}}""".trimIndent() + "\n"
        )
    }

    @Test
    fun `tracking ad loaded events adds them to file`() {
        val adEvent = AdEvent.Loaded(
            id = "ad-event-id-789",
            timestamp = 1699270688886,
            networkName = "Google AdMob",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.INTERSTITIAL,
            placement = "rewarded_video",
            adUnitId = "ad-unit-999",
            impressionId = "impression-789",
        )

        eventsManager.track(adEvent)

        checkFileContents(
            """{"type":"ad","event":{"id":"ad-event-id-789","version":1,"type":"rc_ads_ad_loaded","timestamp_ms":1699270688886,"network_name":"Google AdMob","mediator_name":"AdMob","ad_format":"interstitial","placement":"rewarded_video","ad_unit_id":"ad-unit-999","impression_id":"impression-789","app_user_id":"testAppUserId","app_session_id":"${appSessionID}"}}""".trimIndent() + "\n"
        )
    }

    @Test
    fun `tracking ad failed to load events adds them to file`() {
        val adEvent = AdEvent.FailedToLoad(
            id = "ad-event-id-789",
            timestamp = 1699270688886,
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.BANNER,
            placement = "rewarded_video",
            adUnitId = "ad-unit-999",
            mediatorErrorCode = 123,
        )

        eventsManager.track(adEvent)

        checkFileContents(
            """{"type":"ad","event":{"id":"ad-event-id-789","version":1,"type":"rc_ads_ad_failed_to_load","timestamp_ms":1699270688886,"mediator_name":"AdMob","ad_format":"banner","placement":"rewarded_video","ad_unit_id":"ad-unit-999","app_user_id":"testAppUserId","app_session_id":"${appSessionID}","mediator_error_code":123}}""".trimIndent() + "\n"
        )
    }


    @Test
    fun `tracking mixed events with ad events adds them to file`() {
        val adEvent = AdEvent.Displayed(
            id = "ad-event-id-123",
            timestamp = 1699270688884,
            networkName = "Google AdMob",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.BANNER,
            placement = "banner_home",
            adUnitId = "ca-app-pub-123456",
            impressionId = "impression-123"
        )

        eventsManager.track(paywallEvent)
        eventsManager.track(adEvent)
        eventsManager.track(customerCenterImpressionEvent)

        checkFileContents(
            """{"type":"paywalls","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","version":1,"type":"paywall_impression","app_user_id":"testAppUserId","session_id":"315107f4-98bf-4b68-a582-eb27bcb6e111","offering_id":"offeringID","paywall_id":"paywallID","paywall_revision":5,"timestamp":1699270688884,"display_mode":"footer","dark_mode":true,"locale":"es_ES"}}""".trimIndent()
                + "\n"
                + """{"type":"ad","event":{"id":"ad-event-id-123","version":1,"type":"rc_ads_ad_displayed","timestamp_ms":1699270688884,"network_name":"Google AdMob","mediator_name":"AdMob","ad_format":"banner","placement":"banner_home","ad_unit_id":"ca-app-pub-123456","impression_id":"impression-123","app_user_id":"testAppUserId","app_session_id":"${appSessionID}"}}""".trimIndent()
                + "\n"
                + """{"type":"customer_center","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","revision_id":1,"type":"customer_center_impression","app_user_id":"testAppUserId","app_session_id":"${appSessionID}","timestamp":1699270688884,"dark_mode":true,"locale":"es_ES","display_mode":"full_screen"}}""".trimIndent()
                + "\n"
        )
    }

    @Test
    fun `tracking custom paywall impression event adds it to file with appSessionID`() {
        val fixedId = UUID.fromString("298207f4-87af-4b57-a581-eb27bcc6e009")
        val fixedDate = Date(1699270688884)
        val customPaywallImpressionEvent = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallEvent.Impression.Data(paywallId = "my-paywall"),
        )

        eventsManager.track(customPaywallImpressionEvent)

        checkFileContents(
            """{"type":"custom_paywall_event","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","version":1,"type":"custom_paywall_impression","app_user_id":"testAppUserId","app_session_id":"${appSessionID}","timestamp":1699270688884,"paywall_id":"my-paywall"}}""".trimIndent() + "\n",
        )
    }

    @Test
    fun `tracking custom paywall impression event with null paywallId adds it to file`() {
        val fixedId = UUID.fromString("298207f4-87af-4b57-a581-eb27bcc6e009")
        val fixedDate = Date(1699270688884)
        val customPaywallImpressionEvent = CustomPaywallEvent.Impression(
            creationData = CustomPaywallEvent.Impression.CreationData(
                id = fixedId,
                date = fixedDate,
            ),
            data = CustomPaywallEvent.Impression.Data(paywallId = null),
        )

        eventsManager.track(customPaywallImpressionEvent)

        checkFileContents(
            """{"type":"custom_paywall_event","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","version":1,"type":"custom_paywall_impression","app_user_id":"testAppUserId","app_session_id":"${appSessionID}","timestamp":1699270688884}}""".trimIndent() + "\n",
        )
    }

    @Test
    fun `flushEvents sends ad events to backend`() {
        mockBackendResponse(success = true)
        val adEvent = AdEvent.Displayed(
            id = "ad-event-id-123",
            timestamp = 1699270688884,
            networkName = "Google AdMob",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.BANNER,
            placement = "banner_home",
            adUnitId = "ca-app-pub-123456",
            impressionId = "impression-123"
        )

        eventsManager.track(adEvent)
        eventsManager.flushEvents()

        checkFileContents("")
        verify(exactly = 1) {
            backend.postEvents(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `tracking multiple ad event types adds them to file`() {
        val displayedEvent = AdEvent.Displayed(
            id = "ad-event-id-1",
            timestamp = 1699270688884,
            networkName = "Google AdMob",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.BANNER,
            placement = "banner",
            adUnitId = "ad-unit-1",
            impressionId = "impression-1"
        )

        val openedEvent = AdEvent.Open(
            id = "ad-event-id-2",
            timestamp = 1699270688885,
            networkName = "AppLovin",
            mediatorName = AdMediatorName.APP_LOVIN,
            adFormat = AdFormat.INTERSTITIAL,
            placement = "interstitial",
            adUnitId = "ad-unit-2",
            impressionId = "impression-2"
        )

        val revenueEvent = AdEvent.Revenue(
            id = "ad-event-id-3",
            timestamp = 1699270688886,
            networkName = "Google AdMob",
            mediatorName = AdMediatorName.AD_MOB,
            adFormat = AdFormat.REWARDED,
            placement = "rewarded",
            adUnitId = "ad-unit-3",
            impressionId = "impression-3",
            revenueMicros = 2000000,
            currency = "EUR",
            precision = AdRevenuePrecision.ESTIMATED
        )

        eventsManager.track(displayedEvent)
        eventsManager.track(openedEvent)
        eventsManager.track(revenueEvent)

        checkFileNumberOfEvents(3)
    }

    @Test
    fun `flushEvents stops after maximum batch limit`() {
        mockBackendResponse(success = true)
        // Add 550 events (more than 10 batches of 50)
        for (i in 0..549) {
            eventsManager.track(paywallEvent)
        }
        checkFileNumberOfEvents(550)
        eventsManager.flushEvents()
        // Should flush 10 batches (500 events), leaving 50
        checkFileNumberOfEvents(50)
        // Verify backend was called exactly 10 times (the maximum)
        verify(exactly = 10) {
            backend.postEvents(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `flushEvents stops on first batch failure`() {
        // Mock backend to fail on first call
        var callCount = 0
        val successSlot = slot<() -> Unit>()
        val errorSlot = slot<(PurchasesError, Boolean) -> Unit>()
        every {
            backend.postEvents(any(), any(), any(), capture(successSlot), capture(errorSlot))
        } answers {
            callCount++
            if (callCount == 1) {
                // First batch fails
                errorSlot.captured.invoke(PurchasesError(PurchasesErrorCode.UnknownError), false)
            } else {
                // Subsequent batches succeed (but shouldn't be called)
                successSlot.captured.invoke()
            }
        }

        // Add 150 events (3 potential batches)
        for (i in 0..149) {
            eventsManager.track(paywallEvent)
        }
        checkFileNumberOfEvents(150)
        eventsManager.flushEvents()
        // Events should not be deleted since first batch failed
        checkFileNumberOfEvents(150)
        // Verify backend was called only once (stopped after first failure)
        verify(exactly = 1) {
            backend.postEvents(any(), any(), any(), any(), any())
        }
    }

    // File Size Limit Tests

    @Test
    fun `file size limit is not reached with small number of events`() {
        // Track a small number of events that won't reach the 2048KB limit
        for (i in 0..9) {
            eventsManager.track(paywallEvent)
        }
        checkFileNumberOfEvents(10)
    }

    @Test
    fun `file size limit clears oldest events when limit is reached`() {
        // Each event is approximately 300 bytes when serialized
        // To reach 2048KB (2097152 bytes), we need about 7000 events
        // Let's track 7100 events to exceed the limit
        val eventsToTrack = 7100

        for (i in 0 until eventsToTrack) {
            eventsManager.track(paywallEvent)
        }

        val file = File(testFolder, EventsManager.EVENTS_FILE_PATH_NEW)
        val finalEventCount = file.readLines().size

        // The file should have fewer events than we tracked because
        // the oldest 50 events were cleared each time the limit was reached
        assertThat(finalEventCount).isLessThan(eventsToTrack)
        assertThat(finalEventCount).isGreaterThan(5000) // Should still have a significant number of events

        // Verify file size is under the limit (2048KB)
        val fileSizeKB = file.length() / 1024.0
        assertThat(fileSizeKB).isLessThan(EventsManager.FILE_SIZE_LIMIT_KB)
    }

    @Test
    fun `file size limit works with mixed event types`() {
        // Track a mix of events to exceed the limit
        val eventsToTrack = 2500

        for (i in 0 until eventsToTrack) {
            when (i % 3) {
                0 -> eventsManager.track(paywallEvent)
                1 -> eventsManager.track(customerCenterImpressionEvent)
                2 -> eventsManager.track(adEvent)
            }
        }

        val file = File(testFolder, EventsManager.EVENTS_FILE_PATH_NEW)
        val fileSizeKB = file.length() / 1024.0

        // Verify file size is under the limit
        assertThat(fileSizeKB).isLessThan(EventsManager.FILE_SIZE_LIMIT_KB)
    }

    @Test
    fun `oldest events are cleared when limit is reached maintaining file integrity`() {
        // Track enough events to trigger the size check multiple times
        // With ~1.3KB per event, we need about 1600 events to reach the limit
        for (i in 0 until 1700) {
            eventsManager.track(paywallEvent.copy(
                data = paywallEvent.data.copy(
                    presentedOfferingContext = PresentedOfferingContext("offeringID_${i}_" + "x".repeat(1000)),
                )
            ))
        }

        val file = File(testFolder, EventsManager.EVENTS_FILE_PATH_NEW)
        val fileSizeKB = file.length() / 1024.0

        // Verify file size is under the limit
        assertThat(fileSizeKB).isLessThan(EventsManager.FILE_SIZE_LIMIT_KB)

        val lines = file.readLines()
        val firstLine = lines.first()
        // Assert that the first line has a more recent offeringIdentifier, indicating older events were cleared
        val firstOfferingID = firstLine.substringAfter("offeringID_").substringBefore("_").toInt()
        assertThat(firstOfferingID).isGreaterThan(50) // At least one clearing should have occurred

        // Verify all remaining events are left as expected
        lines.forEach { line ->
            // Should not throw exception
            assertThat(line).startsWith("{\"type\":")
        }
    }

    // Debug Event Listener Tests

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `debugEventListener is notified when file size limit is reached`() {
        val receivedEvents = mutableListOf<DebugEvent>()
        eventsManager.debugEventListener = DebugEventListener { receivedEvents.add(it) }

        // Track enough events to trigger the file size limit
        for (i in 0 until 7100) {
            eventsManager.track(paywallEvent)
        }

        assertThat(receivedEvents).isNotEmpty
        assertThat(receivedEvents.all { it.name == DebugEventName.FILE_SIZE_LIMIT_REACHED }).isTrue
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `debugEventListener is notified on flush error`() {
        val receivedEvents = mutableListOf<DebugEvent>()
        eventsManager.debugEventListener = DebugEventListener { receivedEvents.add(it) }

        mockBackendResponse(
            success = false,
            shouldMarkAsSyncedOnError = false,
            underlyingErrorMessage = "Server returned 500",
        )
        eventsManager.track(paywallEvent)
        eventsManager.flushEvents()

        val flushErrors = receivedEvents.filter { it.name == DebugEventName.FLUSH_ERROR }
        assertThat(flushErrors).hasSize(1)
        assertThat(flushErrors.first().properties["errorCode"]).isEqualTo(PurchasesErrorCode.UnknownError.name)
        assertThat(flushErrors.first().properties["underlyingErrorMessage"]).isEqualTo("Server returned 500")
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `debugEventListener flush error truncates underlyingErrorMessage to 80 chars`() {
        val receivedEvents = mutableListOf<DebugEvent>()
        eventsManager.debugEventListener = DebugEventListener { receivedEvents.add(it) }

        val longMessage = "A".repeat(120)
        mockBackendResponse(
            success = false,
            shouldMarkAsSyncedOnError = false,
            underlyingErrorMessage = longMessage,
        )
        eventsManager.track(paywallEvent)
        eventsManager.flushEvents()

        val flushErrors = receivedEvents.filter { it.name == DebugEventName.FLUSH_ERROR }
        assertThat(flushErrors).hasSize(1)
        assertThat(flushErrors.first().properties["underlyingErrorMessage"]).hasSize(80)
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `debugEventListener flush error omits underlyingErrorMessage when null`() {
        val receivedEvents = mutableListOf<DebugEvent>()
        eventsManager.debugEventListener = DebugEventListener { receivedEvents.add(it) }

        mockBackendResponse(success = false, shouldMarkAsSyncedOnError = false)
        eventsManager.track(paywallEvent)
        eventsManager.flushEvents()

        val flushErrors = receivedEvents.filter { it.name == DebugEventName.FLUSH_ERROR }
        assertThat(flushErrors).hasSize(1)
        assertThat(flushErrors.first().properties).doesNotContainKey("underlyingErrorMessage")
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `debugEventListener is not notified when no error occurs`() {
        val receivedEvents = mutableListOf<DebugEvent>()
        eventsManager.debugEventListener = DebugEventListener { receivedEvents.add(it) }

        mockBackendResponse(success = true)
        eventsManager.track(paywallEvent)
        eventsManager.flushEvents()

        val flushErrors = receivedEvents.filter { it.name == DebugEventName.FLUSH_ERROR }
        assertThat(flushErrors).isEmpty()
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `debugEventListener is notified with FLUSH_COMPLETED on successful flush`() {
        val receivedEvents = mutableListOf<DebugEvent>()
        eventsManager.debugEventListener = DebugEventListener { receivedEvents.add(it) }

        mockBackendResponse(success = true)
        eventsManager.track(paywallEvent)
        eventsManager.flushEvents()

        val flushCompleted = receivedEvents.filter { it.name == DebugEventName.FLUSH_COMPLETED }
        assertThat(flushCompleted).hasSize(1)
        assertThat(flushCompleted.first().properties["batch_number"]).isEqualTo("1")
        assertThat(flushCompleted.first().properties["elapsed_millis"]).isNotNull()
        assertThat(flushCompleted.first().properties["elapsed_millis"]!!.toLong()).isGreaterThanOrEqualTo(0)
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `debugEventListener receives FLUSH_COMPLETED for each batch with correct batch_number`() {
        val receivedEvents = mutableListOf<DebugEvent>()
        eventsManager.debugEventListener = DebugEventListener { receivedEvents.add(it) }

        mockBackendResponse(success = true)
        for (i in 0..99) {
            eventsManager.track(paywallEvent)
        }
        eventsManager.flushEvents()

        val flushCompleted = receivedEvents.filter { it.name == DebugEventName.FLUSH_COMPLETED }
        assertThat(flushCompleted).hasSize(2)
        assertThat(flushCompleted[0].properties["batch_number"]).isEqualTo("1")
        assertThat(flushCompleted[0].properties["elapsed_millis"]).isNotNull()
        assertThat(flushCompleted[1].properties["batch_number"]).isEqualTo("2")
        assertThat(flushCompleted[1].properties["elapsed_millis"]).isNotNull()
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `debugEventListener receives FLUSH_SKIPPED_NO_EVENTS when no events to flush`() {
        val receivedEvents = mutableListOf<DebugEvent>()
        eventsManager.debugEventListener = DebugEventListener { receivedEvents.add(it) }

        eventsManager.flushEvents()

        val skippedEvents = receivedEvents.filter { it.name == DebugEventName.FLUSH_SKIPPED_NO_EVENTS }
        assertThat(skippedEvents).hasSize(1)
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `debugEventListener does not receive FLUSH_SKIPPED_NO_EVENTS when events are flushed`() {
        val receivedEvents = mutableListOf<DebugEvent>()
        eventsManager.debugEventListener = DebugEventListener { receivedEvents.add(it) }

        mockBackendResponse(success = true)
        eventsManager.track(paywallEvent)
        eventsManager.flushEvents()

        val skippedEvents = receivedEvents.filter { it.name == DebugEventName.FLUSH_SKIPPED_NO_EVENTS }
        assertThat(skippedEvents).isEmpty()
    }

    // Priority Flush Tests

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `isPriorityEvent returns true for paywall impression event`() {
        val impressionEvent = paywallEvent.copy(type = PaywallEventType.IMPRESSION)
        assertThat(impressionEvent.isPriorityEvent).isTrue()
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `isPriorityEvent returns false for non-impression paywall events`() {
        val cancelEvent = paywallEvent.copy(type = PaywallEventType.CANCEL)
        assertThat(cancelEvent.isPriorityEvent).isFalse()

        val closeEvent = paywallEvent.copy(type = PaywallEventType.CLOSE)
        assertThat(closeEvent.isPriorityEvent).isFalse()
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `isPriorityEvent returns true for custom paywall events`() {
        val customEvent = CustomPaywallEvent.Impression(
            data = CustomPaywallEvent.Impression.Data(paywallId = "test"),
        )
        assertThat(customEvent.isPriorityEvent).isTrue()
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `isPriorityEvent returns false for customer center events`() {
        assertThat(customerCenterImpressionEvent.isPriorityEvent).isFalse()
    }

    @Test
    fun `tracking priority event triggers immediate flush`() {
        val enabledRateLimiter = RateLimiter(maxCallsInPeriod = 5, periodSeconds = 60.seconds)
        val priorityEventsManager = EventsManager(
            appSessionID,
            legacyFileHelper,
            fileHelper,
            identityManager,
            paywallEventsDispatcher,
            postEvents = { request, delay, onSuccess, onError ->
                postedRequest = request
                backend.postEvents(
                    paywallEventRequest = request,
                    baseURL = AppConfig.paywallEventsURL,
                    delay = delay,
                    onSuccessHandler = onSuccess,
                    onErrorHandler = onError,
                )
            },
            priorityFlushRateLimiter = enabledRateLimiter,
        )

        mockBackendResponse(success = true)
        priorityEventsManager.track(paywallEvent.copy(type = PaywallEventType.IMPRESSION))

        // Should have triggered a flush automatically
        verify(exactly = 1) {
            backend.postEvents(any(), any(), any(), any(), any())
        }
        checkFileContents("")
    }

    @Test
    fun `tracking non-priority event does not trigger flush`() {
        val enabledRateLimiter = RateLimiter(maxCallsInPeriod = 5, periodSeconds = 60.seconds)
        val priorityEventsManager = EventsManager(
            appSessionID,
            legacyFileHelper,
            fileHelper,
            identityManager,
            paywallEventsDispatcher,
            postEvents = { request, delay, onSuccess, onError ->
                postedRequest = request
                backend.postEvents(
                    paywallEventRequest = request,
                    baseURL = AppConfig.paywallEventsURL,
                    delay = delay,
                    onSuccessHandler = onSuccess,
                    onErrorHandler = onError,
                )
            },
            priorityFlushRateLimiter = enabledRateLimiter,
        )

        priorityEventsManager.track(paywallEvent.copy(type = PaywallEventType.CANCEL))

        // Should not have triggered a flush
        verify(exactly = 0) {
            backend.postEvents(any(), any(), any(), any(), any())
        }
        checkFileNumberOfEvents(1)
    }

    @Test
    fun `priority flush is rate limited after exceeding max calls`() {
        val rateLimiter = RateLimiter(maxCallsInPeriod = 2, periodSeconds = 60.seconds)
        val priorityEventsManager = EventsManager(
            appSessionID,
            legacyFileHelper,
            fileHelper,
            identityManager,
            paywallEventsDispatcher,
            postEvents = { request, delay, onSuccess, onError ->
                postedRequest = request
                backend.postEvents(
                    paywallEventRequest = request,
                    baseURL = AppConfig.paywallEventsURL,
                    delay = delay,
                    onSuccessHandler = onSuccess,
                    onErrorHandler = onError,
                )
            },
            priorityFlushRateLimiter = rateLimiter,
        )

        mockBackendResponse(success = true)

        // First two priority events should trigger flushes
        priorityEventsManager.track(paywallEvent.copy(type = PaywallEventType.IMPRESSION))
        priorityEventsManager.track(paywallEvent.copy(type = PaywallEventType.IMPRESSION))

        // Third priority event should be rate limited (no additional flush)
        priorityEventsManager.track(paywallEvent.copy(type = PaywallEventType.IMPRESSION))

        // Only 2 flushes should have happened (rate limited the 3rd)
        // The 3rd event is stored but not flushed
        verify(exactly = 2) {
            backend.postEvents(any(), any(), any(), any(), any())
        }
        checkFileNumberOfEvents(1)
    }

    @Test
    fun `priority event during flush queues and drains after completion`() {
        val rateLimiter = RateLimiter(maxCallsInPeriod = 5, periodSeconds = 60.seconds)
        val priorityEventsManager = EventsManager(
            appSessionID,
            legacyFileHelper,
            fileHelper,
            identityManager,
            paywallEventsDispatcher,
            postEvents = { request, delay, onSuccess, onError ->
                postedRequest = request
                backend.postEvents(
                    paywallEventRequest = request,
                    baseURL = AppConfig.paywallEventsURL,
                    delay = delay,
                    onSuccessHandler = onSuccess,
                    onErrorHandler = onError,
                )
            },
            priorityFlushRateLimiter = rateLimiter,
        )

        // Mock backend to capture callbacks without immediately invoking them
        val successSlot = slot<() -> Unit>()
        every {
            backend.postEvents(any(), any(), any(), capture(successSlot), any())
        } just Runs

        // Track a priority event - starts flush, but backend doesn't complete yet
        priorityEventsManager.track(paywallEvent.copy(type = PaywallEventType.IMPRESSION))
        verify(exactly = 1) {
            backend.postEvents(any(), any(), any(), any(), any())
        }

        // Track another priority event while flush is in progress - should queue
        priorityEventsManager.track(paywallEvent.copy(type = PaywallEventType.IMPRESSION))
        // Still only 1 backend call since flush is in progress
        verify(exactly = 1) {
            backend.postEvents(any(), any(), any(), any(), any())
        }

        // Complete the first flush - should trigger the queued priority flush
        successSlot.captured()

        // Now the queued flush should have fired
        verify(exactly = 2) {
            backend.postEvents(any(), any(), any(), any(), any())
        }

        // Complete the second flush
        successSlot.captured()

        // All events should be flushed
        checkFileContents("")
    }

    @Test
    fun `queued priority events during flush do not exhaust rate limiter`() {
        val rateLimiter = RateLimiter(maxCallsInPeriod = 3, periodSeconds = 60.seconds)
        val priorityEventsManager = EventsManager(
            appSessionID,
            legacyFileHelper,
            fileHelper,
            identityManager,
            paywallEventsDispatcher,
            postEvents = { request, delay, onSuccess, onError ->
                postedRequest = request
                backend.postEvents(
                    paywallEventRequest = request,
                    baseURL = AppConfig.paywallEventsURL,
                    delay = delay,
                    onSuccessHandler = onSuccess,
                    onErrorHandler = onError,
                )
            },
            priorityFlushRateLimiter = rateLimiter,
        )

        // Mock backend to capture callbacks without immediately invoking them
        val successSlot = slot<() -> Unit>()
        every {
            backend.postEvents(any(), any(), any(), capture(successSlot), any())
        } just Runs

        // Track a priority event - starts flush, but backend doesn't complete yet
        priorityEventsManager.track(paywallEvent.copy(type = PaywallEventType.IMPRESSION))
        verify(exactly = 1) {
            backend.postEvents(any(), any(), any(), any(), any())
        }

        // Track 3 more priority events while flush is in progress
        // These should coalesce into a single pending flush, NOT exhaust the rate limiter
        for (i in 0..2) {
            priorityEventsManager.track(paywallEvent.copy(type = PaywallEventType.IMPRESSION))
        }

        // Still only 1 backend call since flush is in progress
        verify(exactly = 1) {
            backend.postEvents(any(), any(), any(), any(), any())
        }

        // Complete the first flush - should trigger ONE queued priority flush
        successSlot.captured()

        // Now the queued flush should have fired (consuming 1 rate limiter call)
        verify(exactly = 2) {
            backend.postEvents(any(), any(), any(), any(), any())
        }

        // Complete the second flush
        successSlot.captured()

        // Track another priority event — should still flush because rate limiter
        // was only consumed 2 times (initial flush + drain), NOT 4 times (once per queued event).
        // With maxCallsInPeriod = 3, this 3rd call should succeed.
        priorityEventsManager.track(paywallEvent.copy(type = PaywallEventType.IMPRESSION))

        // If rate limiter were exhausted by queued events (old bug), this would still be 2.
        // With the fix, it should be 3 since the rate limiter has capacity remaining.
        verify(exactly = 3) {
            backend.postEvents(any(), any(), any(), any(), any())
        }
    }
}
