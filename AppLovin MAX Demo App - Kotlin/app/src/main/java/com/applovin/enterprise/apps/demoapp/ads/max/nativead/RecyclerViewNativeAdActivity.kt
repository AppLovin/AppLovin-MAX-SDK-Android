package com.applovin.enterprise.apps.demoapp.ads.max.nativead

import android.app.Activity
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.mediation.MaxAd
import com.applovin.mediation.nativeAds.adPlacer.MaxAdPlacer
import com.applovin.mediation.nativeAds.adPlacer.MaxAdPlacerSettings
import com.applovin.mediation.nativeAds.adPlacer.MaxRecyclerAdapter

class RecyclerViewNativeAdActivity : AppCompatActivity() {

    private val sampleData = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".chunked(1)
    private lateinit var adAdapter: MaxRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_native_recycler_view)

        // Create recycler adapter
        val originalAdapter = CustomRecyclerAdapter(this, sampleData)

        // Configure ad adapter
        val settings = MaxAdPlacerSettings("YOUR_AD_UNIT_ID")
        settings.addFixedPosition(2)
        settings.addFixedPosition(8)
        settings.repeatingInterval = 6

        // If using custom views, you must also set the nativeAdViewBinder on the adapter

        adAdapter = MaxRecyclerAdapter(settings, originalAdapter, this)
        adAdapter.setListener(object : MaxAdPlacer.Listener {
            override fun onAdLoaded(position: Int) {}

            override fun onAdRemoved(position: Int) {}

            override fun onAdClicked(ad: MaxAd?) {}

            override fun onAdRevenuePaid(ad: MaxAd?) {}
        })

        // Configure recycler view
        val recyclerView = findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.adapter = adAdapter
        recyclerView.layoutManager = LinearLayoutManager(this)

        adAdapter.loadAds()
    }

    override fun onDestroy() {
        adAdapter.destroy()
        super.onDestroy()
    }

    class CustomRecyclerAdapter(private val activity: Activity, val data: List<String>) : RecyclerView.Adapter<CustomRecyclerAdapter.ViewHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = activity.layoutInflater.inflate(R.layout.activity_text_recycler_view_holder, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            holder.textView.text = data[position]
        }

        override fun getItemCount(): Int {
            return data.size
        }

        class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textView: TextView = itemView.findViewById(R.id.text_view)
        }

    }
}
