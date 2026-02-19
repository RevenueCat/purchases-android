package com.revenuecat.purchases.utils

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.DebugEvent
import com.revenuecat.purchases.DebugEventName
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.common.FileHelper
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
class EventsFileHelperTest {

    private val testFolder = "temp_events_file_helper_test_folder"
    private val testFilePath = "test_events.jsonl"

    private lateinit var fileHelper: FileHelper
    private lateinit var eventsFileHelper: EventsFileHelper<TestEvent>

    data class TestEvent(val value: String) : Event {
        override fun toString(): String = value
    }

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
        fileHelper = FileHelper(context)
    }

    @After
    fun tearDown() {
        val tempTestFolder = File(testFolder)
        tempTestFolder.deleteRecursively()
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `debugEventCallback is called when appendEvent throws`() {
        val receivedEvents = mutableListOf<DebugEvent>()
        val badFileHelper = mockk<FileHelper>().apply {
            every { appendToFile(any(), any()) } throws RuntimeException("Write failed")
        }
        eventsFileHelper = EventsFileHelper(
            badFileHelper,
            testFilePath,
            { it.toString() },
            { TestEvent(it) },
        )
        eventsFileHelper.debugEventCallback = { receivedEvents.add(it) }

        try {
            eventsFileHelper.appendEvent(TestEvent("test"))
        } catch (_: RuntimeException) {
            // expected
        }

        assertThat(receivedEvents).hasSize(1)
        assertThat(receivedEvents.first().name).isEqualTo(DebugEventName.APPEND_EVENT_EXCEPTION)
        assertThat(receivedEvents.first().properties["exceptionType"]).isEqualTo("RuntimeException")
        assertThat(receivedEvents.first().properties["message"]).isEqualTo("Write failed")
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `readFile returns empty sequence when eventDeserializer is null`() {
        val receivedEvents = mutableListOf<DebugEvent>()
        eventsFileHelper = EventsFileHelper(
            fileHelper,
            testFilePath,
            { it.toString() },
            null,
        )
        eventsFileHelper.debugEventCallback = { receivedEvents.add(it) }

        // Write a line manually so there's something to read
        fileHelper.appendToFile(testFilePath, "test_line\n")

        var result: List<TestEvent?> = emptyList()
        eventsFileHelper.readFile { sequence ->
            result = sequence.toList()
        }

        // readFile short-circuits with emptySequence when deserializer is null
        assertThat(result).isEmpty()
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `debugEventCallback is called when deserialization throws SerializationException`() {
        val receivedEvents = mutableListOf<DebugEvent>()
        eventsFileHelper = EventsFileHelper(
            fileHelper,
            testFilePath,
            { it.toString() },
            { throw SerializationException("bad data") },
        )
        eventsFileHelper.debugEventCallback = { receivedEvents.add(it) }

        fileHelper.appendToFile(testFilePath, "bad_event\n")

        eventsFileHelper.readFile { sequence ->
            sequence.toList() // force evaluation
        }

        assertThat(receivedEvents).hasSize(1)
        assertThat(receivedEvents.first().name).isEqualTo(DebugEventName.DESERIALIZATION_ERROR)
        assertThat(receivedEvents.first().properties["exceptionType"]).isEqualTo("SerializationException")
        assertThat(receivedEvents.first().properties["message"]).isEqualTo("bad data")
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `debugEventCallback is called when deserialization throws IllegalArgumentException`() {
        val receivedEvents = mutableListOf<DebugEvent>()
        eventsFileHelper = EventsFileHelper(
            fileHelper,
            testFilePath,
            { it.toString() },
            { throw IllegalArgumentException("invalid arg") },
        )
        eventsFileHelper.debugEventCallback = { receivedEvents.add(it) }

        fileHelper.appendToFile(testFilePath, "bad_event\n")

        eventsFileHelper.readFile { sequence ->
            sequence.toList() // force evaluation
        }

        assertThat(receivedEvents).hasSize(1)
        assertThat(receivedEvents.first().name).isEqualTo(DebugEventName.DESERIALIZATION_ERROR)
        assertThat(receivedEvents.first().properties["exceptionType"]).isEqualTo("IllegalArgumentException")
        assertThat(receivedEvents.first().properties["message"]).isEqualTo("invalid arg")
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `debugEventCallback is called when removeFirstLinesFromFile encounters FileNotFoundException`() {
        val receivedEvents = mutableListOf<DebugEvent>()
        eventsFileHelper = EventsFileHelper(
            fileHelper,
            "nonexistent_path/nonexistent_file.jsonl",
            { it.toString() },
            { TestEvent(it) },
        )
        eventsFileHelper.debugEventCallback = { receivedEvents.add(it) }

        // Calling clear on a nonexistent file should trigger the FileNotFoundException path
        eventsFileHelper.clear(1)

        assertThat(receivedEvents).hasSize(1)
        assertThat(receivedEvents.first().name).isEqualTo(DebugEventName.REMOVE_LINES_EXCEPTION)
        assertThat(receivedEvents.first().properties["exceptionType"]).isEqualTo("FileNotFoundException")
        assertThat(receivedEvents.first().properties["message"]).isNotNull
    }

    @OptIn(InternalRevenueCatAPI::class)
    @Test
    fun `debugEventCallback message is truncated to 80 characters`() {
        val receivedEvents = mutableListOf<DebugEvent>()
        val longMessage = "A".repeat(120)
        val badFileHelper = mockk<FileHelper>().apply {
            every { appendToFile(any(), any()) } throws RuntimeException(longMessage)
        }
        eventsFileHelper = EventsFileHelper(
            badFileHelper,
            testFilePath,
            { it.toString() },
            { TestEvent(it) },
        )
        eventsFileHelper.debugEventCallback = { receivedEvents.add(it) }

        try {
            eventsFileHelper.appendEvent(TestEvent("test"))
        } catch (_: RuntimeException) {
            // expected
        }

        assertThat(receivedEvents).hasSize(1)
        assertThat(receivedEvents.first().properties["message"]).hasSize(80)
    }
}
