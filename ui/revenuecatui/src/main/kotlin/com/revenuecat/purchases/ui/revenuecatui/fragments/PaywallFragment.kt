package com.revenuecat.purchases.ui.revenuecatui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.fragment.app.Fragment
import com.revenuecat.purchases.ui.revenuecatui.PaywallView
import com.revenuecat.purchases.ui.revenuecatui.PaywallViewOptions
import com.revenuecat.purchases.ui.revenuecatui.databinding.FragmentPaywallBinding

private const val OFFERING_ID = "offering_id"

class PaywallFragment : Fragment() {

    private var offeringId: String? = null
    private val options: PaywallViewOptions by lazy {
        PaywallViewOptions.Builder()
            .setOfferingId(offeringId)
            .build()
    }

    private var _binding: FragmentPaywallBinding? = null

    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            offeringId = it.getString(OFFERING_ID)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentPaywallBinding.inflate(inflater, container, false)
        binding.composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent {
                MaterialTheme {
                    PaywallView(options)
                }
            }
        }
        return binding.root
    }

    companion object {
        const val TAG = "PaywallFragment"

        @JvmStatic
        fun newInstance(offeringId: String? = null) =
            PaywallFragment().apply {
                arguments = Bundle().apply {
                    putString(OFFERING_ID, offeringId)
                }
            }
    }
}
