package com.revenuecat.purchases.common.events

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.FileHelper
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.customercenter.events.CustomerCenterDisplayMode
import com.revenuecat.purchases.customercenter.events.CustomerCenterEvent
import com.revenuecat.purchases.customercenter.events.CustomerCenterEventType
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.paywalls.events.PaywallEvent
import com.revenuecat.purchases.paywalls.events.PaywallEventType
import com.revenuecat.purchases.paywalls.events.PaywallStoredEvent
import com.revenuecat.purchases.utils.EventsFileHelper
import com.revenuecat.purchases.utils.serializers.DateSerializer
import com.revenuecat.purchases.utils.serializers.UUIDSerializer
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.serialization.Serializable
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import java.util.Date
import java.util.UUID

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@RunWith(AndroidJUnit4::class)
class EventsManagerTest {

    private val userID = "testAppUserId"
    private val paywallEvent = PaywallEvent(
        creationData = PaywallEvent.CreationData(
            id = UUID.fromString("298207f4-87af-4b57-a581-eb27bcc6e009"),
            date = Date(1699270688884)
        ),
        data = PaywallEvent.Data(
            offeringIdentifier = "offeringID",
            paywallRevision = 5,
            sessionIdentifier = UUID.fromString("315107f4-98bf-4b68-a582-eb27bcb6e111"),
            displayMode = "footer",
            localeIdentifier = "es_ES",
            darkMode = true
        ),
        type = PaywallEventType.IMPRESSION,
    )
    private val customerCenterEvent = CustomerCenterEvent(
        creationData = CustomerCenterEvent.CreationData(
            id = UUID.fromString("298207f4-87af-4b57-a581-eb27bcc6e009"),
            date = Date(1699270688884)
        ),
        data = CustomerCenterEvent.Data(
            type = CustomerCenterEventType.IMPRESSION,
            timestamp = Date(1699270688884),
            sessionIdentifier = UUID.fromString("315107f4-98bf-4b68-a582-eb27bcb6e111"),
            darkMode = true,
            locale = "es_ES",
            isSandbox = true,
        )
    )
    private val storedEvent = PaywallStoredEvent(paywallEvent, userID)

    private val testFolder = "temp_test_folder"

    private lateinit var legacyFileHelper: EventsFileHelper<PaywallStoredEvent>
    private lateinit var fileHelper: EventsFileHelper<BackendStoredEvent>

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
            legacyFileHelper,
            fileHelper,
            identityManager,
            paywallEventsDispatcher,
            postEvents = { request, onSuccess, onError ->
                backend.postEvents(
                    paywallEventRequest = request,
                    onSuccessHandler = onSuccess,
                    onErrorHandler = onError,
                )
            },
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
            """{"type":"paywalls","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","version":1,"type":"paywall_impression","app_user_id":"testAppUserId","session_id":"315107f4-98bf-4b68-a582-eb27bcb6e111","offering_id":"offeringID","paywall_revision":5,"timestamp":1699270688884,"display_mode":"footer","dark_mode":true,"locale":"es_ES"}}""".trimIndent() + "\n"
        )

        eventsManager.track(paywallEvent.copy(type = PaywallEventType.CANCEL))
        checkFileContents(
            """{"type":"paywalls","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","version":1,"type":"paywall_impression","app_user_id":"testAppUserId","session_id":"315107f4-98bf-4b68-a582-eb27bcb6e111","offering_id":"offeringID","paywall_revision":5,"timestamp":1699270688884,"display_mode":"footer","dark_mode":true,"locale":"es_ES"}}""".trimIndent()
                + "\n"
                + """{"type":"paywalls","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","version":1,"type":"paywall_cancel","app_user_id":"testAppUserId","session_id":"315107f4-98bf-4b68-a582-eb27bcb6e111","offering_id":"offeringID","paywall_revision":5,"timestamp":1699270688884,"display_mode":"footer","dark_mode":true,"locale":"es_ES"}}""".trimIndent()
                + "\n"
        )
    }

    @Test
    fun `tracking mixed events adds them to file`() {
        eventsManager.track(customerCenterEvent)
        eventsManager.track(paywallEvent)
        checkFileContents(
            """{"type":"customer_center","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","revision_id":1,"type":"customer_center_impression","app_user_id":"testAppUserId","app_session_id":"315107f4-98bf-4b68-a582-eb27bcb6e111","timestamp":1699270688884,"dark_mode":true,"locale":"es_ES","is_sandbox":true,"display_mode":"full_screen"}}""".trimIndent()
                + "\n"
                + """{"type":"paywalls","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","version":1,"type":"paywall_impression","app_user_id":"testAppUserId","session_id":"315107f4-98bf-4b68-a582-eb27bcb6e111","offering_id":"offeringID","paywall_revision":5,"timestamp":1699270688884,"display_mode":"footer","dark_mode":true,"locale":"es_ES"}}""".trimIndent()
                + "\n"
        )
    }

    @Test
    fun `tracking customer center adds them to file`() {
        eventsManager.track(customerCenterEvent)

        checkFileContents(
            """{"type":"customer_center","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","revision_id":1,"type":"customer_center_impression","app_user_id":"testAppUserId","app_session_id":"315107f4-98bf-4b68-a582-eb27bcb6e111","timestamp":1699270688884,"dark_mode":true,"locale":"es_ES","is_sandbox":true,"display_mode":"full_screen"}}""".trimIndent() + "\n"
        )

        var surveyEvent = CustomerCenterEvent(
            creationData = customerCenterEvent.creationData,
            data = customerCenterEvent.data.copy(type = CustomerCenterEventType.SURVEY_OPTION_CHOSEN)
        )
        eventsManager.track(surveyEvent)
        checkFileContents(
            """{"type":"customer_center","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","revision_id":1,"type":"customer_center_impression","app_user_id":"testAppUserId","app_session_id":"315107f4-98bf-4b68-a582-eb27bcb6e111","timestamp":1699270688884,"dark_mode":true,"locale":"es_ES","is_sandbox":true,"display_mode":"full_screen"}}""".trimIndent()
                + "\n"
                + """{"type":"customer_center","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","revision_id":1,"type":"customer_center_survey_option_chosen","app_user_id":"testAppUserId","app_session_id":"315107f4-98bf-4b68-a582-eb27bcb6e111","timestamp":1699270688884,"dark_mode":true,"locale":"es_ES","is_sandbox":true,"display_mode":"full_screen"}}""".trimIndent()
                + "\n"
        )
    }

    @Test
    fun `flushEvents sends available events to backend`() {
        mockBackendResponse(success = true)
        eventsManager.track(paywallEvent)
        eventsManager.track(paywallEvent)
        eventsManager.flushEvents()
        checkFileContents("")
        val expectedRequest = EventsRequest(
            listOf(
                BackendStoredEvent.Paywalls(
                    storedEvent.toBackendEvent()
                ),
                BackendStoredEvent.Paywalls(
                    storedEvent.toBackendEvent()
                )
            ).mapNotNull { it.toBackendEvent() }
        )
        verify(exactly = 1) {
            backend.postEvents(
                expectedRequest,
                any(),
                any(),
            )
        }
    }

    @Test
    fun `flushEvents without events, does not call backend`() {
        eventsManager.flushEvents()
        verify(exactly = 0) {
            backend.postEvents(any(), any(), any())
        }
    }

    @Test
    fun `if more than maximum events flushEvents only posts maximum events`() {
        mockBackendResponse(success = true)
        for (i in 0..99) {
            eventsManager.track(paywallEvent)
        }
        checkFileNumberOfEvents(100)
        eventsManager.flushEvents()
        checkFileNumberOfEvents(50)
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
            backend.postEvents(any(), any(), any())
        } just Runs
        eventsManager.track(paywallEvent)
        eventsManager.track(paywallEvent)
        eventsManager.flushEvents()
        eventsManager.flushEvents()
        eventsManager.flushEvents()
        verify(exactly = 1) {
            backend.postEvents(any(), any(), any())
        }
    }

    @Test
    fun `flushEvents multiple times, then finishing, adding events and flushing again works`() {
        val successSlot = slot<() -> Unit>()
        every {
            backend.postEvents(any(), capture(successSlot), any())
        } just Runs
        eventsManager.track(paywallEvent)
        eventsManager.track(paywallEvent)
        eventsManager.flushEvents()
        eventsManager.flushEvents()
        eventsManager.flushEvents()
        verify(exactly = 1) {
            backend.postEvents(any(), any(), any())
        }
        successSlot.captured()
        checkFileContents("")
        eventsManager.track(paywallEvent)
        eventsManager.track(paywallEvent)
        eventsManager.track(paywallEvent)
        eventsManager.flushEvents()
        eventsManager.flushEvents()
        verify(exactly = 2) {
            backend.postEvents(any(), any(), any())
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
        checkFileNumberOfEvents(28)
        expectNumberOfEventsSynced(48)
    }


    private fun checkFileNumberOfEvents(expectedNumberOfEvents: Int) {
        val file = File(testFolder, EventsManager.EVENTS_FILE_PATH_NEW)
        assertThat(file.readLines().size).isEqualTo(expectedNumberOfEvents)
    }

    private fun checkFileContents(expectedContents: String) {
        val file = File(testFolder, EventsManager.EVENTS_FILE_PATH_NEW)
        assertThat(file.readText()).isEqualTo(expectedContents)
    }

    private fun mockBackendResponse(success: Boolean, shouldMarkAsSyncedOnError: Boolean = false) {
        val successSlot = slot<() -> Unit>()
        val errorSlot = slot<(PurchasesError, Boolean) -> Unit>()
        every {
            backend.postEvents(any(), capture(successSlot), capture(errorSlot))
        } answers {
            if (success) {
                successSlot.captured.invoke()
            } else {
                errorSlot.captured.invoke(PurchasesError(PurchasesErrorCode.UnknownError), shouldMarkAsSyncedOnError)
            }
        }
    }

    private fun expectNumberOfEventsSynced(eventsSynced: Int) {
        val expectedRequest = EventsRequest(
            List(eventsSynced) { BackendStoredEvent.Paywalls(storedEvent.toBackendEvent()) }.mapNotNull { it.toBackendEvent() }
        )
        verify(exactly = 1) {
            backend.postEvents(
                expectedRequest,
                any(),
                any(),
            )
        }
    }

    private fun appendToFile(contents: String) {
        val file = File(testFolder, EventsManager.EVENTS_FILE_PATH_NEW)
        file.appendText(contents)
    }
}
