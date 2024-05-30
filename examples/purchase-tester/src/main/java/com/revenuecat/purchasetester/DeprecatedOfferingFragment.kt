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
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialContainerTransform
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.UpgradeInfo
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.models.GoogleReplacementMode
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
        isUpgrade: Boolean,
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
        isUpgrade: Boolean,
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

    private fun promptForUpgradeInfo(callback: (UpgradeInfo?) -> Unit) {
        showOldSubIdPicker { subId ->
            subId?.let {
                showReplacementModePicker { replacementMode, error ->
                    if (error == null) {
                        replacementMode?.let {
                            callback(UpgradeInfo(subId, replacementMode.playBillingClientMode))
                        } ?: callback(UpgradeInfo(subId))
                    } else {
                        callback(null)
                    }
                }
            } ?: callback(null)
        }
    }

    private fun startPurchaseProduct(
        currentProduct: StoreProduct,
        upgradeInfo: UpgradeInfo?,
    ) {
        when {
            upgradeInfo == null -> Purchases.sharedInstance.purchaseProductWith(
                requireActivity(),
                currentProduct,
                purchaseErrorCallback,
                successfulPurchaseCallback,
            )
            upgradeInfo != null -> Purchases.sharedInstance.purchaseProductWith(
                requireActivity(),
                currentProduct,
                upgradeInfo,
                purchaseErrorCallback,
                successfulUpgradeCallback,
            )
        }
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
        upgradeInfo: UpgradeInfo?,
    ) {
        when {
            upgradeInfo == null -> Purchases.sharedInstance.purchasePackageWith(
                requireActivity(),
                currentPackage,
                purchaseErrorCallback,
                successfulPurchaseCallback,
            )
            upgradeInfo != null -> Purchases.sharedInstance.purchasePackageWith(
                requireActivity(),
                currentPackage,
                upgradeInfo,
                purchaseErrorCallback,
                successfulUpgradeCallback,
            )
        }
    }

    private fun toggleLoadingIndicator(isLoading: Boolean) {
        binding.purchaseProgress.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    private fun showOldSubIdPicker(callback: (String?) -> Unit) {
        // Removes base plan id to get the sub id
        val activeSubIds = activeSubscriptions.map { it.split(":").first() }
        if (activeSubIds.isEmpty()) {
            Toast.makeText(
                requireContext(),
                "Cannot upgrade without an existing active subscription.",
                Toast.LENGTH_LONG,
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

    private fun showReplacementModePicker(callback: (GoogleReplacementMode?, Error?) -> Unit) {
        val replacementModeOptions = GoogleReplacementMode.values()
        var selectedReplacementMode: GoogleReplacementMode? = null

        val replacementModeNames = replacementModeOptions.map { it.name }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose ReplacementMode")
            .setSingleChoiceItems(replacementModeNames, -1) { _, selectedIndex ->
                selectedReplacementMode = replacementModeOptions.elementAt(selectedIndex)
            }
            .setPositiveButton("Start purchase") { dialog, _ ->
                dialog.dismiss()
                callback(selectedReplacementMode, null)
            }
            .setNegativeButton("Cancel purchase") { dialog, _ ->
                dialog.dismiss()
                toggleLoadingIndicator(false)
                callback(null, Error("Purchase cancelled"))
            }
            .setOnDismissListener {
                toggleLoadingIndicator(false)
                callback(null, Error("Selection dismissed"))
            }
            .show()
    }
}
