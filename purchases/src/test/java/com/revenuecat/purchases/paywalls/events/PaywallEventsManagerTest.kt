package com.revenuecat.purchases.paywalls.events

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.common.Dispatcher
import com.revenuecat.purchases.common.FileHelper
import com.revenuecat.purchases.common.SyncDispatcher
import com.revenuecat.purchases.identity.IdentityManager
import io.mockk.every
import io.mockk.mockk
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

    private val testFolder = "temp_test_folder"

    private lateinit var fileHelper: PaywallEventsFileHelper
    private lateinit var identityManager: IdentityManager
    private lateinit var paywallEventsDispatcher: Dispatcher

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
        fileHelper = PaywallEventsFileHelper(FileHelper(context))
        identityManager = mockk<IdentityManager>().apply {
            every { currentAppUserID } returns "testAppUserId"
        }
        paywallEventsDispatcher = SyncDispatcher()

        eventsManager = PaywallEventsManager(
            fileHelper,
            identityManager,
            paywallEventsDispatcher
        )
    }

    @After
    fun tearDown() {
        val tempTestFolder = File(testFolder)
        tempTestFolder.deleteRecursively()
    }

    @Test
    fun `tracking events adds them to file`() {
        val event = PaywallEvent(
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

    private fun checkFileContents(expectedContents: String) {
        val file = File(testFolder, PaywallEventsFileHelper.PAYWALL_EVENTS_FILE_PATH)
        assertThat(file.readText()).isEqualTo(expectedContents)
    }
}
