package com.revenuecat.purchasetester.ui.screens.configure

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.revenuecat.purchasetester.ui.screens.PurchaseTesterApp
import com.revenuecat.purchasetester.ui.theme.PurchaseTesterAndroidTheme

class ConfigureFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PurchaseTesterAndroidTheme {
                    PurchaseTesterApp(
                        onNavigateToLogin = { navigateToLoginFragment() },
                        onNavigateToLogs = { navigateToLogsFragment() },
                        onNavigateToProxy = { navigateToProxyFragment() }
                    )
                }
            }
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
}