package com.revenuecat.purchasetester.ui.screens.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.revenuecat.purchasetester.databinding.FragmentLoginBinding
import com.revenuecat.purchasetester.ui.theme.PurchaseTesterAndroidTheme

class LoginFragment : Fragment() {

    lateinit var binding: FragmentLoginBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return ComposeView(requireContext()).apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                PurchaseTesterAndroidTheme {
                    PurchaseLoginScreen(
                        onNavigateToLogs = ::navigateToLogsFragment,
                        onNavigateToProxy = ::navigateToProxyFragment,
                        onNavigateToOverview = ::advanceToOverviewFragment,
                        onNavigateToConfigure = ::navigateToConfigureFragment
                    )
                }
            }
        }
    }

    private fun advanceToOverviewFragment() {
        val directions = LoginFragmentDirections.actionLoginFragmentToOverviewFragment()
        findNavController().navigate(directions)
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
