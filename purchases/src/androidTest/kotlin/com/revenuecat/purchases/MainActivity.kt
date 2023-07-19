package com.revenuecat.purchases

import android.os.Bundle
import android.widget.LinearLayout
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(LinearLayout(this))
    }
}
