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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialContainerTransform
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.models.PurchaseOption
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.purchasePackageWith
import com.revenuecat.purchases.purchaseProductWith
import com.revenuecat.purchases.purchaseSubscriptionOptionWith
import com.revenuecat.purchases_sample.R
import com.revenuecat.purchases_sample.databinding.FragmentOfferingBinding

class OfferingFragment : Fragment(), PackageCardAdapter.PackageCardAdapterListener {

    lateinit var binding: FragmentOfferingBinding

    private val args: OfferingFragmentArgs by navArgs()
    private val offering: Offering by lazy { args.offering }
    private var activeSubscriptions: Set<String> = setOf()
    private var selectedUpgradeSubId: String? = null
    @ProrationMode
    private var selectedProrationMode: Int? = null

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

    override fun onPurchasePackageClicked(
        cardView: View,
        currentPackage: Package,
        purchaseOption: PurchaseOption?,
        isUpgrade: Boolean
    ) {
        toggleLoadingIndicator(true)

        if (isUpgrade) {
            showOldSubIdPicker()
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
        purchaseOption: PurchaseOption?,
        isUpgrade: Boolean
    ) {
        toggleLoadingIndicator(true)

        if (purchaseOption == null) {
            Purchases.sharedInstance.purchaseProductWith(
                requireActivity(),
                currentProduct,
                { error, userCancelled ->
                    toggleLoadingIndicator(false)
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
                    toggleLoadingIndicator(false)
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
        toggleLoadingIndicator(false)
        context?.let {
            Toast.makeText(
                it,
                "Successful purchase, order id: $orderId",
                Toast.LENGTH_LONG
            ).show()
            findNavController().navigateUp()
        }
    }

    private fun toggleLoadingIndicator(isLoading: Boolean) {
        binding.purchaseProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showOldSubIdPicker() {
        val activeSubIds = activeSubscriptions.map { it.split(":").first() } // TODOBC5 remove sub Id parsing
        if (activeSubIds.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Cannot ugprade without an existing active subscription.",
                Toast.LENGTH_LONG
            ).show()
            toggleLoadingIndicator(false)
            return
        }
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Switch from which currently active subscription?")
            .setSingleChoiceItems(activeSubIds.toTypedArray(), 0) { dialog_, which ->
                selectedUpgradeSubId = activeSubIds[which]
            }
            .setPositiveButton("Continue") { dialog, _ ->
                Log.e("maddietest", "upgrading from $selectedUpgradeSubId ")
                dialog.dismiss()
                showProrationModePicker()
            }
            .setNegativeButton("Cancel purchase") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    private fun showProrationModePicker() {
        val prorationModeOptions = mapOf(
            0 to "None",
            ProrationMode.IMMEDIATE_WITH_TIME_PRORATION to "IMMEDIATE_WITH_TIME_PRORATION",
            ProrationMode.IMMEDIATE_AND_CHARGE_PRORATED_PRICE to "IMMEDIATE_AND_CHARGE_PRORATED_PRICE",
            ProrationMode.IMMEDIATE_WITHOUT_PRORATION to "IMMEDIATE_WITHOUT_PRORATION",
            ProrationMode.DEFERRED to "DEFERRED",
            ProrationMode.IMMEDIATE_AND_CHARGE_FULL_PRICE to "IMMEDIATE_AND_CHARGE_FULL_PRICE"
        )

        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Switch with which ProrationMode?")
            .setSingleChoiceItems(prorationModeOptions.values.toTypedArray(), 0) { _, selectedIndex ->
                selectedProrationMode = prorationModeOptions.keys.elementAt(selectedIndex)
            }
            .setPositiveButton("Start purchase") { dialog, _ ->
                Log.e("maddietest", "upgrading with proration mode $selectedProrationMode")
                // todo start purchase
            }
            .setNegativeButton("Cancel purchase") { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }
}
