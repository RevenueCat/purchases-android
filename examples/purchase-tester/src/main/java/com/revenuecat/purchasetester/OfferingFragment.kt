package com.revenuecat.purchasetester

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.transition.MaterialContainerTransform
import com.revenuecat.purchases.ExperimentalPreviewRevenueCatPurchasesAPI
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.Package
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.PurchasesTransactionException
import com.revenuecat.purchases.awaitPurchase
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.models.GoogleProrationMode
import com.revenuecat.purchases.models.GooglePurchasingData
import com.revenuecat.purchases.models.PurchasingData
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.purchases_sample.R
import com.revenuecat.purchases_sample.databinding.FragmentOfferingBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@SuppressWarnings("TooManyFunctions")
@OptIn(ExperimentalPreviewRevenueCatPurchasesAPI::class)
class OfferingFragment : Fragment(), PackageCardAdapter.PackageCardAdapterListener {

    lateinit var binding: FragmentOfferingBinding

    private val args: OfferingFragmentArgs by navArgs()
    private val offeringId: String by lazy { args.offeringId }
    private var activeSubscriptions: Set<String> = setOf()
    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var dataStoreUtils: DataStoreUtils
    private var isPlayStore: Boolean = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment
            duration = requireContext().resources.getInteger(R.integer.transition_duration).toLong()
            scrimColor = Color.TRANSPARENT
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dataStoreUtils = DataStoreUtils(requireActivity().applicationContext.configurationDataStore)
        binding = FragmentOfferingBinding.inflate(inflater)

        lifecycleScope.launch {
            dataStoreUtils.getSdkConfig().onEach { sdkConfiguration ->
                isPlayStore = !sdkConfiguration.useAmazon
            }.collect()
        }

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
                this,
                isPlayStore,
            )
    }

    override fun onPurchasePackageClicked(
        cardView: View,
        currentPackage: Package,
        isUpgrade: Boolean,
        isPersonalizedPrice: Boolean,
    ) {
        if (Purchases.sharedInstance.finishTransactions) {
            startPurchase(isUpgrade, isPersonalizedPrice, PurchaseParams.Builder(requireActivity(), currentPackage))
        } else {
            startObserverModePurchase(currentPackage.product.purchasingData)
        }
    }

    override fun onPurchaseProductClicked(
        cardView: View,
        currentProduct: StoreProduct,
        isUpgrade: Boolean,
        isPersonalizedPrice: Boolean,
    ) {
        if (Purchases.sharedInstance.finishTransactions) {
            startPurchase(isUpgrade, isPersonalizedPrice, PurchaseParams.Builder(requireActivity(), currentProduct))
        } else {
            startObserverModePurchase(currentProduct.purchasingData)
        }
    }

    override fun onPurchaseSubscriptionOptionClicked(
        cardView: View,
        subscriptionOption: SubscriptionOption,
        isUpgrade: Boolean,
        isPersonalizedPrice: Boolean,
    ) {
        if (Purchases.sharedInstance.finishTransactions) {
            startPurchase(isUpgrade, isPersonalizedPrice, PurchaseParams.Builder(requireActivity(), subscriptionOption))
        } else {
            startObserverModePurchase(subscriptionOption.purchasingData)
        }
    }

    private fun startPurchase(
        isUpgrade: Boolean,
        isPersonalizedPrice: Boolean,
        purchaseParamsBuilder: PurchaseParams.Builder,
    ) {
        toggleLoadingIndicator(true)
        if (isUpgrade) {
            promptForProductChangeInfo { oldProductId, prorationMode ->
                oldProductId?.let {
                    purchaseParamsBuilder.oldProductId(it)

                    prorationMode?.let {
                        purchaseParamsBuilder.googleProrationMode(prorationMode)
                    }

                    if (isPersonalizedPrice) {
                        purchaseParamsBuilder.isPersonalizedPrice(isPersonalizedPrice)
                    }

                    purchase(purchaseParamsBuilder.build())
                }
            }
        } else {
            if (isPersonalizedPrice) {
                purchaseParamsBuilder.isPersonalizedPrice(isPersonalizedPrice)
            }
            purchase(purchaseParamsBuilder.build())
        }
    }

    private fun purchase(params: PurchaseParams) {
        scope.launch {
            try {
                val (storeTransaction, _) = Purchases.sharedInstance.awaitPurchase(params)
                toggleLoadingIndicator(false)
                handleSuccessfulPurchase(storeTransaction.orderId)
            } catch (exception: PurchasesTransactionException) {
                toggleLoadingIndicator(false)
                showUserError(
                    requireActivity(),
                    PurchasesError(underlyingErrorMessage = exception.underlyingErrorMessage, code = exception.code),
                )
            }
        }
    }

    private fun startObserverModePurchase(purchasingData: PurchasingData) {
        when (purchasingData) {
            is GooglePurchasingData.Subscription -> {
                ObserverModeBillingClient.purchase(
                    requireActivity(),
                    purchasingData.productDetails,
                    purchasingData.token,
                    false,
                )
            }

            is GooglePurchasingData.InAppProduct -> {
                ObserverModeBillingClient.purchase(
                    requireActivity(),
                    purchasingData.productDetails,
                    null,
                    false,
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
                Toast.LENGTH_SHORT,
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
}
