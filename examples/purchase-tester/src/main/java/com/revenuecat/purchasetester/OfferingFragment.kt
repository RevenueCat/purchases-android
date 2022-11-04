package com.revenuecat.purchasetester

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
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.purchasePackageWithOption
import com.revenuecat.purchases.purchaseProductWithOption
import com.revenuecat.purchases_sample.R
import com.revenuecat.purchases_sample.databinding.FragmentOfferingBinding

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
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        Purchases.sharedInstance.getCustomerInfoWith {
            activeSubscriptions = it.activeSubscriptions
        }

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

    override fun onPurchasePackageClicked(cardView: View, currentPackage: Package) {
        binding.purchaseProgress.visibility = View.VISIBLE
        val basePlanOption = currentPackage.product.purchaseOptions.first { it.isBasePlan }

        // TODOBC5 this was confusing, there were 4 autocomplete options... we should rename the listener conversion ones
        Purchases.sharedInstance.purchasePackageWithOption(
            requireActivity(),
            currentPackage,
            basePlanOption,
            { error, userCancelled ->
                if (!userCancelled) {
                    showUserError(requireActivity(), error)
                }
            },
            { storeTransaction, _ ->
                handleSuccessfulPurchase(storeTransaction.orderId)
            })
    }

    override fun onPurchaseProductClicked(cardView: View, currentProduct: StoreProduct) {
        binding.purchaseProgress.visibility = View.VISIBLE

        // TODOBC5 should we make this a convenience function?
        val basePlan = currentProduct.purchaseOptions.first { it.isBasePlan }
        Purchases.sharedInstance.purchaseProductWithOption(
            requireActivity(),
            currentProduct,
            basePlan,
            { error, userCancelled ->
                if (!userCancelled) {
                    showUserError(requireActivity(), error)
                }
            },
            { storeTransaction, _ ->
                handleSuccessfulPurchase(storeTransaction.orderId)
            })
    }

    private fun handleSuccessfulPurchase(orderId: String?) {
        binding.purchaseProgress.visibility = View.GONE
        context?.let {
            Toast.makeText(
                it,
                "Successful purchase, order id: $orderId",
                Toast.LENGTH_LONG
            ).show()
            findNavController().navigateUp()
        }
    }
}
