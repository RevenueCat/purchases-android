package com.revenuecat.sample.ui.weather

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import com.revenuecat.purchases.Purchases
import com.revenuecat.purchases.getCustomerInfoWith
import com.revenuecat.sample.R
import com.revenuecat.sample.data.Constants
import com.revenuecat.sample.data.SampleWeatherData
import com.revenuecat.sample.extensions.buildError
import com.revenuecat.sample.ui.paywall.PaywallActivity

class WeatherFragment : Fragment() {

    private lateinit var weatherViewModel: WeatherViewModel
    private lateinit var root: View

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        weatherViewModel = WeatherViewModel.shared

        root = inflater.inflate(R.layout.fragment_weather, container, false)

        /*
        Setup our magic button on click listener
         */
        val changeWeatherButton: Button = root.findViewById(R.id.changeWeatherButton)
        changeWeatherButton.setOnClickListener {
            performMagic()
        }

        /*
        Observe changes to weather data
         */
        weatherViewModel.currentData.observe(
            viewLifecycleOwner,
            Observer {
                setWeatherData(it)
            },
        )

        /*
        Set default weather data
         */
        weatherViewModel.currentData.value?.let {
            setWeatherData(it)
        }

        return root
    }

    private fun setWeatherData(data: SampleWeatherData) {
        /*
        Update the UI with our latest weather data
         */
        val textView: TextView = root.findViewById(R.id.text_weather)
        textView.text = "${data.emoji}\n${data.temperature}°${data.unit.unit.capitalize()}"

        root.setBackgroundResource(data.weatherColor)
    }

    private fun performMagic() {
        /*
        We should check if we can magically change the weather (subscription active) and if not, display the paywall.
         */
        Purchases.sharedInstance.getCustomerInfoWith({
            buildError(context, it.message)
        }, {
            if (it.entitlements[Constants.ENTITLEMENT_ID]?.isActive == true) {
                weatherViewModel.currentData.value =
                    SampleWeatherData.generateSampleData(SampleWeatherData.Environment.EARTH, null)
            } else {
                val intent = Intent(context, PaywallActivity::class.java)
                startActivity(intent)
            }
        })
    }
}
