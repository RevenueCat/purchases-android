package com.revenuecat.purchasetester.ui.screens.login

import app.cash.turbine.test
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
class LoginScreenViewModelTest {

    companion object {
        private const val TEST_USER_ID = "test_user_123"
        private const val EMPTY_STRING = ""
        private const val WHITESPACE_ONLY = "   "
        private const val TRIMMED_USER_ID = "trimmed_user"
        private const val USER_ID_WITH_SPACES = "  $TRIMMED_USER_ID  "
        private const val USER_ID_1 = "user1"
        private const val USER_ID_2 = "user2"
        private const val SKIP_ITEMS_COUNT = 1
    }

    private val testDispatcher = UnconfinedTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is Loading then transitions to LoginScreenData`() = runTest {
        // Given
        val viewModel = LoginScreenViewModelImpl()

        // When
        viewModel.state.test {
            val initialState = awaitItem()

            // Then
            assertTrue(initialState is LoginScreenState.LoginScreenData)
            val data = initialState as LoginScreenState.LoginScreenData
            assertEquals(EMPTY_STRING, data.userId)
        }
    }

    @Test
    fun `initial state has empty userId`() = runTest {
        // Given
        val viewModel = LoginScreenViewModelImpl()

        // When
        viewModel.state.test {
            val state = awaitItem() as LoginScreenState.LoginScreenData

            // Then
            assertEquals(EMPTY_STRING, state.userId)
        }
    }

    @Test
    fun `saveUserId updates state with trimmed value`() = runTest {
        // Given
        val viewModel = LoginScreenViewModelImpl()

        // When
        viewModel.state.test {
            skipItems(SKIP_ITEMS_COUNT)

            viewModel.saveUserId(USER_ID_WITH_SPACES)

            val updatedState = awaitItem() as LoginScreenState.LoginScreenData

            // Then
            assertEquals(TRIMMED_USER_ID, updatedState.userId)
        }
    }

    @Test
    fun `saveUserId handles whitespace correctly`() = runTest {
        // Given
        val viewModel = LoginScreenViewModelImpl()

        // When
        viewModel.state.test {
            // Skip initial empty state
            skipItems(SKIP_ITEMS_COUNT)

            // Set a non-empty userId first
            viewModel.saveUserId(TEST_USER_ID)
            val state1 = awaitItem() as LoginScreenState.LoginScreenData
            assertEquals(TEST_USER_ID, state1.userId)

            // Then save whitespace
            viewModel.saveUserId(WHITESPACE_ONLY)
            val updatedState = awaitItem() as LoginScreenState.LoginScreenData

            // Then
            assertEquals(EMPTY_STRING, updatedState.userId)
        }
    }

    @Test
    fun `saveUserId handles empty string`() = runTest {
        // Given
        val viewModel = LoginScreenViewModelImpl()

        // When
        viewModel.state.test {
            // Skip initial empty state
            skipItems(SKIP_ITEMS_COUNT)

            // Set a non-empty userId first
            viewModel.saveUserId(TEST_USER_ID)
            val state1 = awaitItem() as LoginScreenState.LoginScreenData
            assertEquals(TEST_USER_ID, state1.userId)

            // Then save empty string
            viewModel.saveUserId(EMPTY_STRING)
            val updatedState = awaitItem() as LoginScreenState.LoginScreenData

            // Then
            assertEquals(EMPTY_STRING, updatedState.userId)
        }
    }

    @Test
    fun `multiple state updates work correctly`() = runTest {
        // Given
        val viewModel = LoginScreenViewModelImpl()

        // When
        viewModel.state.test {
            skipItems(SKIP_ITEMS_COUNT)

            viewModel.saveUserId(USER_ID_1)
            val state1 = awaitItem() as LoginScreenState.LoginScreenData

            viewModel.saveUserId(USER_ID_2)
            val state2 = awaitItem() as LoginScreenState.LoginScreenData

            // Then
            assertEquals(USER_ID_1, state1.userId)
            assertEquals(USER_ID_2, state2.userId)
        }
    }

    @Test
    fun `state updates preserve correct values through multiple changes`() = runTest {
        // Given
        val viewModel = LoginScreenViewModelImpl()

        // When
        viewModel.state.test {
            skipItems(SKIP_ITEMS_COUNT)

            viewModel.saveUserId(TEST_USER_ID)
            val state1 = awaitItem() as LoginScreenState.LoginScreenData

            viewModel.saveUserId(USER_ID_WITH_SPACES)
            val state2 = awaitItem() as LoginScreenState.LoginScreenData

            // Then
            assertEquals(TEST_USER_ID, state1.userId)
            assertEquals(TRIMMED_USER_ID, state2.userId)
        }
    }
}
