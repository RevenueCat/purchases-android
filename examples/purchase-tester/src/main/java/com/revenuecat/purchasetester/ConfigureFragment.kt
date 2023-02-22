package com.revenuecat.purchasetester

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.get
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.revenuecat.purchases.LogLevel
import com.revenuecat.purchases.Purchases
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
        savedInstanceState: Bundle?
    ): View {
        dataStoreUtils = DataStoreUtils(requireActivity().applicationContext.configurationDataStore)
        binding = FragmentConfigureBinding.inflate(inflater)

        setupSupportedStoresRadioButtons()

        lifecycleScope.launch {
            dataStoreUtils.getSdkConfig().onEach { sdkConfiguration ->
                binding.apiKeyInput.setText(sdkConfiguration.apiKey)
                binding.proxyUrlInput.setText(sdkConfiguration.proxyUrl)
                val storeToCheckId =
                    if (sdkConfiguration.useAmazon) R.id.amazon_store_radio_id
                    else R.id.google_store_radio_id
                binding.storeRadioGroup.check(storeToCheckId)
            }.collect()
        }

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

        return binding.root
    }

    private suspend fun configureSDK() {
        val apiKey = binding.apiKeyInput.text.toString()
        val proxyUrl = binding.proxyUrlInput.text?.toString() ?: ""
        val useAmazonStore = binding.storeRadioGroup.checkedRadioButtonId == R.id.amazon_store_radio_id

        val application = (requireActivity().application as MainApplication)

        if (proxyUrl.isNotEmpty()) Purchases.proxyURL = URL(proxyUrl)

        Purchases.logLevel = LogLevel.VERBOSE

        val configurationBuilder =
            if (useAmazonStore) AmazonConfiguration.Builder(application, apiKey)
            else PurchasesConfiguration.Builder(application, apiKey)

        val configuration = configurationBuilder
            .diagnosticsEnabled(true)
            .build()
        Purchases.configure(configuration)

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
