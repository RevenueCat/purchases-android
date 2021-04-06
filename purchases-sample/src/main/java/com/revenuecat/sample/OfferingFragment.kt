package com.revenuecat.sample

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialContainerTransform
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getPurchaserInfoWith
import com.revenuecat.purchases.purchasePackageWith
import com.revenuecat.sample.databinding.FragmentOfferingBinding

class OfferingFragment : Fragment(), PackageCardAdapter.PackageCardAdapterListener {

    lateinit var binding: FragmentOfferingBinding

    private val args: OfferingFragmentArgs by navArgs()
    private val offering: Offering by lazy { args.offering }
    private var activeSubscriptions: Set<String> = setOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment
            duration = requireContext().resources.getInteger(R.integer.transition_duration).toLong()
            scrimColor = Color.TRANSPARENT
        }

        Purchases.sharedInstance.getPurchaserInfoWith {
            activeSubscriptions = it.activeSubscriptions
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentOfferingBinding.inflate(inflater)
        binding.offering = offering

        binding.offeringDetailsPackagesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.offeringDetailsPackagesRecycler.adapter =
            PackageCardAdapter(
                offering.availablePackages,
                activeSubscriptions,
                this
            )

        return binding.root
    }

    override fun onBuyPackageClicked(cardView: View, currentPackage: Package) {
        Purchases.sharedInstance.purchasePackageWith(
            requireActivity(),
            currentPackage,
            { error, userCancelled ->
                if (!userCancelled) {
                    showUserError(requireActivity(), error)
                }
            }) { purchaseDetails, _ ->
            Toast.makeText(
                requireContext(),
                "Successful purchase, order id: ${purchaseDetails.orderId}",
                Toast.LENGTH_LONG
            ).show()

            findNavController().navigateUp()
        }
    }
}
