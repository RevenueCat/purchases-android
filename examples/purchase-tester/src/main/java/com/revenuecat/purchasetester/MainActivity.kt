package com.revenuecat.purchasetester

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentContainerView
import androidx.navigation.fragment.NavHostFragment
import com.revenuecat.purchases.WebPurchaseRedemption
import com.revenuecat.purchases.asWebPurchaseRedemption
import com.revenuecat.purchases_sample.R

class MainActivity : AppCompatActivity() {

    internal var webPurchaseRedemption: WebPurchaseRedemption? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        // Only process web purchase redemption if this is not a navigation intent
        if (!intent.hasExtra(NavigationDestinations.EXTRA_DESTINATION)) {
            webPurchaseRedemption = intent.asWebPurchaseRedemption()
        }

        handleNavigationDestination(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        
        // Only process web purchase redemption if this is not a navigation intent
        if (!intent.hasExtra(NavigationDestinations.EXTRA_DESTINATION)) {
            webPurchaseRedemption = intent.asWebPurchaseRedemption()
        }
        
        handleNavigationDestination(intent)
    }

    private fun handleNavigationDestination(intent: Intent) {
        val destination = intent.getStringExtra(NavigationDestinations.EXTRA_DESTINATION) ?: return

        findViewById<FragmentContainerView>(R.id.nav_host_fragment)
            ?.post {
                val navHostFragment = supportFragmentManager
                    .findFragmentById(R.id.nav_host_fragment) as? NavHostFragment
                val navController = navHostFragment?.navController ?: return@post

                when (destination) {
                    NavigationDestinations.LOGIN -> {
                        navController.navigate(R.id.action_configureFragment_to_loginFragment)
                    }

                    NavigationDestinations.LOGS -> {
                        navController.navigate(R.id.action_configureFragment_to_logsFragment)
                    }

                    NavigationDestinations.PROXY -> {
                        navController.navigate(R.id.action_configureFragment_to_proxySettingsBottomSheetFragment)
                    }
                }
            }
    }

    fun clearWebPurchaseRedemption() {
        webPurchaseRedemption = null
    }
}
