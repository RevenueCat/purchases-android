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
    override fun d(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    override fun i(tag: String, msg: String) {
        Log.i(tag, msg)
    }

    override fun w(tag: String, msg: String) {
        Log.w(tag, msg)
    }

    override fun e(tag: String, msg: String, throwable: Throwable?) {
        if (throwable != null) {
            Log.e(tag, msg, throwable)
        } else {
            Log.e(tag, msg)
        }
        mainHandler.post {
            Toast.makeText(applicationContext, "ERROR: $msg", Toast.LENGTH_LONG).show()
        }
    }
}
