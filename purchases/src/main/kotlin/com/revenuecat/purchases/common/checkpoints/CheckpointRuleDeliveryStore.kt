package com.revenuecat.purchases.common.checkpoints

import android.content.Context
import android.database.DatabaseUtils
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteException
import android.database.sqlite.SQLiteOpenHelper
import kotlinx.serialization.json.JsonObject
import java.io.File
import java.io.IOException

private object DeliverySql {
    val createTable by lazy { load("create_events") }
    val createIndex by lazy { load("create_events_index") }
    val record by lazy { load("record_event") }
    val count by lazy { load("count_rule_deliveries") }

    private fun load(name: String): String {
        val path = "/checkpoints/sql/$name.sql"
        val stream = javaClass.getResourceAsStream(path)
            ?: throw SQLiteException("Missing checkpoint SQL resource $path")
        return try {
            stream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (e: IOException) {
            throw SQLiteException("Could not read checkpoint SQL resource $path", e)
        }
    }
}

internal class CheckpointRuleDeliveryStore(context: Context) :
    SQLiteOpenHelper(
        context.applicationContext,
        File(context.applicationContext.noBackupFilesDir, "RevenueCat/checkpoint_deliveries.db").absolutePath,
        null,
        1,
    ) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(DeliverySql.createTable)
        db.execSQL(DeliverySql.createIndex)
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        error("Missing checkpoint delivery database migration from $oldVersion to $newVersion")
    }

    fun recordDelivery(ruleId: String, timestampMillis: Long) {
        recordEvent("rule_delivered", ruleId, timestampMillis, properties = null)
    }

    fun recordEvent(eventType: String, name: String, timestampMillis: Long, properties: JsonObject?) {
        writableDatabase.execSQL(
            DeliverySql.record,
            arrayOf<Any?>(eventType, name, timestampMillis, properties?.toString()),
        )
    }

    fun countDeliveriesInWindow(ruleId: String, cutoffMillis: Long, evaluationMillis: Long): Long =
        DatabaseUtils.longForQuery(
            readableDatabase,
            DeliverySql.count,
            arrayOf(ruleId, cutoffMillis.toString(), evaluationMillis.toString()),
        )
}
