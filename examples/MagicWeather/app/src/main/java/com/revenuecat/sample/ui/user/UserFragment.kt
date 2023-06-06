package com.revenuecat.sample.ui.user

import android.app.AlertDialog
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.revenuecat.purchases.*
import com.revenuecat.sample.R
import com.revenuecat.sample.data.Constants
import com.revenuecat.sample.extensions.buildError

class UserFragment : Fragment() {

    private lateinit var userViewModel: UserViewModel
    private lateinit var root: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        userViewModel = UserViewModel.shared

        root = inflater.inflate(R.layout.fragment_user, container, false)

        val restoreButton: Button = root.findViewById(R.id.restoreTransactionsButton)
        restoreButton.setOnClickListener {
            restorePurchases()
        }

        val identityButton: Button = root.findViewById(R.id.identityButton)
        identityButton.setOnClickListener {
            identityFlow()
        }

        /*
        Observe changes to weather data
         */
        userViewModel.customerInfo.observe(
            viewLifecycleOwner,
            Observer {
                it?.let {
                    updateUserDetails(it)
                }
            }
        )

        return root
    }

    private fun updateUserDetails(info: CustomerInfo) {
        val appUserIdLabel = root.findViewById<TextView>(R.id.text_user_id)
        appUserIdLabel.text = Purchases.sharedInstance.appUserID

        val userStatusLabel = root.findViewById<TextView>(R.id.text_user_status)

        if (info.entitlements[Constants.ENTITLEMENT_ID]?.isActive == true) {
            userStatusLabel.text = "Active"
            userStatusLabel.setTextColor(Color.GREEN)
        } else {
            userStatusLabel.text = "Not Active"
            userStatusLabel.setTextColor(Color.RED)
        }

        val identityButton: Button = root.findViewById(R.id.identityButton)

        if (Purchases.sharedInstance.isAnonymous) {
            identityButton.text = "Log In"
        } else {
            identityButton.text = "Log Out"
        }
    }

    /*
    How to restore purchases using the Purchases SDK. Read more about restoring purchases here: https://docs.revenuecat.com/docs/making-purchases#restoring-purchases
     */
    private fun restorePurchases() {
        Purchases.sharedInstance.restorePurchasesWith(onError = {
            buildError(context, it.message)
        }, onSuccess = {})
    }

    /*
    How to login and identify your users with the Purchases SDK.

    These functions mimic displaying a login dialog, identifying the user, then logging out later.

    Public-facing usernames aren't optimal for user ID's - you should use something non-guessable,
    like a non-public database ID.

    Read more about Identifying Users here: https://docs.revenuecat.com/docs/user-ids
     */
    private fun identityFlow() {
        if (Purchases.sharedInstance.isAnonymous) {
            val view = layoutInflater.inflate(R.layout.dialog_login, null)
            val input = view.findViewById<EditText>(R.id.username)

            val alert = AlertDialog.Builder(requireContext())
                .setTitle("Log In")
                .setView(view)
                .setPositiveButton("Log In") { _, _ ->
                    /*
                    Call `identify` with the Purchases SDK with the unique user ID
                     */
                    Purchases.sharedInstance.logInWith(
                        input.text.toString(),
                        onError = { error ->
                            buildError(context, error.message)
                        },
                        onSuccess = { info, _ ->
                            updateUserDetails(info)
                        }
                    )
                }
                .setNegativeButton("Cancel") { _, _ -> }
                .create()

            alert.show()
        } else {
            Purchases.sharedInstance.logOut()
        }
    }
}
