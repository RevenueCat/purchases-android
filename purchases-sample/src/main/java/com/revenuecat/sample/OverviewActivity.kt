package com.revenuecat.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getPurchaserInfoWith
import com.revenuecat.sample.databinding.ActivityOverviewBinding

class OverviewActivity : AppCompatActivity() {

    lateinit var binding: ActivityOverviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_overview)
    }

    override fun onResume() {
        super.onResume()
        Purchases.sharedInstance.getPurchaserInfoWith(::showError) { purchaserInfo ->
            binding.purchaserInfo = purchaserInfo
        }
    }
}
