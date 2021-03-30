package com.revenuecat.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.getPurchaserInfoWith
import com.revenuecat.sample.databinding.FragmentOverviewBinding
import org.json.JSONObject

class OverviewFragment : Fragment() {

    lateinit var binding: FragmentOverviewBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentOverviewBinding.inflate(inflater)

        binding.purchaserInfoCard.setOnClickListener {
            with(binding.purchaserInfoDetailsContainer) {
                visibility = if (visibility == View.GONE) View.VISIBLE else View.GONE
            }
        }

        return binding.root
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
        binding.overviewOfferingsRecycler.layoutManager = LinearLayoutManager(requireContext())
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
