package com.revenuecat.sample

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.getPurchaserInfoWith
import com.revenuecat.sample.databinding.ActivityOverviewBinding
import kotlinx.android.synthetic.main.activity_overview.view.*
import org.json.JSONObject

class OverviewActivity : AppCompatActivity() {

    lateinit var binding: ActivityOverviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_overview)

        binding.purchaserInfoCard.setOnClickListener { card ->
            card.purchaser_info_details_container.visibility = View.VISIBLE
        }
    }

    override fun onResume() {
        super.onResume()
        Purchases.sharedInstance.getPurchaserInfoWith(::showError) { purchaserInfo ->
            binding.purchaserInfo = purchaserInfo

            binding.purchaserInfoJsonObject.text = JSONObject(purchaserInfo.jsonObject.toString()).toString(2)
            binding.purchaserInfoAppActiveEntitlements.detail = formatEntitlements(purchaserInfo.entitlements.active)
            binding.purchaserInfoAppAllEntitlements.detail = formatEntitlements(purchaserInfo.entitlements.all)
        }

        Purchases.sharedInstance.getOfferingsWith(::showError, ::populateOfferings)
    }

    private fun populateOfferings(offerings: Offerings) {
        binding.overviewOfferingsRecycler.layoutManager = LinearLayoutManager(this)
        binding.overviewOfferingsRecycler.adapter = OfferingCardAdapter(offerings.all.values.toList())
    }

    private fun formatEntitlements(entitlementInfoMap: Map<String, EntitlementInfo>): String {
        if (entitlementInfoMap.isEmpty()) return "None"

        var formattedString = ""
        for (entitlementInfo in entitlementInfoMap) {
            formattedString += "${entitlementInfo.key} -> expires ${entitlementInfo.value.expirationDate}\n"
        }
        return formattedString
    }
}
