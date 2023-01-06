package com.revenuecat.purchasetester

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.android.billingclient.api.BillingFlowParams.ProrationMode
import com.google.android.material.transition.MaterialContainerTransform
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.ProductType
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.UpgradeInfo
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.models.PurchaseOption
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.purchasePackageWith
import com.revenuecat.purchases.purchaseProductWith
import com.revenuecat.purchases.purchaseSubscriptionOptionWith
import com.revenuecat.purchases_sample.R
import com.revenuecat.purchases_sample.databinding.FragmentOfferingBinding

@Suppress("LongMethod")
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

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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

    override fun onPurchasePackageClicked(cardView: View, currentPackage: Package, purchaseOption: PurchaseOption?) {
        binding.purchaseProgress.visibility = View.VISIBLE

        // TODO is this true? is it possible to have multiple subs to one app at once,
        //  in which case we don't do an update but simply purchase again?
        val isUpgrade = currentPackage.product.type == ProductType.SUBS && activeSubscriptions.isNotEmpty()

        if (isUpgrade) {
            // TODO how do we know which one to upgrade from if theres <1?
            val currentSubIdBAD = activeSubscriptions.first()
            val currentSubId = currentSubIdBAD.split(":").first()
            val upgradeInfo = UpgradeInfo(currentSubId, ProrationMode.IMMEDIATE_AND_CHARGE_FULL_PRICE)

            Log.e("maddietest", "upgrading from $currentSubId to ${currentPackage.product.productId} with " +
                "proration ${upgradeInfo.prorationMode}")

            // TODO should we just make upgradeinfo default to null in the purchasePackage method
            //  to avoid this duplicate code?
            if (purchaseOption == null) {
                Purchases.sharedInstance.purchasePackageWith(
                    requireActivity(),
                    currentPackage,
                    upgradeInfo,
                    { error, userCancelled ->
                        if (!userCancelled) {
                            showUserError(requireActivity(), error)
                        }
                    },
                    { storeTransaction, _ ->
                        handleSuccessfulPurchase(storeTransaction?.orderId)
                    }
                )
            } else {
                Purchases.sharedInstance.purchaseSubscriptionOptionWith(
                    requireActivity(),
                    currentPackage.product,
                    purchaseOption,
                    upgradeInfo,
                    { error, userCancelled ->
                        if (!userCancelled) {
                            showUserError(requireActivity(), error)
                        }
                    },
                    { storeTransaction, _ ->
                        handleSuccessfulPurchase(storeTransaction?.orderId)
                    })
            }
        } else {
            if (purchaseOption == null) {
                Purchases.sharedInstance.purchasePackageWith(
                    requireActivity(),
                    currentPackage,
                    { error, userCancelled ->
                        if (!userCancelled) {
                            showUserError(requireActivity(), error)
                        }
                    },
                    { storeTransaction, _ ->
                        handleSuccessfulPurchase(storeTransaction.orderId)
                    }
                )
            } else {
                Purchases.sharedInstance.purchaseSubscriptionOptionWith(
                    requireActivity(),
                    currentPackage.product,
                    purchaseOption,
                    { error, userCancelled ->
                        if (!userCancelled) {
                            showUserError(requireActivity(), error)
                        }
                    },
                    { storeTransaction, _ ->
                        handleSuccessfulPurchase(storeTransaction.orderId)
                    })
            }
        }
    }

    override fun onPurchaseProductClicked(
        cardView: View,
        currentProduct: StoreProduct,
        purchaseOption: PurchaseOption?
    ) {
        binding.purchaseProgress.visibility = View.VISIBLE

        if (purchaseOption == null) {
            Purchases.sharedInstance.purchaseProductWith(
                requireActivity(),
                currentProduct,
                { error, userCancelled ->
                    if (!userCancelled) {
                        showUserError(requireActivity(), error)
                    }
                },
                { storeTransaction, _ ->
                    handleSuccessfulPurchase(storeTransaction.orderId)
                }
            )
        } else {
            Purchases.sharedInstance.purchaseSubscriptionOptionWith(
                requireActivity(),
                currentProduct,
                purchaseOption,
                { error, userCancelled ->
                    if (!userCancelled) {
                        showUserError(requireActivity(), error)
                    }
                },
                { storeTransaction, _ ->
                    handleSuccessfulPurchase(storeTransaction.orderId)
                })
        }
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
