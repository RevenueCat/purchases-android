package com.revenuecat.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getPurchaserInfoWith
import com.revenuecat.sample.databinding.ActivityOverviewBinding
import kotlinx.android.synthetic.main.activity_overview.view.*
import org.json.JSONObject

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

            binding.purchaserInfoJsonObject.detail =
                JSONObject(purchaserInfo.jsonObject.toString()).toString(JSON_FORMATTER_INDENT_SPACES)
            binding.purchaserInfoAppActiveEntitlements.detail =
                formatEntitlements(purchaserInfo.entitlements.active.values)
            binding.purchaserInfoAppAllEntitlements.detail = formatEntitlements(purchaserInfo.entitlements.all.values)
        }
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
