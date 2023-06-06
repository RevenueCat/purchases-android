package com.revenuecat.sample.ui.paywall

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.revenuecat.purchases.*
import com.revenuecat.purchases.models.StoreProduct
import com.revenuecat.purchases.models.SubscriptionOption
import com.revenuecat.sample.R
import com.revenuecat.sample.extensions.buildError

class PaywallFragment : Fragment() {
    private lateinit var root: View
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PaywallAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        root = inflater.inflate(R.layout.fragment_paywall, container, false)

        recyclerView = root.findViewById(R.id.paywall_list)

        recyclerView.setHasFixedSize(true)
        linearLayoutManager = LinearLayoutManager(context)
        linearLayoutManager.orientation = LinearLayoutManager.VERTICAL
        recyclerView.layoutManager = linearLayoutManager

        adapter = PaywallAdapter(null, didChoosePaywallItem = { item: PaywallItem ->
            when (item) {
                is PaywallItem.Product -> {
                    purchaseProduct(item.storeProduct)
                }
                is PaywallItem.Option -> {
                    purchaseOption(item.subscriptionOption)
                }
                is PaywallItem.Title -> {
                    // Do nothing
                }
            }
        })

        recyclerView.adapter = adapter

        /*
        Load offerings when the paywall is displayed
         */
        fetchOfferings()

        return root
    }

    private fun fetchOfferings() {
        Purchases.sharedInstance.getOfferingsWith { offerings: Offerings ->
            adapter.offering = offerings.current
            adapter.notifyDataSetChanged()
        }
    }

    private fun purchaseProduct(item: StoreProduct) {
        Purchases.sharedInstance.purchaseWith(
            PurchaseParams.Builder(requireActivity(), item).build(),
            onError = { error, userCancelled ->
                if (!userCancelled) {
                    buildError(context, error.message)
                }
            },
            onSuccess = { _, _ ->
                activity?.finish()
            }
        )
    }

    private fun purchaseOption(item: SubscriptionOption) {
        Purchases.sharedInstance.purchaseWith(
            PurchaseParams.Builder(requireActivity(), item).build(),
            onError = { error, userCancelled ->
                if (!userCancelled) {
                    buildError(context, error.message)
                }
            },
            onSuccess = { _, _ ->
                activity?.finish()
            }
        )
    }
}
