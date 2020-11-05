
import androidx.test.ext.junit.rules.activityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getPurchaserInfoWith
import com.revenuecat.sample.InitialActivity
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.fail
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

@RunWith(AndroidJUnit4::class)
class IntegrationTest {

    @get:Rule var activityScenarioRule = activityScenarioRule<InitialActivity>()

    @Test
    fun SDKCanBeConfigured() {
        val scenario = activityScenarioRule.scenario
        scenario.onActivity {
            assertThat(Purchases.sharedInstance.appUserID).isNotNull()
        }
    }

    @Test
    fun purchaserInfoCanBeFetched() {
        val lock = CountDownLatch(1)

        val scenario = activityScenarioRule.scenario
        scenario.onActivity {
            Purchases.sharedInstance.getPurchaserInfoWith({
                fail("should be success. Error: ${it.message}")
            }) {
                lock.countDown()
            }
        }
        lock.await(5, TimeUnit.SECONDS)
        assertThat(lock.count).isZero()
    }
}
