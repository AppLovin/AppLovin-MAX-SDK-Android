package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.cards

import android.content.Context
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import com.applovin.enterprise.apps.demoapp.kotlin.R
import com.applovin.apps.kotlindemoapp.nativeads.carouselui.cards.InlineCarouselCardCallbacks
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.AppLovinCarouselViewSettings
import com.applovin.apps.kotlindemoapp.nativeads.carouselui.support.AppLovinTouchToClickListener
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.util.LayoutUtils
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.util.LayoutUtils.WRAP_CONTENT
import com.applovin.nativeAds.AppLovinNativeAd
import com.applovin.nativeAds.AppLovinNativeAdPrecacheListener
import com.applovin.sdk.AppLovinSdk
import com.applovin.sdk.AppLovinSdkUtils

/**
 * This view represents a single 'card' within a carousel view.
 */
class InlineCarouselCardView : FrameLayout, InlineCarouselCardCallbacks, AppLovinNativeAdPrecacheListener
{

    var sdk: AppLovinSdk? = null
    var ad: AppLovinNativeAd? = null
    private var uiHandler: Handler? = null

    private var slotRendered: Boolean = false
    @Volatile private var videoPlayerNotificationRequested: Boolean = false
    var cardState: InlineCarouselCardState? = null
    private var loadingIndicator: ProgressBar? = null

    private var contentLayout: LinearLayout? = null
    private var appIconImageView: ImageView? = null
    private var appTitleTextView: TextView? = null
    private var appDescriptionTextView: TextView? = null
    private var mediaView: InlineCarouselCardMediaView? = null
    private var starRatingImageView: ImageView? = null
    private var downloadCTATextView: TextView? = null
    private var downloadButton: Button? = null

    constructor(context: Context) : super(context)

    constructor(context: Context, attrs: AttributeSet) : super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int) : super(context, attrs, defStyleAttr)

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    fun setUpView()
    {
        this.uiHandler = Handler(Looper.getMainLooper())

        val inflater = context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater
        inflater.inflate(R.layout.applovin_card_view, this, true)

        bindViews()

        renderActivityIndicator()

        if (sdk == null)
        {
            sdk = AppLovinSdk.getInstance(context)
        }

        sdk!!.nativeAdService.precacheResources(ad, this)
    }

    private fun bindViews()
    {
        contentLayout = findViewById<View>(R.id.applovin_card_content_layout) as LinearLayout
        appIconImageView = findViewById<View>(R.id.applovin_card_app_icon) as ImageView
        appTitleTextView = findViewById<View>(R.id.applovin_card_title) as TextView
        appDescriptionTextView = findViewById<View>(R.id.applovin_card_app_description_text_view) as TextView
        mediaView = findViewById<View>(R.id.applovin_card_video_ad_view) as InlineCarouselCardMediaView
        starRatingImageView = findViewById<View>(R.id.applovin_card_star_rating) as ImageView
        downloadCTATextView = findViewById<View>(R.id.applovin_card_caption) as TextView
        downloadButton = findViewById<View>(R.id.applovin_card_action_button) as Button
    }

    fun renderCard()
    {
        if (!slotRendered)
        {
            slotRendered = true

            if (loadingIndicator != null)
            {
                removeView(loadingIndicator)
                loadingIndicator = null
            }

            mediaView!!.ad = ad
            mediaView!!.cardState = cardState
            mediaView!!.sdk = sdk
            mediaView!!.setUiHandler(uiHandler!!)
            mediaView!!.setUpView()

            appTitleTextView!!.text = ad!!.title
            appIconImageView!!.setOnClickListener { ad!!.launchClickTarget(context) }

            appDescriptionTextView!!.text = ad!!.descriptionText

            val clickBridge = AppLovinTouchToClickListener(context, View.OnClickListener { handleVideoClicked() })

            mediaView!!.setOnTouchListener(clickBridge)

            downloadCTATextView!!.text = ad!!.captionText
            downloadButton!!.text = ad!!.ctaText
            downloadButton!!.setOnClickListener { ad!!.launchClickTarget(context) }

            contentLayout!!.setOnClickListener { ad!!.launchClickTarget(context) }

            AppLovinSdkUtils.safePopulateImageView(appIconImageView, Uri.parse(ad!!.iconUrl),
                                                   AppLovinSdkUtils.dpToPx(context, AppLovinCarouselViewSettings.ICON_IMAGE_MAX_SCALE_SIZE))

            val starRatingDrawable = getStarRatingDrawable(ad!!.starRating)
            starRatingImageView!!.setImageDrawable(starRatingDrawable)

            if (videoPlayerNotificationRequested)
            {
                mediaView!!.onVideoPrecached(ad!!)
                videoPlayerNotificationRequested = false
            }
        }
    }

    private fun getStarRatingDrawable(starRating: Float): Drawable
    {
        val sanitizedRating = java.lang.Float.toString(starRating).replace(".", "_")
        val resourceName = "applovin_star_sprite_" + sanitizedRating
        Log.d("InlineCarouselCardView", "Looking up resource named: " + resourceName)
        val drawableId = context.resources.getIdentifier(resourceName, "drawable", context.packageName)
        return context.resources.getDrawable(drawableId)
    }

    private fun renderActivityIndicator()
    {
        loadingIndicator = ProgressBar(context)
        loadingIndicator!!.isIndeterminate = true
        loadingIndicator!!.layoutParams = LayoutUtils.createFrameParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER)
        addView(loadingIndicator)
        bringChildToFront(loadingIndicator)
    }

    override fun onCardActivated()
    {
        if (mediaView != null)
        {
            mediaView!!.onCardActivated()
        }

        cardState!!.isPreviouslyActivated = true
        cardState!!.isCurrentlyActive = true

        if (!cardState!!.isImpressionTracked)
        {
            cardState!!.isImpressionTracked = true
            ad!!.trackImpression()
        }
    }

    override fun onCardDeactivated()
    {
        if (mediaView != null)
        {
            mediaView!!.onCardDeactivated()
        }

        cardState!!.isCurrentlyActive = false
    }

    fun handleVideoClicked()
    {
        if (AppLovinCarouselViewSettings.TAP_TO_PAUSE_VIDEO)
        {
            mediaView!!.togglePlayback()
        }
        else
        {
            ad!!.launchClickTarget(context)
        }
    }

    override fun onNativeAdImagesPrecached(slot: AppLovinNativeAd)
    {
        uiHandler!!.post { renderCard() }
    }

    //    @Overrides
    override fun onNativeAdVideoPreceached(slot: AppLovinNativeAd)
    {
        if (mediaView != null)
        {
            uiHandler!!.post { mediaView!!.onVideoPrecached(slot) }
        }
        else
        {
            videoPlayerNotificationRequested = true
        }
    }

    override fun onNativeAdImagePrecachingFailed(ad: AppLovinNativeAd, errorCode: Int)
    {

    }

    override fun onNativeAdVideoPrecachingFailed(ad: AppLovinNativeAd, errorCode: Int)
    {

    }

    fun destroy()
    {
        if (mediaView != null)
        {
            mediaView!!.destroy()
        }

        removeAllViews()
    }
}