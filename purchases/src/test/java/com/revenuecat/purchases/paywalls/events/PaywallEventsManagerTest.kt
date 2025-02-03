package com.revenuecat.purchases.paywalls.events

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesErrorCode
import com.revenuecat.purchases.common.Backend
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.FileHelper
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.common.events.FeatureEvent
import com.revenuecat.purchases.common.events.BackendEvent
import com.revenuecat.purchases.common.events.EventRequest
import com.revenuecat.purchases.common.events.EventsManager
import com.revenuecat.purchases.identity.IdentityManager
import com.revenuecat.purchases.utils.EventsFileHelper
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

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@RunWith(AndroidJUnit4::class)
class PaywallEventsManagerTest {

    private val userID = "testAppUserId"
    private val event = PaywallEvent(
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
    private val storedEvent = PaywallStoredEvent(event, userID)

    private val testFolder = "temp_test_folder"

    private lateinit var legacyFileHelper: EventsFileHelper<PaywallStoredEvent>
    private lateinit var fileHelper: EventsFileHelper<BackendEvent>

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
                backend.postPaywallEvents(
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
    fun `tracking events adds them to file`() {
        eventsManager.track(FeatureEvent.Paywall(event))

        checkFileContents(
            """{"type":"paywalls","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","version":1,"type":"paywall_impression","app_user_id":"testAppUserId","session_id":"315107f4-98bf-4b68-a582-eb27bcb6e111","offering_id":"offeringID","paywall_revision":5,"timestamp":1699270688884,"display_mode":"footer","dark_mode":true,"locale":"es_ES"}}""".trimIndent() + "\n"
        )

        eventsManager.track(FeatureEvent.Paywall(event.copy(type = PaywallEventType.CANCEL)))
        checkFileContents(
            """{"type":"paywalls","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","version":1,"type":"paywall_impression","app_user_id":"testAppUserId","session_id":"315107f4-98bf-4b68-a582-eb27bcb6e111","offering_id":"offeringID","paywall_revision":5,"timestamp":1699270688884,"display_mode":"footer","dark_mode":true,"locale":"es_ES"}}""".trimIndent()
                + "\n"
                + """{"type":"paywalls","event":{"id":"298207f4-87af-4b57-a581-eb27bcc6e009","version":1,"type":"paywall_cancel","app_user_id":"testAppUserId","session_id":"315107f4-98bf-4b68-a582-eb27bcb6e111","offering_id":"offeringID","paywall_revision":5,"timestamp":1699270688884,"display_mode":"footer","dark_mode":true,"locale":"es_ES"}}""".trimIndent()
                + "\n"
        )
    }

    @Test
    fun `flushEvents sends available events to backend`() {
        mockBackendResponse(success = true)
        eventsManager.track(FeatureEvent.Paywall(event))
        eventsManager.track(FeatureEvent.Paywall(event))
        eventsManager.flushEvents()
        checkFileContents("")
        val expectedRequest = EventRequest(
            listOf(
                BackendEvent.Paywalls(
                    storedEvent.toPaywallBackendEvent()
                ),
                BackendEvent.Paywalls(
                    storedEvent.toPaywallBackendEvent()
                )
            )
        )
        verify(exactly = 1) {
            backend.postPaywallEvents(
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
            backend.postPaywallEvents(any(), any(), any())
        }
    }

    @Test
    fun `if more than maximum events flushEvents only posts maximum events`() {
        mockBackendResponse(success = true)
        for (i in 0..99) {
            eventsManager.track(FeatureEvent.Paywall(event))
        }
        checkFileNumberOfEvents(100)
        eventsManager.flushEvents()
        checkFileNumberOfEvents(50)
    }

    @Test
    fun `if backend errors without marking events as synced, events are not deleted`() {
        mockBackendResponse(success = false, shouldMarkAsSyncedOnError = false)
        for (i in 0..99) {
            eventsManager.track(FeatureEvent.Paywall(event))
        }
        checkFileNumberOfEvents(100)
        eventsManager.flushEvents()
        checkFileNumberOfEvents(100)
    }

    @Test
    fun `if backend errors but marking events as synced, events are deleted`() {
        mockBackendResponse(success = false, shouldMarkAsSyncedOnError = true)
        for (i in 0..99) {
            eventsManager.track(FeatureEvent.Paywall(event))
        }
        checkFileNumberOfEvents(100)
        eventsManager.flushEvents()
        checkFileNumberOfEvents(50)
    }

    @Test
    fun `flushEvents multiple times only executes once`() {
        every {
            backend.postPaywallEvents(any(), any(), any())
        } just Runs
        eventsManager.track(FeatureEvent.Paywall(event))
        eventsManager.track(FeatureEvent.Paywall(event))
        eventsManager.flushEvents()
        eventsManager.flushEvents()
        eventsManager.flushEvents()
        verify(exactly = 1) {
            backend.postPaywallEvents(any(), any(), any())
        }
    }

    @Test
    fun `flushEvents multiple times, then finishing, adding events and flushing again works`() {
        val successSlot = slot<() -> Unit>()
        every {
            backend.postPaywallEvents(any(), capture(successSlot), any())
        } just Runs
        eventsManager.track(FeatureEvent.Paywall(event))
        eventsManager.track(FeatureEvent.Paywall(event))
        eventsManager.flushEvents()
        eventsManager.flushEvents()
        eventsManager.flushEvents()
        verify(exactly = 1) {
            backend.postPaywallEvents(any(), any(), any())
        }
        successSlot.captured()
        checkFileContents("")
        eventsManager.track(FeatureEvent.Paywall(event))
        eventsManager.track(FeatureEvent.Paywall(event))
        eventsManager.track(FeatureEvent.Paywall(event))
        eventsManager.flushEvents()
        eventsManager.flushEvents()
        verify(exactly = 2) {
            backend.postPaywallEvents(any(), any(), any())
        }
        successSlot.captured()
        checkFileContents("")
    }

    @Test
    fun `flushEvents with invalid events, flushes valid events`() {
        mockBackendResponse(success = true)
        eventsManager.track(FeatureEvent.Paywall(event))
        appendToFile("invalid event\n")
        eventsManager.track(FeatureEvent.Paywall(event))
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
            eventsManager.track(FeatureEvent.Paywall(event))
        }
        appendToFile("invalid event\n")
        appendToFile("invalid event 2\n")
        for (i in 0..49) {
            eventsManager.track(FeatureEvent.Paywall(event))
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
            backend.postPaywallEvents(any(), capture(successSlot), capture(errorSlot))
        } answers {
            if (success) {
                successSlot.captured.invoke()
            } else {
                errorSlot.captured.invoke(PurchasesError(PurchasesErrorCode.UnknownError), shouldMarkAsSyncedOnError)
            }
        }
    }

    private fun expectNumberOfEventsSynced(eventsSynced: Int) {
        val expectedRequest = EventRequest(
            List(eventsSynced) { BackendEvent.Paywalls(storedEvent.toPaywallBackendEvent()) }
        )
        verify(exactly = 1) {
            backend.postPaywallEvents(
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
