package com.revenuecat.sample

import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.navArgs
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.transition.MaterialContainerTransform
import com.revenuecat.purchases.Offering
import com.revenuecat.purchases.Package
import com.revenuecat.sample.databinding.FragmentOfferingBinding

class OfferingFragment : Fragment(), PackageCardAdapter.PackageCardAdapterListener {

    lateinit var binding: FragmentOfferingBinding

    private val args: OfferingFragmentArgs by navArgs()
    private val offering: Offering by lazy(LazyThreadSafetyMode.NONE) { args.offering }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        sharedElementEnterTransition = MaterialContainerTransform().apply {
            drawingViewId = R.id.nav_host_fragment
            duration = requireContext().resources.getInteger(R.integer.transition_duration).toLong()
            scrimColor = Color.TRANSPARENT
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentOfferingBinding.inflate(inflater)
        binding.offering = offering

        binding.offeringsPackagesRecycler.layoutManager = LinearLayoutManager(requireContext())
        binding.offeringsPackagesRecycler.adapter = PackageCardAdapter(offering.availablePackages, this)
        return binding.root
    }

    override fun onBuyClicked(cardView: View, currentPackage: Package) {
        // TODO handle purchasing package
    }
}
