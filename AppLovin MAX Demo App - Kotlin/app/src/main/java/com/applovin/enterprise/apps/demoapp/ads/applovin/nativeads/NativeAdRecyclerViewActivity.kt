package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads

import android.net.Uri
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.applovin.enterprise.apps.demoapp.kotlin.R
import com.applovin.nativeAds.AppLovinNativeAd
import com.applovin.nativeAds.AppLovinNativeAdLoadListener
import com.applovin.nativeAds.AppLovinNativeAdPrecacheListener
import com.applovin.nativeAds.AppLovinNativeAdService
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkUtils
import kotlinx.android.synthetic.main.activity_native_ad_recycler_view.*

class NativeAdRecyclerViewActivity : AppCompatActivity()
{

    override fun onCreate(savedInstanceState: Bundle?)
    {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_native_ad_recycler_view)

        // Load an initial batch of native ads.
        // In a real app, you'd ideally load smaller batches, and load more as the user scrolls.
        val sdk = AppLovinSdk.getInstance(this)
        val activityRef = this

        val nativeAdService = sdk.nativeAdService
        nativeAdService.loadNativeAds(10, object : AppLovinNativeAdLoadListener
        {
            override fun onNativeAdsLoaded(list: List<AppLovinNativeAd>)
            {
                runOnUiThread {
                    renderRecyclerView(list)
                    retrieveImageResources(nativeAdService, list)
                }
            }

            override fun onNativeAdsFailedToLoad(errorCode: Int)
            {
                runOnUiThread { Toast.makeText(activityRef, "Failed to load native ads: " + errorCode, Toast.LENGTH_LONG).show() }
            }
        })
    }

    private fun retrieveImageResources(nativeAdService: AppLovinNativeAdService, nativeAds: List<AppLovinNativeAd>)
    {
        val thisRef = this

        for (nativeAd in nativeAds)
        {
            nativeAdService.precacheResources(nativeAd, object : AppLovinNativeAdPrecacheListener
            {
                override fun onNativeAdImagesPrecached(appLovinNativeAd: AppLovinNativeAd)
                {
                    runOnUiThread {
                        val adapter = nativeAdsRecyclerView.adapter as NativeAdRecyclerViewAdapter
                        adapter.notifyItemChanged(nativeAds.indexOf(appLovinNativeAd))
                    }
                }

                override fun onNativeAdVideoPreceached(appLovinNativeAd: AppLovinNativeAd)
                {
                    // This example does not implement videos; see CarouselUINativeAdActivity for an example of a widget which does.
                }

                override fun onNativeAdImagePrecachingFailed(appLovinNativeAd: AppLovinNativeAd, errorCode: Int)
                {
                    runOnUiThread { Toast.makeText(thisRef, "Failed to load images for native ad: " + errorCode, Toast.LENGTH_LONG).show() }
                }

                override fun onNativeAdVideoPrecachingFailed(appLovinNativeAd: AppLovinNativeAd, i: Int)
                {
                    // This example does not implement videos; see CarouselUINativeAdActivity for an example of a widget which does.
                }
            })
        }
    }

    private fun renderRecyclerView(nativeAds: List<AppLovinNativeAd>)
    {
        nativeAdsRecyclerView.layoutManager = LinearLayoutManager(this)
        nativeAdsRecyclerView.adapter = NativeAdRecyclerViewAdapter(nativeAds)
    }

    private fun onRecyclerViewItemClicked(clickedView: View, nativeAds: List<AppLovinNativeAd>)
    {
        val itemPosition = nativeAdsRecyclerView.getChildAdapterPosition(clickedView)
        val ad = nativeAds[itemPosition]
        ad.launchClickTarget(this)
    }

    private inner class NativeAdRecyclerViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    {
        val appTitleTextView: TextView = itemView.findViewById<View>(R.id.appTitleTextView) as TextView
        val appDescriptionTextView: TextView = itemView.findViewById<View>(R.id.appDescriptionTextView) as TextView
        val appIconImageView: ImageView = itemView.findViewById<View>(R.id.appIconImageView) as ImageView
    }

    private inner class NativeAdRecyclerViewAdapter(private val nativeAds: List<AppLovinNativeAd>) : RecyclerView.Adapter<NativeAdRecyclerViewHolder>()
    {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NativeAdRecyclerViewHolder
        {
            val prototypeView = LayoutInflater.from(parent.context).inflate(R.layout.recycler_view_cell_nativead, parent, false)

            prototypeView.setOnClickListener({ v -> onRecyclerViewItemClicked(v, nativeAds) })

            return NativeAdRecyclerViewHolder(prototypeView)
        }

        override fun onBindViewHolder(holder: NativeAdRecyclerViewHolder, position: Int)
        {
            val nativeAd = nativeAds[position]

            holder.appTitleTextView.text = nativeAd.title
            holder.appDescriptionTextView.text = nativeAd.descriptionText

            val maxSizeDp = 50 // match the size defined in the XML layout
            AppLovinSdkUtils.safePopulateImageView(holder.appIconImageView, Uri.parse(nativeAd.imageUrl), maxSizeDp)

            // Track impression
            nativeAd.trackImpression()
        }

        override fun getItemCount(): Int
        {
            return nativeAds.size
        }
    }
}
