package com.revenuecat.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.databinding.DataBindingUtil
import com.revenuecat.purchases.EntitlementInfo
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
        Purchases.sharedInstance.getPurchaserInfoWith(::showError) { info ->
            with(binding) {
                purchaserInfo = info

                purchaserInfoJsonObject.detail = info.jsonObject.toString(JSON_FORMATTER_INDENT_SPACES)

                purchaserInfoActiveEntitlements.detail = formatEntitlements(info.entitlements.active.values)
                purchaserInfoAllEntitlements.detail = formatEntitlements(info.entitlements.all.values)
            }
        }
    }

    private fun formatEntitlements(entitlementInfos: Collection<EntitlementInfo>): String {
        return entitlementInfos.joinToString(separator = "\n") { it.toBriefString() }
    }

    companion object {
        private const val JSON_FORMATTER_INDENT_SPACES = 4
    }
}
