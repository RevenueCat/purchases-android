package com.revenuecat.purchasetester

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.logInWith
import com.revenuecat.purchases.logOutWith
import com.revenuecat.purchases_sample.databinding.FragmentLoginBinding
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    lateinit var binding: FragmentLoginBinding
    private lateinit var dataStoreUtils: DataStoreUtils

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        dataStoreUtils = DataStoreUtils(requireActivity().applicationContext.configurationDataStore)
        binding = FragmentLoginBinding.inflate(inflater)

        lifecycleScope.launch {
            val sdkConfiguration = dataStoreUtils.getSdkConfig().first()
            binding.loginUsernameEditText.setText(sdkConfiguration.appUserId.orEmpty())
        }

        binding.loginButton.setOnClickListener {
            binding.loginUsernameEditText.text?.toString()?.let { userId ->
                Purchases.sharedInstance
                    .logInWith(
                        userId,
                        { error -> showUserError(requireActivity(), error) },
                        { _, _ ->
                            saveAppUserId(userId)
                            advanceToOverviewFragment()
                        },
                    )
            }
        }

        binding.anonymousUserButton.setOnClickListener {
            if (Purchases.sharedInstance.isAnonymous) {
                saveAppUserId(null)
                advanceToOverviewFragment()
            } else {
                Purchases.sharedInstance.logOutWith(
                    { error -> showUserError(requireActivity(), error) },
                    {
                        saveAppUserId(null)
                        advanceToOverviewFragment()
                    },
                )
            }
        }

        binding.resetSdkButton.setOnClickListener {
            resetSdk()
            navigateToConfigureFragment()
        }

        binding.logsButton.setOnClickListener {
            navigateToLogsFragment()
        }

        binding.proxyButton.setOnClickListener {
            navigateToProxyFragment()
        }

        return binding.root
    }

    private fun resetSdk() {
        Purchases.sharedInstance.close()
    }

    private fun advanceToOverviewFragment() {
        val directions = LoginFragmentDirections.actionLoginFragmentToOverviewFragment()
        findNavController().navigate(directions)
    }

    private fun saveAppUserId(appUserId: String?) {
        requireActivity().lifecycleScope.launch {
            val existingConfiguration = dataStoreUtils.getSdkConfig().first()
            dataStoreUtils.saveSdkConfig(existingConfiguration.copy(appUserId = appUserId))
        }
    }

    private fun navigateToConfigureFragment() {
        val directions = LoginFragmentDirections.actionLoginFragmentToConfigureFragment()
        findNavController().navigate(directions)
    }

    private fun navigateToLogsFragment() {
        val directions = LoginFragmentDirections.actionLoginFragmentToLogsFragment()
        findNavController().navigate(directions)
    }

    private fun navigateToProxyFragment() {
        val directions = LoginFragmentDirections.actionLoginFragmentToProxySettingsBottomSheetFragment()
        findNavController().navigate(directions)
    }
}
