package com.revenuecat.purchases.utils

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.assertj.core.api.AssertionsForClassTypes
import org.junit.Rule
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.reflect.KClass

/**
 * If your test doesn't need to extend any other test, you can use this to include the rule automatically.
 * Alternatively, just include the contents of this class in your own test to get the same functionality.
 */
abstract class CoroutineTest {
    public suspend fun assertThrows(expectedType: KClass<out Throwable>, block: suspend () -> Unit): Unit = try {
        block()
        AssertionsForClassTypes.fail("Expected ${expectedType.simpleName} to be thrown")
    } catch (e: Throwable) {
        if (e::class != expectedType) {
            AssertionsForClassTypes
                .fail("Expected ${expectedType.simpleName} to be thrown, but ${e::class.simpleName} was thrown instead")
        } else {
            // Success
        }
    }

    @get:Rule
    val coroutineTestRule = CoroutineTestRule()
}

/**
 * Sets up a test so that it uses a [UnconfinedTestDispatcher]. This allows the tests to be run synchronously and
 * deterministically.
 */
public class CoroutineTestRule : TestWatcher() {

    @OptIn(ExperimentalCoroutinesApi::class)
    public val dispatcher = UnconfinedTestDispatcher()

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun starting(description: Description) {
        super.starting(description)
        Dispatchers.setMain(dispatcher)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    override fun finished(description: Description) {
        super.finished(description)
        Dispatchers.resetMain()
    }
}
