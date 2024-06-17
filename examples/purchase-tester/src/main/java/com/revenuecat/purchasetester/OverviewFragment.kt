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
import com.revenuecat.purchases.getAmazonLWAConsentStatus
import com.revenuecat.purchases.getOfferingsWith
import com.revenuecat.purchases.interfaces.GetStoreProductsCallback
import com.revenuecat.purchases.interfaces.PurchaseCallback
import com.revenuecat.purchases.interfaces.SyncAttributesAndOfferingsCallback
import com.revenuecat.purchases.logOutWith
import com.revenuecat.purchases.models.GoogleStoreProduct
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.StoreTransaction
import com.revenuecat.purchases_sample.R
import com.revenuecat.purchases_sample.databinding.FragmentOverviewBinding

@SuppressWarnings("TooManyFunctions")
class OverviewFragment : Fragment(), OfferingCardAdapter.OfferingCardAdapterListener, OverviewInteractionHandler {

    private lateinit var viewModel: OverviewViewModel
    private lateinit var binding: FragmentOverviewBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
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

        binding.logsButton.setOnClickListener {
            navigateToLogsFragment()
        }

        binding.proxyButton.setOnClickListener {
            navigateToProxyFragment()
        }

        binding.purchaseProductIdButton.setOnClickListener {
            showPurchaseProductIdDialog()
        }

        binding.findByPlacementButton.setOnClickListener {
            showFindPlacementDialog()
        }

        viewModel = OverviewViewModel(this)
        binding.lifecycleOwner = this
        binding.viewModel = viewModel

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

        Purchases.sharedInstance.getOfferingsWith(::showError, ::populateOfferings)

        Purchases.sharedInstance.getAmazonLWAConsentStatus({
            Log.i("PurchaseTester", "AmazonLWAConsentStatus Success: $it")
        }, {
            Log.e("PurchaseTester", "AmazonLWAConsentStatus Error: $it")
        })
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
}
