package com.revenuecat.purchasetester

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.Toast
import androidx.core.view.doOnPreDraw
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.FragmentNavigatorExtras
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.transition.MaterialElevationScale
import com.revenuecat.purchases.CustomerInfo
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Offerings
import com.revenuecat.purchases.PurchaseParams
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesError
import com.revenuecat.purchases.getAmazonLWAConsentStatusWith
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.interfaces.GetStoreProductsCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.RedeemWebPurchaseListener
import com.revenuecat.purchases.interfaces.SyncAttributesAndOfferingsCallback
import com.revenuecat.purchases.logOutWith
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases_sample.R
import com.revenuecat.purchases_sample.databinding.FragmentOverviewBinding
import com.revenuecat.purchases_sample.databinding.RowViewBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

@SuppressWarnings("TooManyFunctions")
class OverviewFragment : Fragment(), OfferingCardAdapter.OfferingCardAdapterListener, OverviewInteractionHandler {

    private lateinit var viewModel: OverviewViewModel
    private lateinit var binding: FragmentOverviewBinding
    private lateinit var dataStoreUtils: DataStoreUtils

    @Suppress("LongMethod")
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        dataStoreUtils = DataStoreUtils(requireActivity().applicationContext.configurationDataStore)

        binding = FragmentOverviewBinding.inflate(inflater)

        binding.customerInfoLogoutButton.setOnClickListener {
            if (Purchases.sharedInstance.isAnonymous) {
                navigateToLoginFragment()
            } else {
                Purchases.sharedInstance.logOutWith(
                    { error -> showUserError(requireActivity(), error) },
                    { navigateToLoginFragment() },
                )
            }
        }

        binding.logsButton.setOnClickListener { navigateToLogsFragment() }
        binding.proxyButton.setOnClickListener { navigateToProxyFragment() }
        binding.getProductsButton.setOnClickListener { showGetProductsDialog() }
        binding.purchaseProductIdButton.setOnClickListener { showPurchaseProductIdDialog() }
        binding.findByPlacementButton.setOnClickListener { showFindPlacementDialog() }

        viewModel = OverviewViewModel(this)

        binding.customerInfoCard.setOnClickListener { viewModel.onCardClicked() }
        binding.customerInfoCopyUserIdButton.setOnClickListener { viewModel.onCopyClicked() }
        binding.customerInfoManageButton.setOnClickListener { viewModel.onManageClicked() }
        binding.customerInfoRestorePurchasesButton.setOnClickListener { viewModel.onRestoreClicked() }
        binding.customerInfoSetAttribute.setOnClickListener { viewModel.onSetAttributeClicked() }
        binding.customerInfoSyncAttributes.setOnClickListener { viewModel.onSyncAttributesClicked() }
        binding.blockStoreClearButton.setOnClickListener { viewModel.onBlockStoreClearClicked(requireContext()) }
        binding.customerInfoFetchVcsButton.setOnClickListener { viewModel.onFetchVCsClicked() }
        binding.customerInfoInvalidateVcsCacheButton.setOnClickListener {
            viewModel.onInvalidateVirtualCurrenciesCache()
        }
        binding.customerInfoFetchVcCacheButton.setOnClickListener { viewModel.onFetchVCCache() }

        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        postponeEnterTransition()
        view.doOnPreDraw { startPostponedEnterTransition() }

        // This should be done in a ViewModel, but it's a test app ¯\_(ツ)_/¯
        (activity?.application as? MainApplication)?.lastCustomerInfoLiveData?.observe(viewLifecycleOwner) {
            viewModel.customerInfo.value = it
        }

        viewModel.retrieveCustomerInfo()

        setupObservers()

        Purchases.sharedInstance.getOfferingsWith(::showError, ::populateOfferings)

        lifecycleScope.launch {
            dataStoreUtils.getSdkConfig().onEach { sdkConfiguration ->
                if (sdkConfiguration.useAmazon) {
                    Purchases.sharedInstance.getAmazonLWAConsentStatusWith({
                        Log.i("PurchaseTester", "AmazonLWAConsentStatus Success: $it")
                    }, {
                        Log.e("PurchaseTester", "AmazonLWAConsentStatus Error: $it")
                    })
                }
            }.collect()
        }
    }

    override fun onResume() {
        super.onResume()

        val activity = requireActivity() as MainActivity
        val webPurchaseRedemption = activity.webPurchaseRedemption ?: return
        activity.clearWebPurchaseRedemption()
        Purchases.sharedInstance.redeemWebPurchase(webPurchaseRedemption) { result ->
            when (result) {
                is RedeemWebPurchaseListener.Result.Success -> {
                    showToast("Successfully redeemed web purchase. Updating customer info.")
                }
                is RedeemWebPurchaseListener.Result.Error -> {
                    showUserError(requireActivity(), result.error)
                }
                RedeemWebPurchaseListener.Result.InvalidToken -> {
                    showToast("Invalid web redemption token. Please check your link.")
                }
                RedeemWebPurchaseListener.Result.PurchaseBelongsToOtherUser -> {
                    showToast("Web purchase belongs to a different user. Ignoring.")
                }
                is RedeemWebPurchaseListener.Result.Expired -> {
                    showToast(
                        "Web purchase redemption token expired. " +
                            "An email with a new one was sent to ${result.obfuscatedEmail}.",
                    )
                }
            }
        }
    }

    private fun populateOfferings(offerings: Offerings) {
        if (offerings.all.isEmpty()) {
            binding.offeringHeader.text = "No Offerings"
            return
        }

        val currentOffering = offerings.current
        val allOfferings = offerings.all.values.toList().sortedBy {
            it.identifier != currentOffering?.identifier
        }

        val adapter = OfferingCardAdapter(
            allOfferings,
            currentOffering,
            this,
        )
        binding.overviewOfferingsRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.overviewOfferingsRecycler.adapter = adapter
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
        val directions = OverviewFragmentDirections.actionOverviewFragmentToOfferingFragment(offering.identifier)
        // uncomment line below and comment line above to test deprecated purchase methods
//        val directions = OverviewFragmentDirections.
//        actionOverviewFragmentToDeprecatedOfferingFragment(offering.identifier)
        findNavController().navigate(directions, extras)
    }

    private fun showPurchaseProductIdDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Enter the Product ID you want to purchase:")
        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)
        builder.setPositiveButton("Purchase") { dialog, which ->
            var productId = input.text.toString()
            val segments = productId.split(":")
            Purchases.sharedInstance.getProducts(
                listOf(segments.first()),
                object : GetStoreProductsCallback {
                    override fun onReceived(storeProducts: List<StoreProduct>) {
                        if (storeProducts.isEmpty()) {
                            showToast("A product with ID $productId does not exist")
                            return
                        }
                        var product = storeProducts.first()
                        if (segments.count() == 2) {
                            storeProducts.firstOrNull { (it as? GoogleStoreProduct)?.basePlanId == segments.last() }
                                .let { storeProduct ->
                                    if (storeProduct != null) {
                                        product = storeProduct
                                    } else {
                                        showToast("A product with ID $productId does not exist")
                                        return
                                    }
                                }
                        }
                        Purchases.sharedInstance.purchase(
                            PurchaseParams.Builder(requireActivity(), product).build(),
                            object : PurchaseCallback {
                                override fun onCompleted(
                                    storeTransaction: StoreTransaction,
                                    customerInfo: CustomerInfo,
                                ) {
                                    showToast("Successful purchase, order id: ${storeTransaction.orderId}")
                                }

                                override fun onError(error: PurchasesError, userCancelled: Boolean) {
                                    showError(error)
                                }
                            },
                        )
                    }

                    override fun onError(error: PurchasesError) {
                        showError(error)
                    }
                },
            )
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.cancel()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun showGetProductsDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Enter Product ID(s) to fetch (comma-separated):")
        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        input.hint = "e.g., connect or connect:connect-monthly"
        builder.setView(input)
        builder.setPositiveButton("Get Products") { dialog, which ->
            val productIdsString = input.text.toString()
            if (productIdsString.isBlank()) {
                showToast("Please enter at least one product ID")
                return@setPositiveButton
            }
            val productIds = productIdsString.split(",").map { it.trim() }.filter { it.isNotEmpty() }

            Purchases.sharedInstance.getProducts(
                productIds,
                object : GetStoreProductsCallback {
                    override fun onReceived(storeProducts: List<StoreProduct>) {
                        if (storeProducts.isEmpty()) {
                            showToast("No products found for IDs: ${productIds.joinToString()}")
                            return
                        }
                        val productInfo = storeProducts.joinToString("\n\n") { product ->
                            val basePlanInfo = (product as? GoogleStoreProduct)?.basePlanId?.let {
                                "\nBase Plan ID: $it"
                            } ?: ""
                            "Product ID: ${product.id}\n" +
                                "Name: ${product.name}\n" +
                                "Price: ${product.price.formatted}\n" +
                                "Type: ${product.type}" +
                                basePlanInfo
                        }
                        MaterialAlertDialogBuilder(requireContext())
                            .setTitle("Found ${storeProducts.size} product(s)")
                            .setMessage(productInfo)
                            .setPositiveButton("OK", null)
                            .show()
                    }

                    override fun onError(error: PurchasesError) {
                        showError(error)
                    }
                },
            )
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.cancel()
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun showFindPlacementDialog() {
        val builder = MaterialAlertDialogBuilder(requireContext())
        builder.setTitle("Enter the Placement ID you want to get:")
        val input = EditText(context)
        input.inputType = InputType.TYPE_CLASS_TEXT
        builder.setView(input)
        builder.setPositiveButton("Find") { dialog, which ->
            var placementId = input.text.toString()

            Purchases.sharedInstance.getOfferingsWith {
                it.getCurrentOfferingForPlacement(placementId)?.let { offering ->
                    exitTransition = MaterialElevationScale(false).apply {
                        duration = requireContext().resources.getInteger(R.integer.transition_duration).toLong()
                    }
                    reenterTransition = MaterialElevationScale(true).apply {
                        duration = requireContext().resources.getInteger(R.integer.transition_duration).toLong()
                    }

                    val directions = OverviewFragmentDirections.actionOverviewFragmentToOfferingFragment(
                        offering.identifier,
                    )
                    findNavController().navigate(directions)
                } ?: run {
                    print("no offering")
                }
            }
        }
        builder.setNegativeButton("Cancel") { dialog, which ->
            dialog.cancel()
        }
        val dialog = builder.create()
        dialog.show()
    }

    override fun displayError(error: PurchasesError) {
        showError(error)
    }

    override fun showToast(message: String) {
        Toast.makeText(
            requireContext(),
            message,
            Toast.LENGTH_LONG,
        ).show()
    }

    override fun toggleCard() {
        with(binding.customerInfoDetailsContainer) {
            visibility = if (visibility == View.GONE) View.VISIBLE else View.GONE
        }
    }

    override fun copyToClipboard(text: String) {
        copyToClipboard(requireContext(), "RevenueCat userId", text)
    }

    override fun launchURL(url: Uri) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = url
        startActivity(intent)
    }

    override fun setAttribute() {
        val dialogView = layoutInflater.inflate(R.layout.dialog_set_attribute, null)
        MaterialAlertDialogBuilder(requireContext())
            .setTitle("Set Attribute")
            .setView(dialogView)
            .setPositiveButton("Save") { dialog, which ->
                val textField1 = dialogView.findViewById<TextInputEditText>(R.id.textField1).text.toString()
                val textField2 = dialogView.findViewById<TextInputEditText>(R.id.textField2).text.toString()

                Purchases.sharedInstance.setAttributes(
                    mapOf(textField1 to textField2),
                )
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    override fun syncAttributes() {
        Purchases.sharedInstance.syncAttributesAndOfferingsIfNeeded(object : SyncAttributesAndOfferingsCallback {
            override fun onSuccess(offerings: Offerings) {
                showToast("Synced attributes and offerings")
                populateOfferings(offerings)
            }

            override fun onError(error: PurchasesError) {
                showUserError(requireActivity(), error)
            }
        })
    }

    private fun navigateToLoginFragment() {
        val directions = OverviewFragmentDirections.actionOverviewFragmentToLoginFragment()
        findNavController().navigate(directions)
    }

    private fun navigateToLogsFragment() {
        val directions = OverviewFragmentDirections.actionOverviewFragmentToLogsFragment()
        findNavController().navigate(directions)
    }

    private fun navigateToProxyFragment() {
        val directions = OverviewFragmentDirections.actionOverviewFragmentToProxySettingsBottomSheetFragment()
        findNavController().navigate(directions)
    }

    private fun setupObservers() {
        viewModel.customerInfo.observe(viewLifecycleOwner) { customerInfo ->
            binding.customerInfoRequestDate.text = customerInfo?.requestDate?.let { " as of $it" } ?: ""
            binding.customerInfoRequestDate.visibility =
                if (customerInfo?.requestDate != null) View.VISIBLE else View.GONE

            binding.customerInfoAppUserId.updateRowView("Original App User Id: ", customerInfo?.originalAppUserId)
            binding.customerInfoManageButton.visibility =
                if (customerInfo?.managementURL != null) View.VISIBLE else View.GONE
        }

        viewModel.verificationResult.observe(viewLifecycleOwner) { verificationResult ->
            binding.customerInfoVerificationResult.updateRowView(
                "Current verification result: ",
                verificationResult?.name,
            )
        }

        viewModel.activeEntitlements.observe(viewLifecycleOwner) { activeEntitlements ->
            binding.customerInfoActiveEntitlements.updateRowView("Active Entitlements: ", activeEntitlements)
        }

        viewModel.allEntitlements.observe(viewLifecycleOwner) { allEntitlements ->
            binding.customerInfoAllEntitlements.updateRowView("All Entitlements: ", allEntitlements)
        }

        viewModel.formattedVirtualCurrencies.observe(viewLifecycleOwner) { formattedVirtualCurrencies ->
            binding.customerInfoVirtualCurrencies.updateRowView("Virtual Currencies: ", formattedVirtualCurrencies)
        }

        viewModel.customerInfoJson.observe(viewLifecycleOwner) { customerInfoJson ->
            binding.customerInfoJsonObject.updateRowView("JSON Object", customerInfoJson)
        }

        viewModel.isRestoring.observe(viewLifecycleOwner) { isRestoring ->
            binding.customerInfoRestorePurchasesButton.isEnabled = !isRestoring
            binding.customerInfoRestoreProgress.visibility = if (isRestoring) View.VISIBLE else View.GONE
        }
    }

    private fun RowViewBinding.updateRowView(header: String, detail: String?) {
        headerView.text = header
        value.text = if (detail.isNullOrEmpty()) "None" else detail
    }
}
