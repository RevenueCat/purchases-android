// Created by Monika on 2026-10-02

@file:OptIn(InternalRevenueCatAPI::class)

package com.revenuecat.purchases.ui.revenuecatui.customercenter

import android.net.Uri
import androidx.lifecycle.ViewModelStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.EntitlementInfos
import com.revenuecat.purchases.InternalRevenueCatAPI
import com.revenuecat.purchases.VerificationResult
import com.revenuecat.purchases.customercenter.CustomerCenterConfigData
import com.revenuecat.purchases.models.Period
import com.revenuecat.purchases.models.Price
import com.revenuecat.purchases.models.TestStoreProduct
import com.revenuecat.purchases.ui.revenuecatui.customercenter.data.CustomerCenterState
import com.revenuecat.purchases.ui.revenuecatui.customercenter.navigation.CustomerCenterDestination
import com.revenuecat.purchases.ui.revenuecatui.customercenter.viewmodel.CustomerCenterViewModelImpl
import com.revenuecat.purchases.ui.revenuecatui.data.testdata.TestData
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import java.util.Date

/** Executes the same scenario contract consumed by both mobile platforms against SDK action filtering. */
@RunWith(AndroidJUnit4::class)
class CustomerCenterParityTest {
    @Test
    fun `shared scenarios drive the actual SDK action decisions`(): Unit = runBlocking {
        val fixture = JSONObject(javaClass.getResource("/customer_center_parity.json")!!.readText())
        val now = Date()
        val cases = fixture.getJSONArray("cases")
        val config = Json { ignoreUnknownKeys = true }.decodeFromString<CustomerCenterConfigData>(Configuration)
        for (index in 0 until cases.length()) {
            val row = cases.getJSONObject(index)
            val expected = row.getJSONObject("expected")
            val provider = mockk<CustomerCenterPreviewProvider>(relaxed = true)
            coEvery { provider.configuration() } returns config
            coEvery { provider.customerInfo() } coAnswers { customer(expected, now) }
            coEvery { provider.product(any(), any()) } returns TestStoreProduct(
                id = expected.optString("productId", "none"), name = "Preview", title = "Preview",
                description = "Preview", price = Price("$5.99", 5_990_000, "USD"),
                period = if (row.getString("product") == "Lifetime") null else Period(1, Period.Unit.MONTH, "P1M"),
            )
            val model = CustomerCenterViewModelImpl(
                purchases = CustomerCenterPreviewPurchases(provider),
                colorScheme = TestData.Constants.currentColorScheme, isDarkMode = false, previewProvider = provider,
            )
            val store = ViewModelStore()
            store.put("parity", model)
            try {
                val state = model.state.filterIsInstance<CustomerCenterState.Success>().first()
                val purchase = state.purchases.singleOrNull()
                if (purchase != null) model.selectPurchase(purchase)
                val paths = if (purchase != null) {
                    model.state.filterIsInstance<CustomerCenterState.Success>()
                        .first { it.currentDestination is CustomerCenterDestination.SelectedPurchaseDetail }.detailScreenPaths
                } else state.mainScreenPaths
                for (id in listOf("cancel", "refund", "changePlans")) {
                    assertThat(paths.any { it.id == id }).describedAs("${row.getString("id")}: $id")
                        .isEqualTo(expected.getBoolean(id))
                }
                if (expected.getBoolean("active") && row.getString("product") != "Lifetime") {
                    expected.put("active", false)
                    model.refreshCustomerCenter()
                    val refreshedState = model.state.filterIsInstance<CustomerCenterState.Success>().first {
                        (it.currentDestination as? CustomerCenterDestination.SelectedPurchaseDetail)
                            ?.purchaseInformation?.isExpired == true
                    }
                    assertThat(refreshedState.detailScreenPaths.any { it.id == "cancel" }).isFalse()
                }
            } finally {
                store.clear()
            }
        }
    }

    private fun customer(expected: JSONObject, now: Date): CustomerInfo {
        val none = expected.isNull("productId")
        val id = expected.optString("productId", "none")
        val lifetime = id == "lifetime"
        val expires = if (lifetime || none) null else Date(now.time + if (expected.getBoolean("active")) DayMillis else -DayMillis)
        val purchased = Date(now.time - DayMillis * 4)
        val transaction = JSONObject().apply {
            put("id", "preview-transaction")
            put("purchase_date", purchased.toInstant().toString())
            put("original_purchase_date", purchased.toInstant().toString())
            put("expires_date", expires?.toInstant()?.toString() ?: JSONObject.NULL)
            put("store", expected.getString("store").lowercase())
            put("ownership_type", "PURCHASED")
            put("period_type", if (expected.getBoolean("trial")) "trial" else "normal")
            put("is_sandbox", true)
            put("management_url", "https://example.com/manage")
            put("price", JSONObject().put("amount", 5.99).put("currency", "USD"))
        }
        val json = JSONObject().put("subscriber", JSONObject()
            .put("subscriptions", JSONObject().apply { if (!none && !lifetime) put(id, transaction) })
            .put("non_subscriptions", JSONObject().apply { if (lifetime) put(id, JSONArray().put(transaction)) }))
        return CustomerInfo(
            EntitlementInfos(emptyMap(), VerificationResult.NOT_REQUESTED),
            if (!none && !lifetime) mapOf(id to expires) else emptyMap(),
            if (!none) mapOf(id to purchased) else emptyMap(), now, 4, purchased, "preview",
            Uri.parse("https://example.com/manage"), purchased, json,
        )
    }
}

private const val DayMillis = 86_400_000L
private const val Configuration = """{
    "screens": {
        "MANAGEMENT": {"type":"MANAGEMENT","title":"Manage","paths":[
            {"id":"cancel","title":"Cancel","type":"CANCEL"},
            {"id":"refund","title":"Refund","type":"REFUND_REQUEST"},
            {"id":"changePlans","title":"Change plans","type":"CHANGE_PLANS"}]},
        "NO_ACTIVE": {"type":"NO_ACTIVE","title":"No purchases","paths":[]}
    }, "appearance":{}, "localization":{"locale":"en_US","localized_strings":{}}, "support":{}
}"""
