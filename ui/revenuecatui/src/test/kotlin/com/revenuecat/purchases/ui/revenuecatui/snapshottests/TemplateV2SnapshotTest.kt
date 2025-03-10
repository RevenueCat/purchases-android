package com.revenuecat.purchases.ui.revenuecatui.snapshottests

import app.cash.paparazzi.DeviceConfig
import com.revenuecat.purchases.LogHandler
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.ui.revenuecatui.components.OfferingProvider
import com.revenuecat.purchases.ui.revenuecatui.components.PaywallComponentsTemplate_Preview
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized

/**
 * This is used to record screenshots of our [PaywallComponentsTemplate_Preview]. They're then uploaded to Emerge and
 * verified there. The reason for this is that this specific preview often fails to render on Emerge, and crashes the
 * entire snapshots rendering run. Emerge and Google are working on a fix, but until that time we can use this
 * as a workaround.
 *
 * [issue tracker](https://issuetracker.google.com/issues/398784711)
 */
@RunWith(Parameterized::class)
class TemplateV2SnapshotTest(private val offering: Offering): BasePaparazziTest(
    testConfig = TestConfig(
        name = "pixel6",
        deviceConfig = DeviceConfig.PIXEL_6
    )
) {

    companion object {
        @JvmStatic
        @Parameterized.Parameters
        fun data(): List<Array<Offering>> {
            // The OfferingProvider uses an OfferingParser under the hood, which logs.
            // We have to replace the log handler, as the default one uses android.util.Log, which gives an
            // UnsatisfiedLinkError on Paparazzi.
            Purchases.logHandler = PrintLnLogHandler
            return OfferingProvider()
                .values
                .map { offering -> arrayOf(offering) }
                .toList()
        }
    }

    @Suppress("TestFunctionName")
    @Test
    fun PaywallComponentsTemplate_Test() {
        screenshotTest {
            PaywallComponentsTemplate_Preview(offering = offering)
        }
    }

    private object PrintLnLogHandler: LogHandler {
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
