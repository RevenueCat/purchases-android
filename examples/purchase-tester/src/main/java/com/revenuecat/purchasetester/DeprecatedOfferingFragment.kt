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
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.purchasePackageWith
import com.revenuecat.purchases.purchaseProductWith
import com.revenuecat.purchases_sample.R
import com.revenuecat.purchases_sample.databinding.FragmentOfferingBinding

@SuppressWarnings("TooManyFunctions")
class DeprecatedOfferingFragment : Fragment(), DeprecatedPackageCardAdapter.PackageCardAdapterListener {

    lateinit var binding: FragmentOfferingBinding

    private val args: OfferingFragmentArgs by navArgs()
    private val offeringId: String by lazy { args.offeringId }
    private var activeSubscriptions: Set<String> = setOf()

    private val purchaseErrorCallback: (error: PurchasesError, userCancelled: Boolean) -> Unit =
        { error, userCancelled ->
            toggleLoadingIndicator(false)
            if (!userCancelled) {
                showUserError(requireActivity(), error)
            }
        }

    private val successfulPurchaseCallback: (purchase: StoreTransaction, customerInfo: CustomerInfo) -> Unit =
        { storeTransaction, _ ->
            toggleLoadingIndicator(false)
            handleSuccessfulPurchase(storeTransaction.orderId)
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment
            duration = requireContext().resources.getInteger(R.integer.transition_duration).toLong()
            scrimColor = Color.TRANSPARENT
        }
    }

    private fun populateOfferings(offerings: Offerings) {
        val offering = offerings.getOffering(offeringId) ?: return
        binding.offering = offering

        binding.offeringDetailsPackagesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.offeringDetailsPackagesRecycler.adapter =
            DeprecatedPackageCardAdapter(
                offering.availablePackages,
                activeSubscriptions,
                this,
            )
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Purchases.sharedInstance.getCustomerInfoWith {
            activeSubscriptions = it.activeSubscriptions
        }
        binding = FragmentOfferingBinding.inflate(inflater)

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Purchases.sharedInstance.getOfferingsWith(::showError, ::populateOfferings)
    }

    override fun onPurchasePackageClicked(
        cardView: View,
        currentPackage: Package,
    ) {
        toggleLoadingIndicator(true)

        startPurchasePackage(currentPackage)
    }

    override fun onPurchaseProductClicked(
        cardView: View,
        currentProduct: StoreProduct,
    ) {
        toggleLoadingIndicator(true)

        startPurchaseProduct(currentProduct)
    }

    private fun startPurchaseProduct(
        currentProduct: StoreProduct,
    ) {
        Purchases.sharedInstance.purchaseProductWith(
            requireActivity(),
            currentProduct,
            purchaseErrorCallback,
            successfulPurchaseCallback,
        )
    }

    private fun handleSuccessfulPurchase(orderId: String?) {
        context?.let {
            Toast.makeText(
                it,
                "Successful purchase, order id: $orderId",
                Toast.LENGTH_SHORT,
            ).show()
            findNavController().navigateUp()
        }
    }

    private fun startPurchasePackage(
        currentPackage: Package,
    ) {
        Purchases.sharedInstance.purchasePackageWith(
            requireActivity(),
            currentPackage,
            purchaseErrorCallback,
            successfulPurchaseCallback,
        )
    }

    private fun toggleLoadingIndicator(isLoading: Boolean) {
        binding.purchaseProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
    }
}
