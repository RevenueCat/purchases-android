package com.revenuecat.purchasetester

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialElevationScale
import com.revenuecat.purchases.EntitlementInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.logOutWith
import com.revenuecat.purchases.restorePurchasesWith
import com.revenuecat.purchasetester.databinding.FragmentOverviewBinding

class OverviewFragment : Fragment(), OfferingCardAdapter.OfferingCardAdapterListener {

    private lateinit var viewModel: OverviewViewModel
    private lateinit var binding: FragmentOverviewBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentOverviewBinding.inflate(inflater)

        binding.customerInfoCard.setOnClickListener {
            with(binding.customerInfoDetailsContainer) {
                visibility = if (visibility == View.GONE) View.VISIBLE else View.GONE
            }
        }

        binding.customerInfoLogoutButton.setOnClickListener {
            if (Purchases.sharedInstance.isAnonymous) {
                findNavController().navigateUp()
            } else {
                Purchases.sharedInstance.logOutWith(
                    { error -> showUserError(requireActivity(), error) },
                    { findNavController().navigateUp() }
                )
            }
        }

        viewModel = OverviewViewModel()
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        Purchases.sharedInstance.getCustomerInfoWith(::showError) { info ->
            with(binding) {
                viewModel?.customerInfo?.value = info

                // TODO move more of this into binding
                customerInfoCopyUserIdButton.setOnClickListener {
                    copyToClipboard(requireContext(), "RevenueCat userId", info.originalAppUserId)
                }

                customerInfoJsonObject.detail = info.rawData.toString(JSON_FORMATTER_INDENT_SPACES)

                binding.customerInfoManageButton.setOnClickListener {
                    info.managementURL?.let {
                        val intent = Intent(Intent.ACTION_VIEW)
                        intent.data = it
                        startActivity(intent)
                    }
                }
            }
        }

        binding.customerInfoRestorePurchasesButton.setOnClickListener {
            binding.customerInfoRestoreProgress.visibility = View.VISIBLE
            Purchases.sharedInstance.restorePurchasesWith(onSuccess = {
                viewModel.customerInfo.postValue(it)
                Toast.makeText(
                    requireContext(),
                    "Restoring purchases successful, check for new customer info",
                    Toast.LENGTH_LONG
                ).show()
                binding.customerInfoRestoreProgress.visibility = View.GONE
            }, onError = {
                showError(it)
                binding.customerInfoRestoreProgress.visibility = View.GONE
            })
        }

        Purchases.sharedInstance.getOfferingsWith(::showError, ::populateOfferings)
    }

    private fun populateOfferings(offerings: Offerings) {
        if (offerings.all.isEmpty()) {
            binding.offeringHeader.text = "No Offerings"
            return
        }

        binding.overviewOfferingsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.overviewOfferingsRecycler.adapter = OfferingCardAdapter(
            offerings.all.values.toList(),
            offerings.current,
            this
        )
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
}
