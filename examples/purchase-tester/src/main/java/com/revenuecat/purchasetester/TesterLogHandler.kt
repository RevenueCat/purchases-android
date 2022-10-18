package com.revenuecat.purchasetester

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.revenuecat.purchases.LogHandler

class TesterLogHandler(
    private val applicationContext: Context,
    private val mainHandler: Handler = Handler(Looper.getMainLooper())
) : LogHandler {

    val storedLogs: List<LogMessage>
        get() = mutableStoredLogs

    private val mutableStoredLogs: MutableList<LogMessage> = mutableListOf()

    @Synchronized
    override fun d(tag: String, msg: String) {
        Log.d(tag, msg)
        mutableStoredLogs.add(LogMessage(LogLevel.DEBUG, "$tag: $msg"))
    }

    @Synchronized
    override fun i(tag: String, msg: String) {
        Log.i(tag, msg)
        mutableStoredLogs.add(LogMessage(LogLevel.INFO, "$tag: $msg"))
    }

    @Synchronized
    override fun w(tag: String, msg: String) {
        Log.w(tag, msg)
        mutableStoredLogs.add(LogMessage(LogLevel.WARNING, "$tag: $msg"))
    }

    @Synchronized
    override fun e(tag: String, msg: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(tag, msg, throwable)
        } else {
            Log.e(tag, msg)
        }
        mutableStoredLogs.add(LogMessage(LogLevel.ERROR, "$tag: $msg, ${throwable?.localizedMessage}"))
        mainHandler.post {
            Toast.makeText(applicationContext, "ERROR: $msg", Toast.LENGTH_LONG).show()
        }
    }
}
