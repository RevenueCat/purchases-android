package com.revenuecat.purchasetester

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.revenuecat.purchase_tester.databinding.FragmentLoginBinding
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.logInWith
import com.revenuecat.purchases.logOutWith

class LoginFragment : Fragment() {

    lateinit var binding: FragmentLoginBinding

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentLoginBinding.inflate(inflater)

        binding.loginButton.setOnClickListener {
            binding.loginUsernameEditText.text?.toString()?.let { userId ->
                Purchases.sharedInstance
                    .logInWith(
                        userId,
                        { error -> showUserError(requireActivity(), error) },
                        { _, _ -> advanceToOverviewFragment() }
                    )
            }
        }

        binding.anonymousUserButton.setOnClickListener {
            if (Purchases.sharedInstance.isAnonymous) {
                advanceToOverviewFragment()
            } else {
                Purchases.sharedInstance.logOutWith(
                    { error -> showUserError(requireActivity(), error) },
                    { advanceToOverviewFragment() }
                )
            }
        }

        return binding.root
    }

    private fun advanceToOverviewFragment() {
        val directions = LoginFragmentDirections.actionLoginFragmentToOverviewFragment()
        findNavController().navigate(directions)
    }
}
