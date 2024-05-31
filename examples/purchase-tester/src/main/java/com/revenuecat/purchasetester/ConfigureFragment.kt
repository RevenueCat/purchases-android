package com.revenuecat.purchasetester

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.core.view.children
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.revenuecat.purchases.EntitlementVerificationMode
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.PurchasesAreCompletedBy
import com.revenuecat.purchases.PurchasesConfiguration
import com.revenuecat.purchases.amazon.AmazonConfiguration
import com.revenuecat.purchases_sample.BuildConfig
import com.revenuecat.purchases_sample.R
import com.revenuecat.purchases_sample.databinding.FragmentConfigureBinding
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.net.MalformedURLException
import java.net.URL

class ConfigureFragment : Fragment() {

    lateinit var binding: FragmentConfigureBinding

    private lateinit var dataStoreUtils: DataStoreUtils

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        dataStoreUtils = DataStoreUtils(requireActivity().applicationContext.configurationDataStore)
        binding = FragmentConfigureBinding.inflate(inflater)

        binding.verificationOptionsInput.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            EntitlementVerificationMode.values(),
        )
        setupSupportedStoresRadioButtons()

        lifecycleScope.launch {
            dataStoreUtils.getSdkConfig().onEach { sdkConfiguration ->
                binding.apiKeyInput.setText(sdkConfiguration.apiKey)
                binding.proxyUrlInput.setText(sdkConfiguration.proxyUrl)
                val storeToCheckId =
                    if (sdkConfiguration.useAmazon) {
                        R.id.amazon_store_radio_id
                    } else R.id.google_store_radio_id
                binding.storeRadioGroup.check(storeToCheckId)
            }.collect()
        }

        @Suppress("EmptyFunctionBlock")
        binding.proxyUrlInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                try {
                    if (!s.isNullOrEmpty()) {
                        val url = URL(s.toString())
                        Purchases.proxyURL = url
                    } else {
                        Purchases.proxyURL = null
                    }
                } catch (e: MalformedURLException) {
                    Purchases.proxyURL = null
                }
            }

            override fun afterTextChanged(s: Editable?) {}
        })

        binding.continueButton.setOnClickListener {
            if (!validateInputs()) return@setOnClickListener
            binding.continueButton.isEnabled = false

            lifecycleScope.launch {
                configureSDK()
                navigateToLoginFragment()
            }
        }

        binding.logsButton.setOnClickListener {
            navigateToLogsFragment()
        }

        binding.proxyButton.setOnClickListener {
            navigateToProxyFragment()
        }

        binding.amazonStoreRadioId.setOnCheckedChangeListener { _, isChecked ->
            // Disable PurchasesAreCompletedBy options if Amazon
            with(binding.purchaseCompletionRadioGroup) {
                isEnabled = !isChecked
                children.forEach { it.isEnabled = !isChecked }
            }

            // Check PurchasesAreCompletedBy.REVENUECAT if Amazon is checked
            if (isChecked) {
                binding.completedByRevenuecatRadioId.isChecked = true
            }
        }

        return binding.root
    }

    private suspend fun configureSDK() {
        val apiKey = binding.apiKeyInput.text.toString()
        val proxyUrl = binding.proxyUrlInput.text?.toString() ?: ""
        val verificationModeIndex = binding.verificationOptionsInput.selectedItemPosition

        val entitlementVerificationMode = EntitlementVerificationMode.values()[verificationModeIndex]
        val useAmazonStore = binding.storeRadioGroup.checkedRadioButtonId == R.id.amazon_store_radio_id
        val purchasesAreCompletedBy = when (binding.purchaseCompletionRadioGroup.checkedRadioButtonId) {
            R.id.completed_by_revenuecat_radio_id -> PurchasesAreCompletedBy.REVENUECAT
            R.id.completed_by_my_app_radio_id -> PurchasesAreCompletedBy.MY_APP
            else -> error(
                "Unexpected checkedRadioButtonId: ${binding.purchaseCompletionRadioGroup.checkedRadioButtonId}",
            )
        }

        val application = (requireActivity().application as MainApplication)

        if (proxyUrl.isNotEmpty()) Purchases.proxyURL = URL(proxyUrl)

        Purchases.logLevel = LogLevel.VERBOSE

        val configurationBuilder =
            if (useAmazonStore) {
                AmazonConfiguration.Builder(application, apiKey)
            } else PurchasesConfiguration.Builder(application, apiKey)

        val configuration = configurationBuilder
            .diagnosticsEnabled(true)
            .entitlementVerificationMode(entitlementVerificationMode)
            .purchasesAreCompletedBy(purchasesAreCompletedBy)
            .build()
        Purchases.configure(configuration)

        if (purchasesAreCompletedBy == PurchasesAreCompletedBy.MY_APP) {
            ObserverModeBillingClient.start(application, application.logHandler)
        }

        // set attributes to store additional, structured information for a user in RevenueCat.
        // More info: https://docs.revenuecat.com/docs/user-attributes
        Purchases.sharedInstance.setAttributes(mapOf("favorite_cat" to "garfield"))

        Purchases.sharedInstance.updatedCustomerInfoListener = application

        dataStoreUtils.saveSdkConfig(SdkConfiguration(apiKey, proxyUrl, useAmazonStore))
    }

    private fun setupSupportedStoresRadioButtons() {
        val supportedStores = BuildConfig.SUPPORTED_STORES.split(",")
        if (!supportedStores.contains("google")) {
            binding.googleStoreRadioId.isEnabled = false
            binding.googleUnavailableTextView.visibility = View.VISIBLE
        }
        if (!supportedStores.contains("amazon")) {
            binding.amazonStoreRadioId.isEnabled = false
            binding.amazonUnavailableTextView.visibility = View.VISIBLE
        }
    }

    private fun navigateToLoginFragment() {
        val directions = ConfigureFragmentDirections.actionConfigureFragmentToLoginFragment()
        findNavController().navigate(directions)
    }

    private fun navigateToLogsFragment() {
        val directions = ConfigureFragmentDirections.actionConfigureFragmentToLogsFragment()
        findNavController().navigate(directions)
    }

    private fun navigateToProxyFragment() {
        val directions = ConfigureFragmentDirections.actionConfigureFragmentToProxySettingsBottomSheetFragment()
        findNavController().navigate(directions)
    }

    private fun validateInputs(): Boolean {
        return validateApiKey() && validateProxyURL()
    }

    private fun validateApiKey(): Boolean {
        val errorMessages = mutableListOf<String>()
        if (binding.apiKeyInput.text?.isNotEmpty() != true) errorMessages.add("API Key is empty")
        if (errorMessages.isNotEmpty()) showError("API Key errors: $errorMessages")
        return errorMessages.isEmpty()
    }

    private fun validateProxyURL(): Boolean {
        val proxyURLString = binding.proxyUrlInput.text?.toString()
        if (proxyURLString?.isEmpty() != false) return true
        val errorMessages = mutableListOf<String>()
        try {
            URL(proxyURLString)
        } catch (e: MalformedURLException) { errorMessages.add("Invalid proxy URL. Could not convert to URL.") }
        if (errorMessages.isNotEmpty()) showError("Proxy URL errors: $errorMessages")
        return errorMessages.isEmpty()
    }

    private fun showError(msg: String) {
        context?.let {
            MaterialAlertDialogBuilder(it)
                .setMessage(msg)
                .setPositiveButton("OK") { dialog, _ -> dialog.dismiss() }
                .show()
        }
    }
}
