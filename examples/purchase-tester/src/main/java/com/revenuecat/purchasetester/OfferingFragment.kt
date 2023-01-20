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
import com.android.billingclient.api.BillingFlowParams.ProrationMode
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialContainerTransform
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.UpgradeInfo
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.purchasePackageWith
import com.revenuecat.purchases.purchaseProductWith
import com.revenuecat.purchases.purchaseSubscriptionOptionWith
import com.revenuecat.purchases_sample.R
import com.revenuecat.purchases_sample.databinding.FragmentOfferingBinding

@SuppressWarnings("TooManyFunctions")
class OfferingFragment : Fragment(), PackageCardAdapter.PackageCardAdapterListener {

    lateinit var binding: FragmentOfferingBinding

    private val args: OfferingFragmentArgs by navArgs()
    private val offering: Offering by lazy { args.offering }
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

    private val successfulUpgradeCallback: (purchase: StoreTransaction?, customerInfo: CustomerInfo) -> Unit =
        { storeTransaction, _ ->
            toggleLoadingIndicator(false)
            handleSuccessfulPurchase(storeTransaction?.orderId)
        }

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
        isUpgrade: Boolean
    ) {
        toggleLoadingIndicator(true)

        if (isUpgrade) {
            promptForUpgradeInfo { upgradeInfo ->
                upgradeInfo?.let {
                    startPurchasePackage(currentPackage, upgradeInfo)
                }
            }
        } else {
            startPurchasePackage(currentPackage, null)
        }
    }

    override fun onPurchaseProductClicked(
        cardView: View,
        currentProduct: StoreProduct,
        isUpgrade: Boolean
    ) {
        toggleLoadingIndicator(true)

        if (isUpgrade) {
            promptForUpgradeInfo { upgradeInfo ->
                upgradeInfo?.let {
                    startPurchaseProduct(currentProduct, upgradeInfo)
                }
            }
        } else {
            startPurchaseProduct(currentProduct, null)
        }
    }

    override fun onPurchaseOptionClicked(
        cardView: View,
        subscriptionOption: SubscriptionOption,
        isUpgrade: Boolean
    ) {
        toggleLoadingIndicator(true)

        if (isUpgrade) {
            promptForUpgradeInfo { upgradeInfo ->
                upgradeInfo?.let {
                    startPurchaseOption(subscriptionOption, upgradeInfo)
                }
            }
        } else {
            startPurchaseOption(subscriptionOption, null)
        }
    }

    private fun promptForUpgradeInfo(callback: (UpgradeInfo?) -> Unit) {
        showOldSubIdPicker { subId ->
            subId?.let {
                showProrationModePicker { prorationMode ->
                    prorationMode?.let {
                        val upgradeInfo = if (prorationMode == 0) {
                            UpgradeInfo(subId)
                        } else {
                            UpgradeInfo(subId, prorationMode)
                        }
                        callback(upgradeInfo)
                    } ?: callback(null)
                }
            } ?: callback(null)
        }
    }

    private fun startPurchaseProduct(
        currentProduct: StoreProduct,
        upgradeInfo: UpgradeInfo?
    ) {
        when {
            upgradeInfo == null -> Purchases.sharedInstance.purchaseProductWith(
                requireActivity(),
                currentProduct,
                purchaseErrorCallback,
                successfulPurchaseCallback
            )
            upgradeInfo != null -> Purchases.sharedInstance.purchaseProductWith(
                requireActivity(),
                currentProduct,
                upgradeInfo,
                purchaseErrorCallback,
                successfulUpgradeCallback
            )
        }
    }

    private fun startPurchaseOption(
        subscriptionOption: SubscriptionOption,
        upgradeInfo: UpgradeInfo?
    ) {
        when {
            upgradeInfo == null -> Purchases.sharedInstance.purchaseSubscriptionOptionWith(
                requireActivity(),
                subscriptionOption,
                purchaseErrorCallback,
                successfulPurchaseCallback
            )
            upgradeInfo != null -> Purchases.sharedInstance.purchaseSubscriptionOptionWith(
                requireActivity(),
                subscriptionOption,
                upgradeInfo,
                purchaseErrorCallback,
                successfulUpgradeCallback
            )
        }
    }

    private fun handleSuccessfulPurchase(orderId: String?) {
        context?.let {
            Toast.makeText(
                it,
                "Successful purchase, order id: $orderId",
                Toast.LENGTH_SHORT
            ).show()
            findNavController().navigateUp()
        }
    }

    private fun startPurchasePackage(
        currentPackage: Package,
        upgradeInfo: UpgradeInfo?
    ) {
        when {
            upgradeInfo == null -> Purchases.sharedInstance.purchasePackageWith(
                requireActivity(),
                currentPackage,
                purchaseErrorCallback,
                successfulPurchaseCallback
            )
            upgradeInfo != null -> Purchases.sharedInstance.purchasePackageWith(
                requireActivity(),
                currentPackage,
                upgradeInfo,
                purchaseErrorCallback,
                successfulUpgradeCallback
            )
        }
    }

    private fun toggleLoadingIndicator(isLoading: Boolean) {
        binding.purchaseProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showOldSubIdPicker(callback: (String?) -> Unit) {
        val activeSubIds = activeSubscriptions.map { it.split(":").first() } // TODOBC5 remove sub Id parsing
        if (activeSubIds.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Cannot upgrade without an existing active subscription.",
                Toast.LENGTH_LONG
            ).show()
            toggleLoadingIndicator(false)
            callback(null)
            return
        }

        var selectedUpgradeSubId: String = activeSubIds[0]
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose which active sub to switch from")
            .setSingleChoiceItems(activeSubIds.toTypedArray(), 0) { _, which ->
                selectedUpgradeSubId = activeSubIds[which]
            }
            .setPositiveButton("Continue") { dialog, _ ->
                dialog.dismiss()
                callback(selectedUpgradeSubId)
            }
            .setNegativeButton("Cancel purchase") { dialog, _ ->
                dialog.dismiss()
                toggleLoadingIndicator(false)
                callback(null)
            }
            .setOnDismissListener {
                toggleLoadingIndicator(false)
                callback(null)
            }
            .show()
    }

    private fun showProrationModePicker(callback: (Int?) -> Unit) {
        val prorationModeOptions = mapOf(
            0 to "None",
            ProrationMode.IMMEDIATE_WITH_TIME_PRORATION to ProrationMode::IMMEDIATE_WITH_TIME_PRORATION.name,
            ProrationMode.IMMEDIATE_AND_CHARGE_PRORATED_PRICE to
                ProrationMode::IMMEDIATE_AND_CHARGE_PRORATED_PRICE.name,
            ProrationMode.IMMEDIATE_WITHOUT_PRORATION to ProrationMode::IMMEDIATE_WITHOUT_PRORATION.name,
            ProrationMode.DEFERRED to ProrationMode::DEFERRED.name,
            ProrationMode.IMMEDIATE_AND_CHARGE_FULL_PRICE to ProrationMode::IMMEDIATE_AND_CHARGE_FULL_PRICE.name
        )
        @ProrationMode var selectedProrationMode = 0
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose ProrationMode")
            .setSingleChoiceItems(prorationModeOptions.values.toTypedArray(), 0) { _, selectedIndex ->
                selectedProrationMode = prorationModeOptions.keys.elementAt(selectedIndex)
            }
            .setPositiveButton("Start purchase") { dialog, _ ->
                dialog.dismiss()
                callback(selectedProrationMode)
            }
            .setNegativeButton("Cancel purchase") { dialog, _ ->
                dialog.dismiss()
                toggleLoadingIndicator(false)
                callback(null)
            }
            .setOnDismissListener {
                toggleLoadingIndicator(false)
                callback(null)
            }
            .show()
    }
}
