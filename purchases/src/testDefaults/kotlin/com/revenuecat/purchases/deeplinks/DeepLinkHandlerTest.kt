package com.revenuecat.purchases.deeplinks

import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.BasePurchasesTest
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.interfaces.RedeemWebPurchaseListener
import io.mockk.every
import io.mockk.mockk
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
@RunWith(AndroidJUnit4::class)
internal class DeepLinkHandlerTest: BasePurchasesTest() {

    private lateinit var intent: Intent

    override val shouldConfigureOnSetUp = false

    @Before
    fun setUpDeepLinkHandlerTests() {
        intent = mockk()

        every { intent.data } returns Uri.parse("revenuecatbilling://redeem_web_purchase?redemption_token=token")
    }

    @Test
    fun `if deep link not recognized, its ignored`() {
        every { intent.data } returns Uri.parse("revenuecatbilling://unknown_deep_link?redemption_token=token")
        val result = DeepLinkHandler.handleDeepLink(intent)
        DeepLinkHandler.forEachCachedLink {
            fail("Should not be called")
        }
        assertThat(result).isEqualTo(DeepLinkHandler.Result.IGNORED)
    }

    @Test
    fun `if SDK is not configured, deep link is cached and deferred to SDK configuration`() {
        val result = DeepLinkHandler.handleDeepLink(intent)
        val deepLinksCached = mutableListOf<DeepLinkParser.DeepLink>()
        DeepLinkHandler.forEachCachedLink {
            deepLinksCached.add(it)
        }
        assertThat(result).isEqualTo(DeepLinkHandler.Result.DEFERRED)
        assertThat(deepLinksCached.size).isEqualTo(1)
        assertThat(deepLinksCached[0]).isEqualTo(DeepLinkParser.DeepLink.RedeemWebPurchase("token"))
    }

    @Test
    fun `if SDK is not configured, multiple deep link are cached`() {
        val intent2 = mockk<Intent>()
        every { intent2.data } returns Uri.parse("revenuecatbilling://redeem_web_purchase?redemption_token=token2")
        DeepLinkHandler.handleDeepLink(intent)
        DeepLinkHandler.handleDeepLink(intent2)
        val deepLinksCached = mutableListOf<DeepLinkParser.DeepLink>()
        DeepLinkHandler.forEachCachedLink {
            deepLinksCached.add(it)
        }
        assertThat(deepLinksCached.size).isEqualTo(2)
        assertThat(deepLinksCached[0]).isEqualTo(DeepLinkParser.DeepLink.RedeemWebPurchase("token2"))
        assertThat(deepLinksCached[1]).isEqualTo(DeepLinkParser.DeepLink.RedeemWebPurchase("token"))
    }

    @Test
    fun `when processing deep links, if returning not handled they remain cached`() {
        val intent2 = mockk<Intent>()
        every { intent2.data } returns Uri.parse("revenuecatbilling://redeem_web_purchase?redemption_token=token2")
        DeepLinkHandler.handleDeepLink(intent)
        DeepLinkHandler.handleDeepLink(intent2)
        val deepLinksCached = mutableListOf<DeepLinkParser.DeepLink>()
        DeepLinkHandler.forEachCachedLink {
            deepLinksCached.add(it)
            false
        }
        assertThat(deepLinksCached.size).isEqualTo(2)
        val deepLinksCachedAfterIteratingOnceWithoutProcessing = mutableListOf<DeepLinkParser.DeepLink>()
        DeepLinkHandler.forEachCachedLink {
            deepLinksCachedAfterIteratingOnceWithoutProcessing.add(it)
            true
        }
        assertThat(deepLinksCachedAfterIteratingOnceWithoutProcessing.size).isEqualTo(2)
        DeepLinkHandler.forEachCachedLink {
            fail("Should not be called")
        }
    }

    @Test
    fun `if not configured, if disabling caching`() {
        val intent2 = mockk<Intent>()
        every { intent2.data } returns Uri.parse("revenuecatbilling://redeem_web_purchase?redemption_token=token2")
        DeepLinkHandler.handleDeepLink(intent, shouldCache = false)
        DeepLinkHandler.handleDeepLink(intent2, shouldCache = true)
        val deepLinksCached = mutableListOf<DeepLinkParser.DeepLink>()
        DeepLinkHandler.forEachCachedLink {
            deepLinksCached.add(it)
        }
        assertThat(deepLinksCached.size).isEqualTo(1)
        assertThat(deepLinksCached[0]).isEqualTo(DeepLinkParser.DeepLink.RedeemWebPurchase("token2"))
    }

    @Test
    fun `if not configured, and cached deep link accessed in different threads only processed once`() {
        val intent2 = mockk<Intent>()
        every { intent2.data } returns Uri.parse("revenuecatbilling://redeem_web_purchase?redemption_token=token2")
        DeepLinkHandler.handleDeepLink(intent)
        DeepLinkHandler.handleDeepLink(intent2)
        val deepLinksCachedThread1 = mutableListOf<DeepLinkParser.DeepLink>()
        val deepLinksCachedThread2 = mutableListOf<DeepLinkParser.DeepLink>()
        val countDownLatch = CountDownLatch(2)
        Thread {
            DeepLinkHandler.forEachCachedLink {
                deepLinksCachedThread1.add(it)
                true
            }
            countDownLatch.countDown()
        }.start()
        Thread {
            Thread.sleep(50)
            DeepLinkHandler.forEachCachedLink {
                deepLinksCachedThread2.add(it)
                true
            }
            countDownLatch.countDown()
        }.start()
        countDownLatch.await(3, TimeUnit.SECONDS)
        assertThat(countDownLatch.count).isEqualTo(0)
        assertThat(deepLinksCachedThread1.size).isEqualTo(2)
        assertThat(deepLinksCachedThread2.size).isEqualTo(0)
    }

    @Test
    fun `if not configured, and cached deep link accessed in different threads but not processed are processed again`() {
        val intent2 = mockk<Intent>()
        every { intent2.data } returns Uri.parse("revenuecatbilling://redeem_web_purchase?redemption_token=token2")
        DeepLinkHandler.handleDeepLink(intent)
        DeepLinkHandler.handleDeepLink(intent2)
        val deepLinksCachedThread1 = mutableListOf<DeepLinkParser.DeepLink>()
        val deepLinksCachedThread2 = mutableListOf<DeepLinkParser.DeepLink>()
        val countDownLatch = CountDownLatch(2)
        Thread {
            DeepLinkHandler.forEachCachedLink {
                deepLinksCachedThread1.add(it)
                false
            }
            countDownLatch.countDown()
        }.start()
        Thread {
            Thread.sleep(50)
            DeepLinkHandler.forEachCachedLink {
                deepLinksCachedThread2.add(it)
                true
            }
            countDownLatch.countDown()
        }.start()
        countDownLatch.await(3, TimeUnit.SECONDS)
        assertThat(countDownLatch.count).isEqualTo(0)
        assertThat(deepLinksCachedThread1.size).isEqualTo(2)
        assertThat(deepLinksCachedThread2.size).isEqualTo(2)
    }

    @Test
    fun `if SDK configured, but redeem listener not set, deep link deferred`() {
        anonymousSetup(anonymous = false)
        val result = DeepLinkHandler.handleDeepLink(intent)
        DeepLinkHandler.forEachCachedLink {
            fail("Should not be called")
        }
        assertThat(result).isEqualTo(DeepLinkHandler.Result.DEFERRED)
    }

    @Test
    fun `if SDK configured, and web purchase redeem listener set, deep link handled`() {
        anonymousSetup(anonymous = false)
        Purchases.sharedInstance.redeemWebPurchaseListener = object : RedeemWebPurchaseListener {
            override fun handleWebPurchaseRedemption(startRedemption: RedeemWebPurchaseListener.WebPurchaseRedeemer) {
                // Do nothing
            }
        }
        val result = DeepLinkHandler.handleDeepLink(intent)
        DeepLinkHandler.forEachCachedLink {
            fail("Should not be called")
        }
        assertThat(result).isEqualTo(DeepLinkHandler.Result.HANDLED)
    }
}
