package com.revenuecat.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialElevationScale
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.getPurchaserInfoWith
import com.revenuecat.sample.databinding.FragmentOverviewBinding

class OverviewFragment : Fragment(), OfferingCardAdapter.OfferingCardAdapterListener {

    lateinit var binding: FragmentOverviewBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentOverviewBinding.inflate(inflater)

        binding.purchaserInfoCard.setOnClickListener {
            with(binding.purchaserInfoDetailsContainer) {
                visibility = if (visibility == View.GONE) View.VISIBLE else View.GONE

                binding.purchaserInfoCardExpandButton
                    .animate()
                    .rotationBy(
                        if (visibility == View.GONE) -ARROW_ANIMATION_ROTATION_DEGREES
                        else ARROW_ANIMATION_ROTATION_DEGREES
                    )
                    .setDuration(resources.getInteger(R.integer.transition_duration).toLong())
                    .start()
            }
        }
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        Purchases.sharedInstance.getPurchaserInfoWith(::showError) { info ->
            with(binding) {
                purchaserInfo = info

                purchaserInfoJsonObject.detail = info.jsonObject.toString(JSON_FORMATTER_INDENT_SPACES)

                purchaserInfoActiveEntitlements.detail = formatEntitlements(info.entitlements.active.values)
                purchaserInfoAllEntitlements.detail = formatEntitlements(info.entitlements.all.values)
            }
        }

        Purchases.sharedInstance.getOfferingsWith(::showError, ::populateOfferings)
    }

    private fun populateOfferings(offerings: Offerings) {
        binding.overviewOfferingsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.overviewOfferingsRecycler.adapter = OfferingCardAdapter(offerings.all.values.toList(), this)
    }

    private fun formatEntitlements(entitlementInfos: Collection<EntitlementInfo>): String {
        return entitlementInfos.joinToString(separator = "\n") { it.toBriefString() }
    }

    override fun onOfferingClicked(cardView: View, offering: Offering) {
        exitTransition = MaterialElevationScale(false).apply {
            duration = requireContext().resources.getInteger(R.integer.transition_duration).toLong()
        }
        reenterTransition = MaterialElevationScale(true).apply {
            duration = requireContext().resources.getInteger(R.integer.transition_duration).toLong()
        }

        val offeringCardTransitionName = getString(R.string.offering_fragment_transition)
        val extras = FragmentNavigatorExtras(cardView to offeringCardTransitionName)
        val directions = OverviewFragmentDirections.actionOverviewFragmentToOfferingFragment(offering)
        findNavController().navigate(directions, extras)
    }

    companion object {
        private const val JSON_FORMATTER_INDENT_SPACES = 4
        private const val ARROW_ANIMATION_ROTATION_DEGREES = 180f
    }
}
