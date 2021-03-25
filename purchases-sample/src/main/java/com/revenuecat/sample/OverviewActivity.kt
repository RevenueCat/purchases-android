package com.revenuecat.sample

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.transition.TransitionManager
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getPurchaserInfoWith
import com.revenuecat.sample.databinding.ActivityOverviewBinding
import kotlinx.android.synthetic.main.activity_overview.view.*

class OverviewActivity : AppCompatActivity() {

    lateinit var binding: ActivityOverviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_overview)

        binding.purchaserInfoCard.setOnClickListener { card ->
            TransitionManager.beginDelayedTransition(card.purchaser_info_card_content)
            card.purchaser_info_details_container.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        Purchases.sharedInstance.getPurchaserInfoWith(::showError) { purchaserInfo ->
            binding.purchaserInfo = purchaserInfo
        }
    }
}
