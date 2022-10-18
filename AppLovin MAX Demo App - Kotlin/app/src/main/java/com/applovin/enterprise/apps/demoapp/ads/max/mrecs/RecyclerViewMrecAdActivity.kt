package com.applovin.enterprise.apps.demoapp.ads.max.mrecs

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.applovin.enterprise.apps.demoapp.R
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAdView
import com.applovin.sdk.AppLovinSdkUtils

class RecyclerViewMrecAdActivity : AppCompatActivity(), MaxAdViewAdListener {
    private val AD_VIEW_COUNT = 5
    private val AD_INTERVAL = 10
    private val sampleData = ArrayList("ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".chunked(1))
    private val adViews = ArrayList<MaxAdView>(AD_VIEW_COUNT)
    private lateinit var adapter: CustomRecyclerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mrec_recycler_view)
        setTitle(R.string.activity_recycler_view_mrecs)

        adapter = CustomRecyclerAdapter(this, sampleData)

        // Configure recycler view
        val recyclerView = findViewById<RecyclerView>(R.id.mrec_recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter

        configureAdViews()
    }

    private fun configureAdViews() {
        // Insert rows at each interval to be used to display an ad
        for (i in sampleData.indices step AD_INTERVAL) {
            sampleData.add(i, "")
            adapter.notifyItemInserted(i)
        }

        for (i in 0 until AD_VIEW_COUNT) {
            val adView = MaxAdView("YOUR_AD_UNIT_ID", MaxAdFormat.MREC, this)
            adView.setListener(this)

            // Set this extra parameter to work around SDK bug that ignores calls to stopAutoRefresh()
            adView.setExtraParameter("allow_pause_auto_refresh_immediately", "true")
            adView.stopAutoRefresh()

            // Load the ad
            adView.loadAd()
            adViews.add(adView)
        }
    }

    //region RecyclerAdapter and ViewHolder

    private enum class ViewHolderType {
        AD_VIEW,
        CUSTOM_VIEW
    }

    inner class CustomRecyclerAdapter(private val activity: Activity, val data: List<String>) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val viewHolderType = ViewHolderType.values()[viewType]

            return when (viewHolderType) {
                ViewHolderType.AD_VIEW -> AdViewHolder(activity.layoutInflater.inflate(R.layout.activity_mrec_ad_view_holder, parent, false))
                ViewHolderType.CUSTOM_VIEW -> CustomViewHolder(activity.layoutInflater.inflate(R.layout.activity_text_recycler_view_holder, parent, false))
            }
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            if (holder is AdViewHolder) {
                // Select an ad view to display
                val adView: MaxAdView = adViews[(position / AD_INTERVAL) % AD_VIEW_COUNT]

                // Configure view holder with an ad
                holder.configure(adView)
            } else if (holder is CustomViewHolder) {
                holder.textView.text = data[position]
            }
        }

        override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
            super.onViewRecycled(holder)

            if (holder is AdViewHolder) {
                (holder.itemView as ViewGroup).removeView(holder.adView)
                holder.adView = null
            }
        }

        override fun onViewDetachedFromWindow(holder: RecyclerView.ViewHolder) {
            super.onViewDetachedFromWindow(holder)

            if (holder is AdViewHolder) {
                holder.stopAutoRefresh()
            }
        }

        override fun getItemCount(): Int {
            return data.count()
        }

        override fun getItemViewType(position: Int): Int {
            return if (position % AD_INTERVAL == 0) ViewHolderType.AD_VIEW.ordinal else ViewHolderType.CUSTOM_VIEW.ordinal
        }

        inner class AdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            var adView: MaxAdView? = null

            fun configure(adView: MaxAdView) {
                this.adView = adView
                this.adView!!.startAutoRefresh()

                // MREC width and height are 300 and 250 respectively, on phones and tablets
                val widthPx = AppLovinSdkUtils.dpToPx(this@RecyclerViewMrecAdActivity, 300)
                val heightPx = AppLovinSdkUtils.dpToPx(this@RecyclerViewMrecAdActivity, 250)

                // Set background or background color for MRECs to be fully functional
                this.adView!!.setBackgroundColor(Color.BLACK)
                this.adView!!.layoutParams = FrameLayout.LayoutParams(widthPx, heightPx, Gravity.CENTER)
                (itemView as ViewGroup).addView(this.adView)
            }

            fun stopAutoRefresh() {
                adView?.setExtraParameter("allow_pause_auto_refresh_immediately", "true")
                adView?.stopAutoRefresh()
            }
        }

        inner class CustomViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val textView: TextView = itemView.findViewById(R.id.text_view)
        }
    }

    //endregion

    //region MAX Ad Listener

    override fun onAdLoaded(maxAd: MaxAd) {}

    override fun onAdLoadFailed(adUnitId: String?, error: MaxError?) {}

    override fun onAdDisplayFailed(ad: MaxAd?, error: MaxError?) {}

    override fun onAdClicked(maxAd: MaxAd) {}

    override fun onAdExpanded(maxAd: MaxAd) {}

    override fun onAdCollapsed(maxAd: MaxAd) {}

    override fun onAdDisplayed(maxAd: MaxAd) { /* DO NOT USE - THIS IS RESERVED FOR FULLSCREEN ADS ONLY AND WILL BE REMOVED IN A FUTURE SDK RELEASE */ }

    override fun onAdHidden(maxAd: MaxAd) { /* DO NOT USE - THIS IS RESERVED FOR FULLSCREEN ADS ONLY AND WILL BE REMOVED IN A FUTURE SDK RELEASE */ }

    //endregion
}
