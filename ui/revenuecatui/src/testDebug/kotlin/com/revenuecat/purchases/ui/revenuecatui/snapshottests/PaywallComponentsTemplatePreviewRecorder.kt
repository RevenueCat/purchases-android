package com.revenuecat.purchases.ui.revenuecatui.snapshottests

import app.cash.paparazzi.DeviceConfig
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ui.revenuecatui.components.OfferingProvider
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallComponentsTemplate_Preview
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameters

/**
 * This is used to record screenshots of our [PaywallComponentsTemplate_Preview] with a fixed resolution and density.
 * They're then uploaded to our paywall-rendering-validation repository to be able to validate rendering of all
 * platforms.
 */
@OptIn(ExperimentalCoroutinesApi::class, DelicateCoroutinesApi::class)
@RunWith(Parameterized::class)
class PaywallComponentsTemplatePreviewRecorder(
    @Suppress("UNUSED_PARAMETER") name: String,
    private val offering: Offering,
) : BasePaparazziTest(
    testConfig = TestConfig(
        name = "pixel6",
        deviceConfig = DeviceConfig.PIXEL_6,
    ),
) {

    companion object {
        @JvmStatic
        // Placing the offering ID in double square brackets so we can easily parse it later.
        @Parameters(name = "[[{0}]]")
        fun data(): List<Array<Any>> {
            // The OfferingProvider uses an OfferingParser under the hood, which logs.
            // We have to replace the log handler, as the default one uses android.util.Log, which gives an
            // UnsatisfiedLinkError on Paparazzi.
            Purchases.logHandler = PrintLnLogHandler
            return OfferingProvider()
                .values
                .map { offering -> arrayOf(offering.identifier, offering) }
                .toList()
        }
    }

    @Before
    fun setup() {
        Dispatchers.setMain(newSingleThreadContext("PaywallComponentsTemplatePreviewRecorder-main-dispatcher"))
    }

    @After
    fun teardown() {
        Dispatchers.resetMain()
    }

    @Suppress("TestFunctionName", "FunctionNaming")
    @Test
    fun PaywallComponentsTemplate_Test() {
        screenshotTest {
            PaywallComponentsTemplate_Preview(offering = offering)
        }
    }

    private object PrintLnLogHandler : LogHandler {
        override fun v(tag: String, msg: String) {
            println("V [$tag]: $msg")
        }

        override fun d(tag: String, msg: String) {
            println("D [$tag]: $msg")
        }

        override fun i(tag: String, msg: String) {
            println("I [$tag]: $msg")
        }

        override fun w(tag: String, msg: String) {
            println("W [$tag]: $msg")
        }

        override fun e(tag: String, msg: String, throwable: Throwable?) {
            println("E [$tag]: $msg")
            throwable?.printStackTrace()
        }
    }
}
