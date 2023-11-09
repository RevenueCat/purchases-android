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

    private lateinit var fileHelper: EventsFileHelper<PaywallStoredEvent>
    private lateinit var identityManager: IdentityManager
    private lateinit var paywallEventsDispatcher: Dispatcher
    private lateinit var backend: Backend

    private lateinit var eventsManager: PaywallEventsManager

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
        fileHelper = EventsFileHelper(
            FileHelper(context),
            PaywallEventsManager.PAYWALL_EVENTS_FILE_PATH,
            PaywallStoredEvent::fromString,
        )
        identityManager = mockk<IdentityManager>().apply {
            every { currentAppUserID } returns userID
        }
        paywallEventsDispatcher = SyncDispatcher()
        backend = mockk()

        eventsManager = PaywallEventsManager(
            fileHelper,
            identityManager,
            paywallEventsDispatcher,
            backend,
        )
    }

    @After
    fun tearDown() {
        val tempTestFolder = File(testFolder)
        tempTestFolder.deleteRecursively()
    }

    @Test
    fun `tracking events adds them to file`() {
        eventsManager.track(event)
        checkFileContents("{" +
            "\"event\":{" +
                "\"creationData\":{" +
                    "\"id\":\"298207f4-87af-4b57-a581-eb27bcc6e009\"," +
                    "\"date\":1699270688884" +
                "}," +
                "\"data\":{" +
                    "\"offeringIdentifier\":\"offeringID\"," +
                    "\"paywallRevision\":5," +
                    "\"sessionIdentifier\":\"315107f4-98bf-4b68-a582-eb27bcb6e111\"," +
                    "\"displayMode\":\"footer\"," +
                    "\"localeIdentifier\":\"es_ES\"," +
                    "\"darkMode\":true" +
                "}," +
                "\"type\":\"IMPRESSION\"" +
            "}," +
            "\"userID\":\"testAppUserId\"" +
        "}\n")
        eventsManager.track(event.copy(type = PaywallEventType.CANCEL))
        checkFileContents("{" +
            "\"event\":{" +
                "\"creationData\":{" +
                    "\"id\":\"298207f4-87af-4b57-a581-eb27bcc6e009\"," +
                    "\"date\":1699270688884" +
                "}," +
                "\"data\":{" +
                    "\"offeringIdentifier\":\"offeringID\"," +
                    "\"paywallRevision\":5," +
                    "\"sessionIdentifier\":\"315107f4-98bf-4b68-a582-eb27bcb6e111\"," +
                    "\"displayMode\":\"footer\"," +
                    "\"localeIdentifier\":\"es_ES\"," +
                    "\"darkMode\":true" +
                "}," +
                "\"type\":\"IMPRESSION\"" +
            "}," +
            "\"userID\":\"testAppUserId\"" +
        "}\n" +
        "{" +
            "\"event\":{" +
                "\"creationData\":{" +
                    "\"id\":\"298207f4-87af-4b57-a581-eb27bcc6e009\"," +
                    "\"date\":1699270688884" +
                "}," +
                "\"data\":{" +
                    "\"offeringIdentifier\":\"offeringID\"," +
                    "\"paywallRevision\":5," +
                    "\"sessionIdentifier\":\"315107f4-98bf-4b68-a582-eb27bcb6e111\"," +
                    "\"displayMode\":\"footer\"," +
                    "\"localeIdentifier\":\"es_ES\"," +
                    "\"darkMode\":true" +
                "}," +
                "\"type\":\"CANCEL\"" +
            "}," +
            "\"userID\":\"testAppUserId\"" +
        "}\n")
    }

    @Test
    fun `flushEvents sends available events to backend`() {
        mockBackendResponse(success = true)
        eventsManager.track(event)
        eventsManager.track(event)
        eventsManager.flushEvents()
        checkFileContents("")
        val expectedRequest = PaywallEventRequest(
            listOf(storedEvent.toPaywallBackendEvent(), storedEvent.toPaywallBackendEvent())
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
            eventsManager.track(event)
        }
        checkFileNumberOfEvents(100)
        eventsManager.flushEvents()
        checkFileNumberOfEvents(50)
    }

    @Test
    fun `if backend errors without marking events as synced, events are not deleted`() {
        mockBackendResponse(success = false, shouldMarkAsSyncedOnError = false)
        for (i in 0..99) {
            eventsManager.track(event)
        }
        checkFileNumberOfEvents(100)
        eventsManager.flushEvents()
        checkFileNumberOfEvents(100)
    }

    @Test
    fun `if backend errors but marking events as synced, events are deleted`() {
        mockBackendResponse(success = false, shouldMarkAsSyncedOnError = true)
        for (i in 0..99) {
            eventsManager.track(event)
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
        eventsManager.track(event)
        eventsManager.track(event)
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
        eventsManager.track(event)
        eventsManager.track(event)
        eventsManager.flushEvents()
        eventsManager.flushEvents()
        eventsManager.flushEvents()
        verify(exactly = 1) {
            backend.postPaywallEvents(any(), any(), any())
        }
        successSlot.captured()
        checkFileContents("")
        eventsManager.track(event)
        eventsManager.track(event)
        eventsManager.track(event)
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
        eventsManager.track(event)
        appendToFile("invalid event\n")
        eventsManager.track(event)
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
            eventsManager.track(event)
        }
        appendToFile("invalid event\n")
        appendToFile("invalid event 2\n")
        for (i in 0..49) {
            eventsManager.track(event)
        }
        appendToFile("invalid event 3\n")
        checkFileNumberOfEvents(78)
        eventsManager.flushEvents()
        checkFileNumberOfEvents(28)
        expectNumberOfEventsSynced(48)
    }


    private fun checkFileNumberOfEvents(expectedNumberOfEvents: Int) {
        val file = File(testFolder, PaywallEventsManager.PAYWALL_EVENTS_FILE_PATH)
        assertThat(file.readLines().size).isEqualTo(expectedNumberOfEvents)
    }

    private fun checkFileContents(expectedContents: String) {
        val file = File(testFolder, PaywallEventsManager.PAYWALL_EVENTS_FILE_PATH)
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
        val expectedRequest = PaywallEventRequest(
            List(eventsSynced) { storedEvent.toPaywallBackendEvent() }
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
        val file = File(testFolder, PaywallEventsManager.PAYWALL_EVENTS_FILE_PATH)
        file.appendText(contents)
    }
}
