package com.revenuecat.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.LinearLayoutManager
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.getPurchaserInfoWith
import com.revenuecat.sample.databinding.ActivityOverviewBinding
import org.json.JSONObject

class OverviewActivity : AppCompatActivity() {

    lateinit var binding: ActivityOverviewBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this, R.layout.activity_overview)

        binding.purchaserInfoCard.setOnClickListener {
            with(binding.purchaserInfoDetailsContainer) {
                visibility = if (visibility == View.GONE) View.VISIBLE else View.GONE
            }
        }
    }

    override fun onResume() {
        super.onResume()
        Purchases.sharedInstance.getPurchaserInfoWith(::showError) { info ->
            with(binding) {
                purchaserInfo = info

                purchaserInfoJsonObject.detail =
                    JSONObject(info.jsonObject.toString()).toString(JSON_FORMATTER_INDENT_SPACES)

                purchaserInfoActiveEntitlements.detail = formatEntitlements(info.entitlements.active.values)
                purchaserInfoAllEntitlements.detail = formatEntitlements(info.entitlements.all.values)
            }
        }

        Purchases.sharedInstance.getOfferingsWith(::showError, ::populateOfferings)
    }

    private fun populateOfferings(offerings: Offerings) {
        binding.overviewOfferingsRecycler.layoutManager = LinearLayoutManager(this)
        binding.overviewOfferingsRecycler.adapter = OfferingCardAdapter(offerings.all.values.toList())
    }

    private fun formatEntitlements(entitlementInfos: Collection<EntitlementInfo>): String {
        if (entitlementInfos.isEmpty()) return "None"

        var formattedString = ""
        entitlementInfos.forEachIndexed { index, entitlementInfo ->
            formattedString += entitlementInfo.toBriefString() +
                if (index != entitlementInfos.size - 1) "\n" else ""
        }

        return formattedString
    }

    companion object {
        private const val JSON_FORMATTER_INDENT_SPACES = 4
    }
}
