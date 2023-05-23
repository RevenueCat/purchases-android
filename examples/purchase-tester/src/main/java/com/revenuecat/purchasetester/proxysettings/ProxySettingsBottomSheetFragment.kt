package com.revenuecat.purchasetester.proxysettings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.revenuecat.purchases_sample.R
import com.revenuecat.purchases_sample.databinding.FragmentProxySettingsBottomSheetBinding

class ProxySettingsBottomSheetFragment : BottomSheetDialogFragment() {

    private val viewModel: ProxySettingsBottomSheetViewModel by viewModels()
    private lateinit var binding: FragmentProxySettingsBottomSheetBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentProxySettingsBottomSheetBinding.inflate(inflater)
        binding.lifecycleOwner = this

        viewModel.state.observe(viewLifecycleOwner) { state ->
            when (state) {
                is ProxySettingsState.Loading -> {
                    binding.proxySettingsRadioGroup.visibility = View.GONE
                    binding.proxySettingsErrorTextView.visibility = View.GONE
                    binding.proxySettingsLoadingProgressBar.visibility = View.VISIBLE
                }
                is ProxySettingsState.Error -> {
                    binding.proxySettingsRadioGroup.visibility = View.GONE
                    binding.proxySettingsErrorTextView.visibility = View.VISIBLE
                    binding.proxySettingsLoadingProgressBar.visibility = View.GONE
                    binding.proxySettingsErrorTextView.text = state.message
                }
                is ProxySettingsState.CurrentMode -> {
                    binding.proxySettingsRadioGroup.visibility = View.VISIBLE
                    binding.proxySettingsErrorTextView.visibility = View.GONE
                    binding.proxySettingsLoadingProgressBar.visibility = View.GONE
                    checkRadioButton(state.mode)
                }
            }
        }

        binding.proxySettingDisabledRadioId.setOnClickListener {
            viewModel.changeMode(ProxyMode.OFF)
        }
        binding.proxySettingOverrideEntitlementRadioId.setOnClickListener {
            viewModel.changeMode(ProxyMode.ENTITLEMENT_OVERRIDE)
        }
        binding.proxySettingServerDownRadioId.setOnClickListener {
            viewModel.changeMode(ProxyMode.SERVER_DOWN)
        }

        return binding.root
    }

    private fun checkRadioButton(mode: ProxyMode) {
        binding.proxySettingsRadioGroup.check(
            when (mode) {
                ProxyMode.OFF -> R.id.proxy_setting_disabled_radio_id
                ProxyMode.ENTITLEMENT_OVERRIDE -> R.id.proxy_setting_override_entitlement_radio_id
                ProxyMode.SERVER_DOWN -> R.id.proxy_setting_server_down_radio_id
            }
        )
    }
}
