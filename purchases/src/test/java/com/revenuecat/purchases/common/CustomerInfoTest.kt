//  Purchases
//
//  Copyright © 2019 RevenueCat, Inc. All rights reserved.
//

package com.revenuecat.purchases.common

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.OwnershipType
import com.revenuecat.purchases.PeriodType
import com.revenuecat.purchases.Store
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.utils.Iso8601Utils
import com.revenuecat.purchases.utils.Responses
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONException
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.time.Duration.Companion.days

@RunWith(AndroidJUnit4::class)
@Config(manifest = Config.NONE)
class CustomerInfoTest {

    private val fullCustomerInfo: CustomerInfo by lazy {
        createCustomerInfo(Responses.validFullPurchaserResponse)
    }
    private val emptyCustomerInfo: CustomerInfo by lazy {
        createCustomerInfo(Responses.validEmptyPurchaserResponse)
    }

    @Test(expected = JSONException::class)
    fun failsToBeCreatedWithEmptyJSONObject() {
        createCustomerInfo("{}")
    }

    @Test
    fun `Given an empty response, empty object is created`() {
        assertThat(emptyCustomerInfo).isNotNull
        assertThat(emptyCustomerInfo.activeSubscriptions).isEmpty()
        assertThat(emptyCustomerInfo.allPurchasedProductIds).isEmpty()
        assertThat(emptyCustomerInfo.nonSubscriptionTransactions).isEmpty()
        assertThat(emptyCustomerInfo.latestExpirationDate).isNull()
    }

    @Test
    fun `Given a full response with non subscription SKUs, all SKUs are parsed properly`() {
        val info = fullCustomerInfo

        assertThat(info.nonSubscriptionTransactions.size).isEqualTo(5)
        assertThat(
            info.nonSubscriptionTransactions.filter { it.productIdentifier == "100_coins_pack" }.size
        ).isEqualTo(2)
        assertThat(
            info.nonSubscriptionTransactions.filter { it.productIdentifier == "7_extra_lives" }.size
        ).isEqualTo(2)
        assertThat(
            info.nonSubscriptionTransactions.filter { it.productIdentifier == "lifetime_access" }.size
        ).isEqualTo(1)
    }

    @Test
    @Throws(JSONException::class)
    fun `Given a full response, active subscription is calculated properly`() {
        val info = fullCustomerInfo
        val actives = info.activeSubscriptions

        assertThat(actives.size).isEqualTo(1)
        assertThat(actives).contains("pro:monthly")
    }

    @Test
    fun `active subscriptions returns expired subscriptions in grace period`() {
        val response = Responses.createFullCustomerResponse(
            oneMonthFreeTrialExpirationDate = 3.days.ago(),
            threeMonthFreeTrialExpirationDate = 1.days.ago()
        )
        val info = createCustomerInfo(response, 2.days.ago())
        val actives = info.activeSubscriptions

        assertThat(actives.size).isEqualTo(1)
        assertThat(actives.first()).isEqualTo("basic:monthly")
    }

    @Test
    fun `active subscriptions returns multiple non expired subscriptions in grace period`() {
        val response = Responses.createFullCustomerResponse(
            oneMonthFreeTrialExpirationDate = 1.days.ago(),
            threeMonthFreeTrialExpirationDate = 1.days.ago()
        )
        val info = createCustomerInfo(response, 2.days.ago())
        val actives = info.activeSubscriptions

        assertThat(actives.size).isEqualTo(2)
        assertThat(actives).containsAll(listOf("pro:monthly", "basic:monthly"))
    }

    @Test
    fun `active subscriptions returns nothing if no subscriptions in grace period`() {
        val response = Responses.createFullCustomerResponse(
            oneMonthFreeTrialExpirationDate = 1.days.ago(),
            threeMonthFreeTrialExpirationDate = 1.days.ago()
        )
        val info = createCustomerInfo(response, 5.days.ago())
        val actives = info.activeSubscriptions

        assertThat(actives.size).isEqualTo(0)
    }

    @Test
    fun `active subscriptions returns non-expired subscriptions`() {
        val response = Responses.createFullCustomerResponse(
            oneMonthFreeTrialExpirationDate = 1.days.fromNow(),
            threeMonthFreeTrialExpirationDate = 2.days.ago()
        )
        val info = createCustomerInfo(response, 1.days.ago())
        val actives = info.activeSubscriptions

        assertThat(actives.size).isEqualTo(1)
        assertThat(actives.first()).isEqualTo("pro:monthly")
    }

    @Test
    @Throws(JSONException::class)
    fun `Given a full response, all purchased SKUs are retrieved properly`() {
        val info = fullCustomerInfo
        val purchasedSkus = info.allPurchasedProductIds

        assertThat(purchasedSkus.size).isEqualTo(6)
        assertThat(purchasedSkus).contains("pro:monthly")
        assertThat(purchasedSkus).contains("100_coins_pack")
        assertThat(purchasedSkus).contains("basic:monthly")
        assertThat(purchasedSkus).contains("7_extra_lives")
        assertThat(purchasedSkus).contains("lifetime_access")
        assertThat(purchasedSkus).contains("paused:monthly")
    }

    @Test
    @Throws(JSONException::class)
    fun `Given a full purchase info, latest expiration date is calculated properly`() {
        val info = fullCustomerInfo

        val latest = info.latestExpirationDate

        assertThat(latest).isNotNull
        assertThat(latest!!.time).isEqualTo(4110728085975L)
    }

    @Test
    @Throws(JSONException::class)
    fun `Given a full purchase info, expiration date is de-serialized properly`() {
        val info = fullCustomerInfo

        val pro = info.getExpirationDateForProductId("pro:monthly")
        val basic = info.getExpirationDateForProductId("basic:monthly")

        assertThat(pro!!.after(basic)).`as`("$pro is after $basic")
            .isTrue
    }

    @Test
    @Throws(JSONException::class)
    fun `Given two valid products, json is deserialized properly`() {
        val info = fullCustomerInfo
        assertThat(info).isNotNull
    }

    @Test
    @Throws(JSONException::class)
    fun `Given a full purchase info, expiration dates are retrieved properly`() {
        val info = fullCustomerInfo

        val pro = info.getExpirationDateForEntitlement("pro")
        val basic = info.getExpirationDateForEntitlement("basic")

        assertThat(pro!!.after(basic)).`as`("$pro is after $basic").isTrue
    }

    @Test
    @Throws(JSONException::class)
    fun `Given a full purchase info, active entitlements are retrieved properly`() {
        val info = fullCustomerInfo
        val actives = info.entitlements.active.keys

        assertThat(actives.size).isEqualTo(2)

        assertThat(actives).contains("pro")
        assertThat(actives).doesNotContain("old_pro")
        assertThat(actives).contains("forever_pro")
        assertThat(actives).doesNotContain("random")
    }

    @Test
    @Throws(JSONException::class)
    fun `Given a null expiration date, expiration date is null`() {
        val info = fullCustomerInfo

        val foreverPro = info.getExpirationDateForEntitlement("forever_pro")

        assertThat(foreverPro).isNull()
    }

    @Test
    fun `Given a request date, it is de-serialized properly`() {
        val info = fullCustomerInfo
        assertThat(info.requestDate).isNotNull
    }

    @Test
    fun `Given a valid purchaser info, purchase date is parsed`() {
        val info = fullCustomerInfo
        assertThat(info.getPurchaseDateForEntitlement("pro")).isNotNull
    }

    @Test
    fun `Given two empty purchaser infos, both are equal`() {
        val info = createCustomerInfo(Responses.validEmptyPurchaserResponse)
        val info1 = createCustomerInfo(Responses.validEmptyPurchaserResponse)
        assertThat(info == info1).isTrue
    }

    @Test
    fun `Given two empty purchaser infos with different request dates, both are equal`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        jsonObject.put("request_date", "2018-06-20T06:24:50Z")
        val info = createCustomerInfo(jsonObject)
        jsonObject.put("request_date", "2018-05-20T06:24:50Z")
        val info1 = createCustomerInfo(jsonObject)
        assertThat(info).isEqualTo(info1)
    }

    @Test
    fun `Given two empty purchaser infos with different active entitlements, both are not equal`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        val info = createCustomerInfo(jsonObject)
        jsonObject.put("request_date", "2101-05-20T06:24:50Z")
        val info1 = createCustomerInfo(jsonObject)

        assertThat(info).isNotEqualTo(info1)
    }

    @Test
    fun `Given two same purchaser infos, their hashcodes are the same`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        val x = createCustomerInfo(jsonObject)
        val y = createCustomerInfo(jsonObject)
        assertThat(x.hashCode()).isEqualTo(y.hashCode())
    }

    @Test
    fun `Management url is properly retrieved`() {
        val x = createCustomerInfo(Responses.validFullPurchaserResponse)
        assertThat(x.managementURL).isEqualTo(Uri.parse("https://play.google.com/store/account/subscriptions"))
    }

    @Test
    fun `If management url is null in the JSON, the purchaser info is properly built`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        val subscriber = jsonObject.getJSONObject("subscriber")
        subscriber.put("management_url", JSONObject.NULL)
        jsonObject.put("subscriber", subscriber)
        val x = createCustomerInfo(jsonObject)
        assertThat(x.managementURL).isNull()
    }

    @Test
    fun `If management url is missing in the JSON, it is null in the customerInfo`() {
        assertThat(emptyCustomerInfo.managementURL).isNull()
    }

    @Test
    fun `Original purchase date is properly retrieved`() {
        val x = createCustomerInfo(Responses.validFullPurchaserResponse)
        assertThat(x.originalPurchaseDate!!.time).isEqualTo(1564183841000L)
    }

    @Test
    fun `Original purchase date is null if missing`() {
        assertThat(emptyCustomerInfo.originalPurchaseDate).isNull()
    }

    @Test
    fun `Original purchase date is null if it's present but it's a null string`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        val subscriber = jsonObject.getJSONObject("subscriber")
        subscriber.put("original_purchase_date", JSONObject.NULL)
        jsonObject.put("subscriber", subscriber)

        val x = createCustomerInfo(jsonObject)

        assertThat(x.originalPurchaseDate).isNull()
    }

    @Test
    fun `Non subscription transactions is empty if there are no non_subscriptions`() {
        assertThat(emptyCustomerInfo.nonSubscriptionTransactions).isEmpty()
    }

    @Test
    fun `Non subscription transactions is correctly created`() {
        assertThat(fullCustomerInfo.nonSubscriptionTransactions).isNotEmpty
        assertThat(fullCustomerInfo.nonSubscriptionTransactions.size).isEqualTo(5)

        val oneTimePurchaseTransactions = fullCustomerInfo.nonSubscriptionTransactions.filter {
            it.productIdentifier == "100_coins_pack"
        }
        assertThat(oneTimePurchaseTransactions.size).isEqualTo(2)

        val consumableTransactions = fullCustomerInfo.nonSubscriptionTransactions.filter {
            it.productIdentifier == "7_extra_lives"
        }
        assertThat(consumableTransactions.size).isEqualTo(2)

        assertThat((oneTimePurchaseTransactions + consumableTransactions)
            .distinctBy { it.transactionIdentifier }.size).isEqualTo(4)
    }

    @Test
    fun `Non subscription transactions list is correctly created`() {
        assertThat(fullCustomerInfo.nonSubscriptionTransactions).isNotEmpty
        assertThat(fullCustomerInfo.nonSubscriptionTransactions.size).isEqualTo(5)
        assertThat(
            (fullCustomerInfo.nonSubscriptionTransactions).distinctBy { it.transactionIdentifier }.size
        ).isEqualTo(5)
    }

    @Test
    fun `Test allExpirationDatesByProduct`() {
        assertThat(fullCustomerInfo.allExpirationDatesByProduct).isNotEmpty
        assertThat(fullCustomerInfo.allExpirationDatesByProduct.size).isEqualTo(3)

        assertThat(fullCustomerInfo.allExpirationDatesByProduct["pro:monthly"]).isNotNull
        assertThat(fullCustomerInfo.allExpirationDatesByProduct["basic:monthly"]).isNotNull
        assertThat(fullCustomerInfo.allExpirationDatesByProduct["paused:monthly"]).isNotNull
    }

    @Test
    fun `Test allPurchaseDatesByProduct`() {
        assertThat(fullCustomerInfo.allPurchaseDatesByProduct).isNotEmpty
        assertThat(fullCustomerInfo.allPurchaseDatesByProduct.size).isEqualTo(6)

        assertThat(fullCustomerInfo.allPurchaseDatesByProduct["pro:monthly"]).isNotNull
        assertThat(fullCustomerInfo.allPurchaseDatesByProduct["basic:monthly"]).isNotNull
        assertThat(fullCustomerInfo.allPurchaseDatesByProduct["100_coins_pack"]).isNotNull
        assertThat(fullCustomerInfo.allPurchaseDatesByProduct["7_extra_lives"]).isNotNull
        assertThat(fullCustomerInfo.allPurchaseDatesByProduct["lifetime_access"]).isNotNull
        assertThat(fullCustomerInfo.allPurchaseDatesByProduct["paused:monthly"]).isNotNull
    }

    @Test
    fun `Test subscriptionsByProductIdentifier`() {
        assertThat(fullCustomerInfo.subscriptionsByProductIdentifier).isNotEmpty
        assertThat(fullCustomerInfo.subscriptionsByProductIdentifier.size).isEqualTo(3)

        val proSubscription = fullCustomerInfo.subscriptionsByProductIdentifier["pro"]
        assertThat(proSubscription).isNotNull
        assertThat(proSubscription!!.productIdentifier).isEqualTo("pro")
        assertThat(proSubscription.purchaseDate).isEqualTo(Iso8601Utils.parse("2019-07-26T23:45:40Z"))
        assertThat(proSubscription.originalPurchaseDate).isEqualTo(Iso8601Utils.parse("2019-07-26T23:30:41Z"))
        assertThat(proSubscription.expiresDate).isEqualTo(Iso8601Utils.parse("2100-04-06T20:54:45.975000Z"))
        assertThat(proSubscription.store).isEqualTo(Store.APP_STORE)
        assertThat(proSubscription.unsubscribeDetectedAt).isNull()
        assertThat(proSubscription.isSandbox).isTrue()
        assertThat(proSubscription.billingIssuesDetectedAt).isNull()
        assertThat(proSubscription.gracePeriodExpiresDate).isNull()
        assertThat(proSubscription.ownershipType).isEqualTo(OwnershipType.PURCHASED)
        assertThat(proSubscription.periodType).isEqualTo(PeriodType.NORMAL)
        assertThat(proSubscription.refundedAt).isNull()
        assertThat(proSubscription.storeTransactionId).isEqualTo("GPA.3394-7009-4518-41945..6")
        assertThat(proSubscription.autoResumeDate).isNull()
        assertThat(proSubscription.displayName).isEqualTo("Pro Monthly")
        assertThat(proSubscription.price).isEqualTo(Price("$4.99", 4_990_000, "USD"))
        assertThat(proSubscription.productPlanIdentifier).isEqualTo("monthly")
        assertThat(proSubscription.managementURL).isEqualTo(
            Uri.parse("https://play.google.com/store/account/subscriptions")
        )
        assertThat(proSubscription.isActive).isTrue()
        assertThat(proSubscription.willRenew).isTrue()

        val basicSubscription = fullCustomerInfo.subscriptionsByProductIdentifier["basic"]
        assertThat(basicSubscription).isNotNull
        assertThat(basicSubscription!!.productIdentifier).isEqualTo("basic")
        assertThat(basicSubscription.purchaseDate).isEqualTo(Iso8601Utils.parse("2019-07-26T23:45:40Z"))
        assertThat(basicSubscription.originalPurchaseDate).isEqualTo(Iso8601Utils.parse("2019-07-26T23:30:41Z"))
        assertThat(basicSubscription.expiresDate).isEqualTo(Iso8601Utils.parse("1990-08-30T02:40:36Z"))
        assertThat(basicSubscription.store).isEqualTo(Store.APP_STORE)
        assertThat(basicSubscription.unsubscribeDetectedAt).isEqualTo(Iso8601Utils.parse("2023-07-26T23:45:40Z"))
        assertThat(basicSubscription.isSandbox).isTrue()
        assertThat(basicSubscription.billingIssuesDetectedAt).isNull()
        assertThat(basicSubscription.gracePeriodExpiresDate).isNull()
        assertThat(basicSubscription.ownershipType).isEqualTo(OwnershipType.PURCHASED)
        assertThat(basicSubscription.periodType).isEqualTo(PeriodType.NORMAL)
        assertThat(basicSubscription.refundedAt).isNull()
        assertThat(basicSubscription.storeTransactionId).isEqualTo("GPA.3394-7009-4518-41945..8")
        assertThat(basicSubscription.autoResumeDate).isNull()
        assertThat(basicSubscription.displayName).isEqualTo("Basic Monthly")
        assertThat(basicSubscription.price).isEqualTo(Price("$5.99", 5_990_000, "USD"))
        assertThat(basicSubscription.productPlanIdentifier).isEqualTo("monthly")
        assertThat(basicSubscription.managementURL).isEqualTo(
            Uri.parse("https://play.google.com/store/account/subscriptions")
        )
        assertThat(basicSubscription.isActive).isFalse()
        assertThat(basicSubscription.willRenew).isFalse()

        val pausedSubscription = fullCustomerInfo.subscriptionsByProductIdentifier["paused"]
        assertThat(pausedSubscription).isNotNull
        assertThat(pausedSubscription!!.productIdentifier).isEqualTo("paused")
        assertThat(pausedSubscription.purchaseDate).isEqualTo(Iso8601Utils.parse("2022-04-04T13:41:06Z"))
        assertThat(pausedSubscription.originalPurchaseDate).isEqualTo(Iso8601Utils.parse("2022-04-04T13:41:06Z"))
        assertThat(pausedSubscription.expiresDate).isEqualTo(Iso8601Utils.parse("2023-04-04T13:46:05Z"))
        assertThat(pausedSubscription.store).isEqualTo(Store.PLAY_STORE)
        assertThat(pausedSubscription.unsubscribeDetectedAt).isNull()
        assertThat(pausedSubscription.isSandbox).isTrue()
        assertThat(pausedSubscription.billingIssuesDetectedAt).isNull()
        assertThat(pausedSubscription.gracePeriodExpiresDate).isNull()
        assertThat(pausedSubscription.ownershipType).isEqualTo(OwnershipType.UNKNOWN)
        assertThat(pausedSubscription.periodType).isEqualTo(PeriodType.NORMAL)
        assertThat(pausedSubscription.refundedAt).isNull()
        assertThat(pausedSubscription.storeTransactionId).isEqualTo("GPA.3304-5067-2770-55157")
        assertThat(pausedSubscription.autoResumeDate).isEqualTo(Iso8601Utils.parse("2100-04-06T20:54:45.975000Z"))
        assertThat(pausedSubscription.displayName).isNull()
        assertThat(pausedSubscription.price).isEqualTo(Price("$9.99", 9_990_000, "USD"))
        assertThat(pausedSubscription.productPlanIdentifier).isEqualTo("monthly")
        assertThat(pausedSubscription.managementURL).isEqualTo(
            Uri.parse("https://play.google.com/store/account/subscriptions")
        )
        assertThat(pausedSubscription.isActive).isFalse()
        assertThat(pausedSubscription.willRenew).isTrue()
    }

    @Test
    fun `Test subscriptionsByProductIdentifier nullable dates`() {
        val jsonObject = JSONObject(Responses.validFullPurchaserResponse)
        val subscriber = jsonObject.getJSONObject("subscriber")
        val subscriptions = subscriber.getJSONObject("subscriptions")

        val proSubscriptionJSON = subscriptions.getJSONObject("pro")
        proSubscriptionJSON.put("unsubscribe_detected_at", "2023-07-26T23:45:40Z")
        proSubscriptionJSON.put("billing_issues_detected_at", "2019-07-26T23:45:40Z")
        proSubscriptionJSON.put("grace_period_expires_date", "2019-07-25T23:45:40Z")
        proSubscriptionJSON.put("refunded_at", "2019-07-24T23:45:40Z")
        proSubscriptionJSON.put("auto_resume_date", "2019-07-24T23:45:40Z")

        subscriptions.put("pro", proSubscriptionJSON)
        subscriber.put("subscriptions", subscriptions)
        jsonObject.put("subscriber", subscriber)

        val customerInfo = createCustomerInfo(jsonObject)

        assertThat(customerInfo.subscriptionsByProductIdentifier).isNotEmpty
        assertThat(customerInfo.subscriptionsByProductIdentifier.size).isEqualTo(3)

        val proSubscription = customerInfo.subscriptionsByProductIdentifier["pro"]
        assertThat(proSubscription).isNotNull
        assertThat(proSubscription!!.productIdentifier).isEqualTo("pro")
        assertThat(proSubscription.purchaseDate).isEqualTo(Iso8601Utils.parse("2019-07-26T23:45:40Z"))
        assertThat(proSubscription.originalPurchaseDate).isEqualTo(Iso8601Utils.parse("2019-07-26T23:30:41Z"))
        assertThat(proSubscription.expiresDate).isEqualTo(Iso8601Utils.parse("2100-04-06T20:54:45.975000Z"))
        assertThat(proSubscription.unsubscribeDetectedAt).isEqualTo(Iso8601Utils.parse("2023-07-26T23:45:40Z"))
        assertThat(proSubscription.billingIssuesDetectedAt).isEqualTo(Iso8601Utils.parse("2019-07-26T23:45:40Z"))
        assertThat(proSubscription.gracePeriodExpiresDate).isEqualTo(Iso8601Utils.parse("2019-07-25T23:45:40Z"))
        assertThat(proSubscription.refundedAt).isEqualTo(Iso8601Utils.parse("2019-07-24T23:45:40Z"))
        assertThat(proSubscription.autoResumeDate).isEqualTo(Iso8601Utils.parse("2019-07-24T23:45:40Z"))
    }
}
