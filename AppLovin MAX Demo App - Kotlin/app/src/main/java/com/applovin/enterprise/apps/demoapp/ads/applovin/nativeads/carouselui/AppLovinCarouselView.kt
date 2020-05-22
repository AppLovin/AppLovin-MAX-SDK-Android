package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui

import android.app.Activity
import android.content.Context
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.AttributeSet
import android.util.Log
import android.view.Gravity
import android.view.MotionEvent
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.FrameLayout
import android.widget.ProgressBar
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.cards.InlineCarouselAdapter
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.cards.InlineCarouselCardState
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.cards.InlineCarouselCardView
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.support.SdkCenteredViewPager
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.util.LayoutUtils
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.util.LayoutUtils.MATCH_PARENT
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.util.LayoutUtils.WRAP_CONTENT
import com.applovin.nativeAds.AppLovinNativeAd
import com.applovin.nativeAds.AppLovinNativeAdLoadListener
import com.applovin.sdk.AppLovinSdk
import java.util.*

/**
 * A default AppLovin view which can be used to render native Ads.
 * Attaching this view to a layout will automatically load ads into it.
 */
class AppLovinCarouselView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0, sdk: AppLovinSdk = AppLovinSdk.getInstance(context), nativeAds: List<AppLovinNativeAd>? = null) : FrameLayout(context, attrs, defStyleAttr),
    AppLovinActivityCallbacks
{

    // Parents
    private var parentActivity: Activity? = null
    private var uiHandler: Handler? = null
    private var sdk: AppLovinSdk? = null

    private var wasPaused: Boolean = false

    // Owned objects
    private var nativeAds: List<AppLovinNativeAd>? = null

    // User callbacks
    @Volatile private var loadListener: AppLovinNativeAdLoadListener? = null

    // State tracking
    private var lastActiveCardIndex: Int = 0

    // UI support
    private var adapter: InlineCarouselAdapter? = null
    private var carouselViewPager: SdkCenteredViewPager? = null
    private var singleCardView: InlineCarouselCardView? = null
    private var loadingIndicator: FrameLayout? = null
    private var cardStates: MutableMap<Int, InlineCarouselCardState>? = null

    init
    {

        if (!isInEditMode)
        {
            this.sdk = sdk
            this.cardStates = HashMap<Int, InlineCarouselCardState>()
            this.nativeAds = nativeAds

            if (context is Activity)
            {
                parentActivity = context
            }

            renderActivityIndicator()
        }
    }

    /**
     * Set a load lisetner to be notified if this carousel view automatically loads a carousel ad.
     *
     *
     * If you do not explicitly provide ads [AppLovinCarouselView.setNativeAds], the view will load one automatically upon being attached ot the window.
     */
    fun setLoadListener(loadListener: AppLovinNativeAdLoadListener)
    {
        this.loadListener = loadListener
    }

    /**
     * Return an immutable copy of the native ads currently rendered in this view.

     * @return Copy of current set of native ads.
     */
    fun getNativeAds(): List<AppLovinNativeAd>
    {
        if (nativeAds != null)
        {
            return Collections.unmodifiableList(nativeAds!!)
        }
        else
        {
            return Collections.unmodifiableList(ArrayList<AppLovinNativeAd>(0))
        }
    }

    /**
     * Provide a specific set of native ads to be rendered into this view.

     * @param nativeAds Ads to render.
     */
    fun setNativeAds(nativeAds: List<AppLovinNativeAd>)
    {
        if (this.nativeAds == null)
        {
            this.nativeAds = nativeAds
            renderCarousel()
        }
        else
        {
            Log.d(TAG, "Cannot render a new native ad group into a carousel view that's already been populated.")
        }
    }

    fun renderCarousel()
    {
        runOnUiThread(Runnable {
            if (Build.VERSION.SDK_INT < 16)
            {
                Log.e(TAG, "AppLovin CarouselView cannot be rendered on systems older than Jelly Bean (4.1); drawing blank view...")
                return@Runnable
            }

            try
            {
                val numCards = nativeAds!!.size
                if (numCards == 1)
                {
                    // If there is only one ad, don't bother w/ a view pager. JUst attach a card to the parent layout.
                    renderSingleView()
                    removeLoadingIndicator()
                }
                else if (numCards >= 2)
                {
                    // 2+ cards means we need a view pager.
                    singleCardView = null
                    renderViewPager()

                    if (lastActiveCardIndex > 0)
                    {
                        carouselViewPager!!.setCurrentItem(lastActiveCardIndex, false)
                    }

                }
            }
            catch (ex: Exception)
            {
                Log.e(TAG, "Unable to render carousel view: ", ex)
            }
        })
    }

    private fun renderSingleView()
    {
        singleCardView = InlineCarouselCardView(context)
        singleCardView!!.sdk = sdk
        singleCardView!!.ad = nativeAds!![0]

        val singleCardState = InlineCarouselCardState()
        singleCardState.isCurrentlyActive = true

        singleCardView!!.cardState = singleCardState
        singleCardView!!.setUpView()

        singleCardView!!.layoutParams = LayoutUtils.createLinearParams(LayoutUtils.MATCH_PARENT, LayoutUtils.MATCH_PARENT, Gravity.CENTER, LayoutUtils.DPMargins(context, AppLovinCarouselViewSettings.VIEW_PAGER_MARGIN, 0, AppLovinCarouselViewSettings.VIEW_PAGER_MARGIN, 0))

        addView(singleCardView)
    }

    private fun renderViewPager()
    {
        // Use view pager
        carouselViewPager = SdkCenteredViewPager(context)
        carouselViewPager!!.isFocusable = false
        carouselViewPager!!.isFocusableInTouchMode = false
        carouselViewPager!!.layoutParams = LayoutUtils.createLinearParams(LayoutUtils.MATCH_PARENT, LayoutUtils.MATCH_PARENT, Gravity.CENTER)
        carouselViewPager!!.setBackgroundColor(AppLovinCarouselViewSettings.VIEW_PAGER_BACKGROUND_COLOR)
        carouselViewPager!!.pageMargin = AppLovinCarouselViewSettings.VIEW_PAGER_MARGIN
        carouselViewPager!!.offscreenPageLimit = AppLovinCarouselViewSettings.VIEW_PAGER_OFF_SCREEN_PAGE_LIMIT

        carouselViewPager!!.clipToPadding = false

        adapter = InlineCarouselAdapter(context, sdk!!, this)

        carouselViewPager!!.adapter = this.adapter

        carouselViewPager!!.setOnPageChangeListener(object : SdkCenteredViewPager.OnPageChangeListener
                                                    {
                                                        override fun onPageScrolled(position: Int, positionOffset: Float, positionOffsetPixels: Int)
                                                        {
                                                            // Useless for us.
                                                        }

                                                        override fun onPageScrollStateChanged(state: Int)
                                                        {
                                                            if (state == SdkCenteredViewPager.SCROLL_STATE_IDLE)
                                                            {
                                                                val currentItem = carouselViewPager!!.currentItem
                                                                lastActiveCardIndex = currentItem

                                                                // Activate the current card if it exists (which it should always)
                                                                activateCard(currentItem)

                                                                // Deactivate the left card if it exists
                                                                deactivateCard(currentItem - 1)

                                                                // Deactivate the right card if it exists
                                                                deactivateCard(currentItem + 1)
                                                            }
                                                        }

                                                        override fun onPageSelected(position: Int)
                                                        {
                                                            // Useless, invoked when .setPage(int) is called
                                                        }
                                                    })

        addView(carouselViewPager)
        removeLoadingIndicator()
    }

    fun getCardState(position: Int): InlineCarouselCardState?
    {
        Log.d(TAG, "Looking up card state for position " + position)
        if (position < 0)
        {
            return null
        }

        if (cardStates!!.size >= position + 1)
        {
            val state = cardStates!![position]
            if (state != null)
            {
                Log.d(TAG, "Returning existing card state for position " + position)
                return state
            }
        }

        Log.d(TAG, "Instantiating new card state for position " + position)
        val state = InlineCarouselCardState()
        cardStates!!.put(position, state)
        return state
    }

    private fun activateCard(currentItem: Int)
    {
        val cardState = getCardState(currentItem)
        if (cardState != null)
        {
            if (!cardState.isCurrentlyActive)
            {
                val currentCardRef = adapter!!.getExistingCard(currentItem)
                if (currentCardRef != null)
                {
                    val currentCard = currentCardRef.get()
                    currentCard?.onCardActivated()
                }
            }
        }
    }

    private fun deactivateCard(currentItem: Int)
    {
        val cardState = getCardState(currentItem)
        if (cardState != null)
        {
            if (cardState.isCurrentlyActive)
            {
                val currentCardRef = adapter!!.getExistingCard(currentItem)
                if (currentCardRef != null)
                {
                    val currentCard = currentCardRef.get()
                    currentCard?.onCardDeactivated()
                }
            }
        }
    }


    /**
     * Intercept touch events which are consumed later by the video view and forward them to the view pager. This is a workaround for an Android limitation: OnClickListeners on the video views absorb touch events and prevent events from propagating up. By intercepting here & manually dispatching,
     * the view pager still receives drag events even if the video views consume click events.
     */
    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean
    {
        if (carouselViewPager != null)
        {
            carouselViewPager!!.onTouchEvent(ev)
        }

        return false
    }

    override fun onAttachedToWindow()
    {
        super.onAttachedToWindow()

        if (nativeAds == null && AppLovinCarouselViewSettings.NUM_ADS_TO_AUTOLOAD > 0)
        {
            sdk!!.nativeAdService.loadNativeAds(AppLovinCarouselViewSettings.NUM_ADS_TO_AUTOLOAD, object : AppLovinNativeAdLoadListener
            {
                override fun onNativeAdsLoaded(nativeAds: List<AppLovinNativeAd>)
                {
                    getUiHandler().post {
                        setNativeAds(nativeAds)

                        if (loadListener != null)
                        {
                            loadListener!!.onNativeAdsLoaded(nativeAds)
                        }
                    }
                }

                override fun onNativeAdsFailedToLoad(errorCode: Int)
                {
                    getUiHandler().post {
                        if (loadListener != null)
                        {
                            loadListener!!.onNativeAdsFailedToLoad(errorCode)
                        }
                    }
                }
            })
        }
    }

    private fun removeLoadingIndicator()
    {
        // Fade out the loading indicator - post delayed to allow time for viewpager sizing to complete (due to android layout passes)
        postDelayed({
                        val fadeOut = AlphaAnimation(1f, 0f)
                        fadeOut.duration = 1000
                        fadeOut.setAnimationListener(object : Animation.AnimationListener
                                                     {
                                                         override fun onAnimationStart(animation: Animation)
                                                         {
                                                         }

                                                         override fun onAnimationEnd(animation: Animation)
                                                         {
                                                             removeView(loadingIndicator)
                                                             loadingIndicator = null

                                                             getUiHandler().postDelayed({
                                                                                            // Scroll and center-lock the first card.
                                                                                            if (carouselViewPager != null)
                                                                                            {
                                                                                                carouselViewPager!!.scrollToItem(lastActiveCardIndex, true, 20, false)
                                                                                            }
                                                                                        }, 500)
                                                         }

                                                         override fun onAnimationRepeat(animation: Animation)
                                                         {

                                                         }
                                                     })

                        loadingIndicator!!.startAnimation(fadeOut)
                    }, 1000)
    }

    private fun renderActivityIndicator()
    {
        loadingIndicator = FrameLayout(context)
        loadingIndicator!!.layoutParams = LayoutUtils.createFrameParams(MATCH_PARENT, MATCH_PARENT, Gravity.CENTER)
        loadingIndicator!!.setBackgroundColor(AppLovinCarouselViewSettings.VIEW_PAGER_BACKGROUND_COLOR)
        val progressBar = ProgressBar(context)
        progressBar.isIndeterminate = true
        progressBar.layoutParams = LayoutUtils.createFrameParams(WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER)
        loadingIndicator!!.addView(progressBar)
        addView(loadingIndicator)
        bringChildToFront(loadingIndicator)
    }

    override fun onResume(activity: Activity)
    {
        if (parentActivity == null)
        {
            parentActivity = activity
        }

        if (wasPaused)
        {
            renderActivityIndicator()

            renderCarousel()

            if (carouselViewPager != null)
            {
                carouselViewPager!!.currentItem = lastActiveCardIndex
                activateCard(carouselViewPager!!.currentItem)
            }
        }
    }

    override fun onStop(activity: Activity)
    {
        wasPaused = true

        try
        {
            adapter!!.destroyCards()
            adapter = null

            removeAllViews()

            carouselViewPager = null
            singleCardView = null
        }
        catch (ex: Exception)
        {
            Log.w(TAG, "Error during activity stop", ex)
        }

    }

    private fun getUiHandler(): Handler
    {
        if (uiHandler == null)
        {
            uiHandler = Handler(Looper.getMainLooper())
        }

        return uiHandler!!
    }

    private fun runOnUiThread(r: Runnable)
    {
        if (parentActivity != null)
        {
            parentActivity!!.runOnUiThread(r)
        }
        else
        {
            getUiHandler().post(r)
        }
    }

    companion object
    {
        private val TAG = "AppLovinWidgetView"
    }
}
