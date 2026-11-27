package com.revenuecat.purchases.common.checkpoints

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.assertj.core.api.Assertions.assertThat
import org.json.JSONObject
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.SQLiteMode
import java.io.File

@RunWith(RobolectricTestRunner::class)
@SQLiteMode(SQLiteMode.Mode.NATIVE)
class CheckpointRuleDeliveryStoreTest {

    @Test
    fun `delivery counts match the portable fixture`() {
        // A follow-up PR will export this fixture so iOS can run the same cases against its store.
        val resource = requireNotNull(javaClass.getResourceAsStream("/checkpoints/delivery_count_cases.json"))
        val fixture = resource.bufferedReader().use { JSONObject(it.readText()) }
        val query = fixture.getJSONObject("query")
        val cases = fixture.getJSONArray("cases")
        val context = ApplicationProvider.getApplicationContext<Context>()
        for (index in 0 until cases.length()) {
            val case = cases.getJSONObject(index)
            val databaseName = CheckpointRuleDeliveryStore(context).use { store ->
                val deliveries = case.getJSONArray("deliveries")
                for (row in 0 until deliveries.length()) {
                    val delivery = deliveries.getJSONObject(row)
                    store.recordDelivery(delivery.getString("rule_id"), delivery.getLong("timestamp_ms"))
                }
                assertThat(store.countDeliveriesInWindow(
                    query.getString("rule_id"),
                    query.getLong("from_ms"),
                    query.getLong("through_ms"),
                )).describedAs(case.getString("name")).isEqualTo(case.getLong("expected"))
                store.databaseName
            }
            context.deleteDatabase(databaseName)
        }
    }

    @Test
    fun `reopened store counts only deliveries for the requested rule within the inclusive snapshot window`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val expectedFile = File(context.noBackupFilesDir, "RevenueCat/checkpoint_deliveries.db")
        assertThat(expectedFile.parentFile).doesNotExist()
        CheckpointRuleDeliveryStore(context).use { store ->
            assertThat(File(store.writableDatabase.path).canonicalFile).isEqualTo(expectedFile.canonicalFile)
            assertThat(expectedFile.isFile).isTrue()
            store.recordDelivery("A", 699_999L)
            store.recordDelivery("A", 700_000L)
            store.recordDelivery("A", 850_000L)
            store.recordDelivery("A", 1_000_000L)
            store.recordDelivery("A", 1_001_000L)
            store.recordDelivery("B", 850_000L)
        }

        CheckpointRuleDeliveryStore(context).use { store ->
            assertThat(store.countDeliveriesInWindow("A", cutoffMillis = 700_000L, evaluationMillis = 1_000_000L))
                .isEqualTo(3L)
        }
    }

    @Test
    fun `two deliveries for the same rule at the same millisecond both count`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        CheckpointRuleDeliveryStore(context).use { store ->
            repeat(2) { store.recordDelivery("A", 101L) }

            assertThat(store.countDeliveriesInWindow("A", cutoffMillis = 101L, evaluationMillis = 101L)).isEqualTo(2L)
        }
    }

    @Test
    fun `deliveries have null properties and counts exclude other event types with the same name`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        CheckpointRuleDeliveryStore(context).use { store ->
            store.recordDelivery("A", 101L)
            store.readableDatabase.rawQuery(
                "SELECT properties FROM events WHERE event_type = 'rule_delivered' AND name = 'A'",
                null,
            ).use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.isNull(0)).isTrue()
            }

            val properties = buildJsonObject {
                put("placement", "user's offer")
                put("page", 2)
            }
            store.recordEvent("paywall_viewed", "A", 101L, properties)
            store.readableDatabase.rawQuery(
                "SELECT timestamp_ms, properties FROM events WHERE event_type = ? AND name = ?",
                arrayOf("paywall_viewed", "A"),
            ).use { cursor ->
                assertThat(cursor.moveToFirst()).isTrue()
                assertThat(cursor.getLong(0)).isEqualTo(101L)
                assertThat(Json.parseToJsonElement(cursor.getString(1))).isEqualTo(properties)
            }
            assertThat(store.countDeliveriesInWindow("A", cutoffMillis = 100L, evaluationMillis = 101L)).isEqualTo(1L)
        }
    }
}
