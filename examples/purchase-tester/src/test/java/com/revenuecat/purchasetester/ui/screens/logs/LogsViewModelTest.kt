package com.revenuecat.purchasetester.ui.screens.logs

import app.cash.turbine.test
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchasetester.LogMessage
import com.revenuecat.purchasetester.TesterLogHandler
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LogsViewModelTest {

    companion object {
        private const val TEST_MESSAGE_1 = "Test log message 1"
        private const val TEST_MESSAGE_2 = "Test log message 2"
        private const val TEST_MESSAGE_3 = "Test log message 3"
    }

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var mockLogHandler: TesterLogHandler

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        mockLogHandler = mockk()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading`() = runTest {
        // Given
        every { mockLogHandler.storedLogs } returns emptyList()
        val viewModel = LogsViewModelImpl(mockLogHandler)

        // When
        viewModel.state.test {
            val initialState = awaitItem()

            // Then
            assertTrue(initialState is LogsScreenState.Loading)
        }
    }

    @Test
    fun `loadLogs transitions to LogsData with logs from handler`() = runTest {
        // Given
        val testLogs = listOf(
            LogMessage(LogLevel.INFO, TEST_MESSAGE_1),
            LogMessage(LogLevel.WARN, TEST_MESSAGE_2),
            LogMessage(LogLevel.ERROR, TEST_MESSAGE_3)
        )
        every { mockLogHandler.storedLogs } returns testLogs
        val viewModel = LogsViewModelImpl(mockLogHandler)

        // When
        viewModel.state.test {
            skipItems(1) // Skip Loading state

            viewModel.loadLogs()

            val logsState = awaitItem() as LogsScreenState.LogsData

            // Then
            assertEquals(3, logsState.logs.size)
            assertEquals(TEST_MESSAGE_1, logsState.logs[0].message)
            assertEquals(LogLevel.INFO, logsState.logs[0].logLevel)
            assertEquals(TEST_MESSAGE_2, logsState.logs[1].message)
            assertEquals(LogLevel.WARN, logsState.logs[1].logLevel)
            assertEquals(TEST_MESSAGE_3, logsState.logs[2].message)
            assertEquals(LogLevel.ERROR, logsState.logs[2].logLevel)
        }
    }

    @Test
    fun `loadLogs handles empty logs list correctly`() = runTest {
        // Given
        every { mockLogHandler.storedLogs } returns emptyList()
        val viewModel = LogsViewModelImpl(mockLogHandler)

        // When
        viewModel.state.test {
            skipItems(1) // Skip Loading state

            viewModel.loadLogs()

            val logsState = awaitItem() as LogsScreenState.LogsData

            // Then
            assertTrue(logsState.logs.isEmpty())
        }
    }

    @Test
    fun `loadLogs updates state with all log levels correctly`() = runTest {
        // Given
        val testLogs = listOf(
            LogMessage(LogLevel.VERBOSE, "Verbose message"),
            LogMessage(LogLevel.DEBUG, "Debug message"),
            LogMessage(LogLevel.INFO, "Info message"),
            LogMessage(LogLevel.WARN, "Warning message"),
            LogMessage(LogLevel.ERROR, "Error message")
        )
        every { mockLogHandler.storedLogs } returns testLogs
        val viewModel = LogsViewModelImpl(mockLogHandler)

        // When
        viewModel.state.test {
            skipItems(1) // Skip Loading state

            viewModel.loadLogs()

            val logsState = awaitItem() as LogsScreenState.LogsData

            // Then
            assertEquals(5, logsState.logs.size)
            assertEquals(LogLevel.VERBOSE, logsState.logs[0].logLevel)
            assertEquals(LogLevel.DEBUG, logsState.logs[1].logLevel)
            assertEquals(LogLevel.INFO, logsState.logs[2].logLevel)
            assertEquals(LogLevel.WARN, logsState.logs[3].logLevel)
            assertEquals(LogLevel.ERROR, logsState.logs[4].logLevel)
        }
    }

    @Test
    fun `loadLogs can be called multiple times`() = runTest {
        // Given
        val initialLogs = listOf(LogMessage(LogLevel.INFO, TEST_MESSAGE_1))
        val updatedLogs = listOf(
            LogMessage(LogLevel.INFO, TEST_MESSAGE_1),
            LogMessage(LogLevel.INFO, TEST_MESSAGE_2)
        )
        every { mockLogHandler.storedLogs } returnsMany listOf(initialLogs, updatedLogs)
        val viewModel = LogsViewModelImpl(mockLogHandler)

        // When
        viewModel.state.test {
            skipItems(1) // Skip Loading state

            viewModel.loadLogs()
            val firstState = awaitItem() as LogsScreenState.LogsData
            assertEquals(1, firstState.logs.size)

            viewModel.loadLogs()
            val secondState = awaitItem() as LogsScreenState.LogsData

            // Then
            assertEquals(2, secondState.logs.size)
        }
    }

    @Test
    fun `StateFlow collects properly`() = runTest {
        // Given
        val testLogs = listOf(LogMessage(LogLevel.INFO, TEST_MESSAGE_1))
        every { mockLogHandler.storedLogs } returns testLogs
        val viewModel = LogsViewModelImpl(mockLogHandler)

        // When
        viewModel.state.test {
            val loadingState = awaitItem()
            assertTrue(loadingState is LogsScreenState.Loading)

            viewModel.loadLogs()
            val dataState = awaitItem()

            // Then
            assertTrue(dataState is LogsScreenState.LogsData)
            val logsData = dataState as LogsScreenState.LogsData
            assertEquals(1, logsData.logs.size)
        }
    }
}
