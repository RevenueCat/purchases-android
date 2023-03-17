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
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.models.GoogleProrationMode
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases.purchaseWith
import com.revenuecat.purchases_sample.R
import com.revenuecat.purchases_sample.databinding.FragmentOfferingBinding

@SuppressWarnings("TooManyFunctions")
class OfferingFragment : Fragment(), PackageCardAdapter.PackageCardAdapterListener {

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

    private val successfulPurchaseCallback: (purchase: StoreTransaction?, customerInfo: CustomerInfo) -> Unit =
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

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Purchases.sharedInstance.getOfferingsWith(::showError, ::populateOfferings)
    }

    private fun populateOfferings(offerings: Offerings) {
        val offering = offerings.getOffering(offeringId) ?: return
        binding.offering = offering

        binding.offeringDetailsPackagesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.offeringDetailsPackagesRecycler.adapter =
            PackageCardAdapter(
                offering.availablePackages,
                activeSubscriptions,
                this
            )
    }

    override fun onPurchasePackageClicked(
        cardView: View,
        currentPackage: Package,
        isUpgrade: Boolean
    ) = startPurchase(isUpgrade, PurchaseParams.Builder(requireActivity(), currentPackage))

    override fun onPurchaseProductClicked(
        cardView: View,
        currentProduct: StoreProduct,
        isUpgrade: Boolean
    ) = startPurchase(isUpgrade, PurchaseParams.Builder(requireActivity(), currentProduct))

    override fun onPurchaseSubscriptionOptionClicked(
        cardView: View,
        subscriptionOption: SubscriptionOption,
        isUpgrade: Boolean
    ) = startPurchase(isUpgrade, PurchaseParams.Builder(requireActivity(), subscriptionOption))

    private fun startPurchase(
        isUpgrade: Boolean,
        purchaseParamsBuilder: PurchaseParams.Builder
    ) {
        toggleLoadingIndicator(true)
        if (isUpgrade) {
            promptForProductChangeInfo { oldProductId, prorationMode ->
                oldProductId?.let {
                    purchaseParamsBuilder.oldProductId(it)

                    prorationMode?.let {
                        purchaseParamsBuilder.googleProrationMode(prorationMode)
                    }
                    showPersonalizedPricePicker { personalizedPrice, _ ->
                        personalizedPrice?.let {
                            purchaseParamsBuilder.isPersonalizedPrice(it)
                        }
                        Purchases.sharedInstance.purchaseWith(
                            purchaseParamsBuilder.build(),
                            purchaseErrorCallback,
                            successfulPurchaseCallback
                        )
                    }
                }
            }
        } else {
            showPersonalizedPricePicker { personalizedPrice, _ ->
                personalizedPrice?.let {
                    purchaseParamsBuilder.isPersonalizedPrice(it)
                }
                Purchases.sharedInstance.purchaseWith(
                    purchaseParamsBuilder.build(),
                    purchaseErrorCallback,
                    successfulPurchaseCallback
                )
            }
        }
    }

    private fun promptForProductChangeInfo(callback: (String?, GoogleProrationMode?) -> Unit) {
        showOldSubIdPicker { subId ->
            subId?.let {
                showProrationModePicker { prorationMode, error ->
                    if (error == null) {
                        prorationMode?.let {
                            callback(subId, it)
                        } ?: callback(subId, null)
                    } else {
                        callback(null, null)
                    }
                }
            } ?: callback(null, null)
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
            .setOnCancelListener {
                toggleLoadingIndicator(false)
                callback(null)
            }
            .show()
    }

    private fun showProrationModePicker(callback: (GoogleProrationMode?, Error?) -> Unit) {
        val prorationModeOptions = GoogleProrationMode.values()
        var selectedProrationMode: GoogleProrationMode? = null

        val prorationModeNames = prorationModeOptions.map { it.name }.toTypedArray()
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Choose ProrationMode")
            .setSingleChoiceItems(prorationModeNames, -1) { _, selectedIndex ->
                selectedProrationMode = prorationModeOptions.elementAt(selectedIndex)
            }
            .setPositiveButton("Start purchase") { dialog, _ ->
                dialog.dismiss()
                callback(selectedProrationMode, null)
            }
            .setNegativeButton("Cancel purchase") { dialog, _ ->
                dialog.dismiss()
                toggleLoadingIndicator(false)
                callback(null, Error("Purchase cancelled"))
            }
            .setOnCancelListener {
                toggleLoadingIndicator(false)
                callback(null, Error("Selection dismissed"))
            }
            .show()
    }

    private fun showPersonalizedPricePicker(callback: (Boolean?, Error?) -> Unit) {
        val personalizedPriceOptions = arrayOf(true, false, null)
        var selectedPersonalizedPrice: Boolean? = null

        val personalizedPriceNames = arrayOf("True", "False", "Null")
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("setIsPersonalizedPrice?")
            .setSingleChoiceItems(personalizedPriceNames, -1) { _, selectedIndex ->
                selectedPersonalizedPrice = personalizedPriceOptions.elementAt(selectedIndex)
            }
            .setPositiveButton("Start purchase") { dialog, _ ->
                dialog.dismiss()
                callback(selectedPersonalizedPrice, null)
            }
            .setNegativeButton("Cancel purchase") { dialog, _ ->
                dialog.dismiss()
                toggleLoadingIndicator(false)
                callback(null, Error("Purchase cancelled"))
            }
            .setOnCancelListener {
                toggleLoadingIndicator(false)
                callback(null, Error("Selection dismissed"))
            }
            .show()
    }
}
