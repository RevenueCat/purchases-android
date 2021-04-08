package com.revenuecat.sample

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.revenuecat.purchases.PurchaserInfo
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.identifyWith
import com.revenuecat.purchases.resetWith
import com.revenuecat.sample.databinding.FragmentLoginBinding

class LoginFragment : Fragment() {

    lateinit var binding: FragmentLoginBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentLoginBinding.inflate(inflater)

        binding.loginButton.setOnClickListener {
            binding.loginUsernameEditText.text?.toString()?.let { userId ->
                Purchases.sharedInstance
                    .identifyWith(
                        userId,
                        { error -> showUserError(requireActivity(), error) },
                        ::advanceToOverviewFragment
                    )
            }
        }

        binding.anonymousUserButton.setOnClickListener {
            Purchases.sharedInstance.resetWith(
                { error -> showUserError(requireActivity(), error) },
                ::advanceToOverviewFragment
            )
        }

        return binding.root
    }

    private fun advanceToOverviewFragment(purchaserInfo: PurchaserInfo) {
        val directions = LoginFragmentDirections.actionLoginFragmentToOverviewFragment()
        findNavController().navigate(directions)
    }
}
