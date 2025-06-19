package com.revenuecat.baselineprofile

import androidx.benchmark.macro.MacrobenchmarkScope
import androidx.test.uiautomator.By
import androidx.test.uiautomator.BySelector
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import androidx.test.uiautomator.Until

fun MacrobenchmarkScope.explorePaywallScreen() = device.apply {
    wait(Until.hasObject(By.res("paywall_screen")), TIMEOUT)
    waitForIdle()

    waitAndFindObject(By.res("full_screen_button"), TIMEOUT).click()
    waitForIdle()

    pressBack()
    waitForIdle()

    waitAndFindObject(By.res("footer_button"), TIMEOUT).click()
    waitForIdle()
}

/**
 * Waits until an object with [selector] if visible on screen and returns the object.
 * If the element is not available in [timeout], throws [AssertionError]
 */
internal fun UiDevice.waitAndFindObject(selector: BySelector, timeout: Long = TIMEOUT): UiObject2 {
    if (!wait(Until.hasObject(selector), timeout)) {
        throw AssertionError("Element not found on screen in ${timeout}ms (selector=$selector)")
    }

    return findObject(selector)
}
