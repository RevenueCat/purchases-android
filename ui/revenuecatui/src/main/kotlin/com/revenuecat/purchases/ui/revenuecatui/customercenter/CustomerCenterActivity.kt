package com.revenuecat.purchases.ui.revenuecatui.customercenter

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.Modifier

internal class CustomerCenterActivity : ComponentActivity() {
    companion object {
        internal fun createIntent(context: Context): Intent {
            return Intent(context, CustomerCenterActivity::class.java)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            val isDarkTheme = isSystemInDarkTheme()
            val colorScheme = if (isDarkTheme) darkColorScheme() else lightColorScheme()

            MaterialTheme(
                colorScheme = colorScheme,
            ) {
                CustomerCenter(
                    modifier = Modifier.fillMaxSize(),
                    onDismiss = {
                        setResult(RESULT_OK)
                        finish()
                    },
                )
            }
        }
    }
}
