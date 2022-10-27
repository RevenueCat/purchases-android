package com.revenuecat.purchases.integrationtests

import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.getCustomerInfoWith
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class IntegrationTest {

    @get:Rule
    var activityScenarioRule = activityScenarioRule<MainActivity>()

    @Before
    fun setup() {
        val scenario = activityScenarioRule.scenario
        scenario.onActivity {
            val builder = PurchasesConfiguration.Builder(it, "REVENUECAT_API_KEY").appUserID("integrationTest")
            Purchases.configure(builder.build())
        }
    }

    @After
    fun tearDown() {
        Purchases.sharedInstance.close()
    }

    @Test
    fun SDKCanBeConfigured() {
        val scenario = activityScenarioRule.scenario
        scenario.onActivity {
            assertThat(Purchases.sharedInstance.appUserID).isNotNull()
        }
    }

    @Test
    fun customerInfoCanBeFetched() {
        val lock = CountDownLatch(1)

        val scenario = activityScenarioRule.scenario
        scenario.onActivity {
            Purchases.sharedInstance.getCustomerInfoWith({
                fail("should be success. Error: ${it.message}")
            }) {
                lock.countDown()
            }
        }
        lock.await(5, TimeUnit.SECONDS)
        assertThat(lock.count).isZero()
    }
}
