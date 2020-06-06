package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui;

import android.app.Activity;
import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.FrameLayout;
import android.widget.ProgressBar;

import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.support.SdkCenteredViewPager;
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.util.LayoutUtils;
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.cards.InlineCarouselAdapter;
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.cards.InlineCarouselCardState;
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.cards.InlineCarouselCardView;
import com.applovin.nativeAds.AppLovinNativeAd;
import com.applovin.nativeAds.AppLovinNativeAdLoadListener;
import com.applovin.sdk.AppLovinSdk;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.util.LayoutUtils.MATCH_PARENT;
import static com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.util.LayoutUtils.WRAP_CONTENT;

/**
 * A default AppLovin view which can be used to render native Ads.
 * Attaching this view to a layout will automatically load ads into it.
 */
public class AppLovinCarouselView
        extends FrameLayout
        implements AppLovinActivityCallbacks
{
    private static final String TAG = "AppLovinWidgetView";

    // Parents
    private Activity    parentActivity;
    private Handler     uiHandler;
    private AppLovinSdk sdk;

    private boolean wasPaused;

    // Owned objects
    private List<AppLovinNativeAd> nativeAds;

    // User callbacks
    private volatile AppLovinNativeAdLoadListener loadListener;

    // State tracking
    private int lastActiveCardIndex;

    // UI support
    private InlineCarouselAdapter                 adapter;
    private SdkCenteredViewPager                  carouselViewPager;
    private InlineCarouselCardView                singleCardView;
    private FrameLayout                           loadingIndicator;
    private Map<Integer, InlineCarouselCardState> cardStates;

    public AppLovinCarouselView(Context context)
    {
        this( context, null );
    }

    public AppLovinCarouselView(Context context, AttributeSet attrs)
    {
        this( context, attrs, 0 );
    }

    public AppLovinCarouselView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        this( context, attrs, defStyleAttr, AppLovinSdk.getInstance( context ), null );
    }

    public AppLovinCarouselView(Context context, AttributeSet attrs, int defStyleAttr, AppLovinSdk sdk, List<AppLovinNativeAd> nativeAds)
    {
        super( context, attrs, defStyleAttr );

        if ( !isInEditMode() )
        {
            this.sdk = sdk;
            this.cardStates = new HashMap<Integer, InlineCarouselCardState>();
            this.nativeAds = nativeAds;

            if ( context instanceof Activity )
            {
                parentActivity = (Activity) context;
            }

            renderActivityIndicator();
        }
    }

    /**
     * Set a load lisetner to be notified if this carousel view automatically loads a carousel ad.
     * <p/>
     * If you do not explicitly provide ads {@link AppLovinCarouselView#setNativeAds(List)}, the view will load one automatically upon being attached ot the window.
     */
    public void setLoadListener(AppLovinNativeAdLoadListener loadListener)
    {
        this.loadListener = loadListener;
    }

    /**
     * Return an immutable copy of the native ads currently rendered in this view.
     *
     * @return Copy of current set of native ads.
     */
    public List<AppLovinNativeAd> getNativeAds()
    {
        if ( nativeAds != null )
        {
            return Collections.unmodifiableList( nativeAds );
        }
        else
        {
            return Collections.unmodifiableList( new ArrayList<AppLovinNativeAd>( 0 ) );
        }
    }

    /**
     * Provide a specific set of native ads to be rendered into this view.
     *
     * @param nativeAds Ads to render.
     */
    public void setNativeAds(List<AppLovinNativeAd> nativeAds)
    {
        if ( this.nativeAds == null )
        {
            this.nativeAds = nativeAds;
            renderCarousel();
        }
        else
        {
            Log.d( TAG, "Cannot render a new native ad group into a carousel view that's already been populated." );
        }
    }

    public void renderCarousel()
    {
        runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                if ( !( Build.VERSION.SDK_INT >= 16 ) )
                {
                    Log.e( TAG, "AppLovin CarouselView cannot be rendered on systems older than Jelly Bean (4.1); drawing blank view..." );
                    return;
                }

                try
                {
                    final int numCards = nativeAds.size();
                    if ( numCards == 1 )
                    {
                        // If there is only one ad, don't bother w/ a view pager. JUst attach a card to the parent layout.
                        renderSingleView();
                        removeLoadingIndicator();
                    }
                    else if ( numCards >= 2 )
                    {
                        // 2+ cards means we need a view pager.
                        singleCardView = null;
                        renderViewPager();

                        if ( lastActiveCardIndex > 0 )
                        {
                            carouselViewPager.setCurrentItem( lastActiveCardIndex, false );
                        }

                    }
                }
                catch ( Exception ex )
                {
                    Log.e( TAG, "Unable to render carousel view: ", ex );
                }
            }
        } );
    }

    private void renderSingleView()
    {
        singleCardView = new InlineCarouselCardView( getContext() );
        singleCardView.setSdk( sdk );
        singleCardView.setAd( nativeAds.get( 0 ) );

        final InlineCarouselCardState singleCardState = new InlineCarouselCardState();
        singleCardState.setCurrentlyActive( true );

        singleCardView.setCardState( singleCardState );
        singleCardView.setUpView();

        singleCardView.setLayoutParams( LayoutUtils.createLinearParams( LayoutUtils.MATCH_PARENT, LayoutUtils.MATCH_PARENT, Gravity.CENTER, new LayoutUtils.DPMargins( getContext(), AppLovinCarouselViewSettings.VIEW_PAGER_MARGIN, 0, AppLovinCarouselViewSettings.VIEW_PAGER_MARGIN, 0 ) ) );

        addView( singleCardView );
    }

    private void renderViewPager()
    {
        // Use view pager
        carouselViewPager = new SdkCenteredViewPager( getContext() );
        carouselViewPager.setFocusable( false );
        carouselViewPager.setFocusableInTouchMode( false );
        carouselViewPager.setLayoutParams( LayoutUtils.createLinearParams( LayoutUtils.MATCH_PARENT, LayoutUtils.MATCH_PARENT, Gravity.CENTER ) );
        carouselViewPager.setBackgroundColor( AppLovinCarouselViewSettings.VIEW_PAGER_BACKGROUND_COLOR );
        carouselViewPager.setPageMargin( AppLovinCarouselViewSettings.VIEW_PAGER_MARGIN );
        carouselViewPager.setOffscreenPageLimit( AppLovinCarouselViewSettings.VIEW_PAGER_OFF_SCREEN_PAGE_LIMIT );

        carouselViewPager.setClipToPadding( false );

        adapter = new InlineCarouselAdapter( getContext(), sdk, this );

        carouselViewPager.setAdapter( this.adapter );

        carouselViewPager.setOnPageChangeListener( new SdkCenteredViewPager.OnPageChangeListener()
        {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels)
            {
                // Useless for us.
            }

            @Override
            public void onPageScrollStateChanged(int state)
            {
                if ( state == SdkCenteredViewPager.SCROLL_STATE_IDLE )
                {
                    final int currentItem = carouselViewPager.getCurrentItem();
                    lastActiveCardIndex = currentItem;

                    // Activate the current card if it exists (which it should always)
                    activateCard( currentItem );

                    // Deactivate the left card if it exists
                    deactivateCard( currentItem - 1 );

                    // Deactivate the right card if it exists
                    deactivateCard( currentItem + 1 );
                }
            }

            @Override
            public void onPageSelected(int position)
            {
                // Useless, invoked when .setPage(int) is called
            }
        } );

        addView( carouselViewPager );
        removeLoadingIndicator();
    }

    public InlineCarouselCardState getCardState(final int position)
    {
        Log.d( TAG, "Looking up card state for position " + position );
        if ( position < 0 )
        {
            return null;
        }

        if ( cardStates.size() >= position + 1 )
        {
            final InlineCarouselCardState state = cardStates.get( position );
            if ( state != null )
            {
                Log.d( TAG, "Returning existing card state for position " + position );
                return state;
            }
        }

        Log.d( TAG, "Instantiating new card state for position " + position );
        final InlineCarouselCardState state = new InlineCarouselCardState();
        cardStates.put( position, state );
        return state;
    }

    private void activateCard(int currentItem)
    {
        final InlineCarouselCardState cardState = getCardState( currentItem );
        if ( cardState != null )
        {
            if ( !cardState.isCurrentlyActive() )
            {
                final WeakReference<InlineCarouselCardView> currentCardRef = adapter.getExistingCard( currentItem );
                if ( currentCardRef != null )
                {
                    final InlineCarouselCardView currentCard = currentCardRef.get();
                    if ( currentCard != null )
                    {
                        currentCard.onCardActivated();
                    }
                }
            }
        }
    }

    private void deactivateCard(int currentItem)
    {
        final InlineCarouselCardState cardState = getCardState( currentItem );
        if ( cardState != null )
        {
            if ( cardState.isCurrentlyActive() )
            {
                final WeakReference<InlineCarouselCardView> currentCardRef = adapter.getExistingCard( currentItem );
                if ( currentCardRef != null )
                {
                    final InlineCarouselCardView currentCard = currentCardRef.get();
                    if ( currentCard != null )
                    {
                        currentCard.onCardDeactivated();
                    }
                }
            }
        }
    }


    /**
     * Intercept touch events which are consumed later by the video view and forward them to the view pager. This is a workaround for an Android limitation: OnClickListeners on the video views absorb touch events and prevent events from propagating up. By intercepting here & manually dispatching,
     * the view pager still receives drag events even if the video views consume click events.
     */
    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev)
    {
        if ( carouselViewPager != null )
        {
            carouselViewPager.onTouchEvent( ev );
        }

        return false;
    }

    @Override
    protected void onAttachedToWindow()
    {
        super.onAttachedToWindow();

        if ( nativeAds == null && AppLovinCarouselViewSettings.NUM_ADS_TO_AUTOLOAD > 0 )
        {
            sdk.getNativeAdService().loadNativeAds( AppLovinCarouselViewSettings.NUM_ADS_TO_AUTOLOAD, new AppLovinNativeAdLoadListener()
            {
                @Override
                public void onNativeAdsLoaded(final List /* <AppLovinNativeAd> */ nativeAds)
                {
                    getUiHandler().post( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            setNativeAds( nativeAds );

                            if ( loadListener != null )
                            {
                                loadListener.onNativeAdsLoaded( nativeAds );
                            }
                        }
                    } );
                }

                @Override
                public void onNativeAdsFailedToLoad(final int errorCode)
                {
                    getUiHandler().post( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            if ( loadListener != null )
                            {
                                loadListener.onNativeAdsFailedToLoad( errorCode );
                            }
                        }
                    } );
                }
            } );
        }
    }

    private void removeLoadingIndicator()
    {
        // Fade out the loading indicator - post delayed to allow time for viewpager sizing to complete (due to android layout passes)
        postDelayed( new Runnable()
        {
            @Override
            public void run()
            {
                final AlphaAnimation fadeOut = new AlphaAnimation( 1f, 0f );
                fadeOut.setDuration( 1000 );
                fadeOut.setAnimationListener( new Animation.AnimationListener()
                {
                    @Override
                    public void onAnimationStart(Animation animation)
                    {
                    }

                    @Override
                    public void onAnimationEnd(Animation animation)
                    {
                        removeView( loadingIndicator );
                        loadingIndicator = null;

                        getUiHandler().postDelayed( new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                // Scroll and center-lock the first card.
                                if ( carouselViewPager != null )
                                {
                                    carouselViewPager.scrollToItem( lastActiveCardIndex, true, 20, false );
                                }
                            }
                        }, 500 );
                    }

                    @Override
                    public void onAnimationRepeat(Animation animation)
                    {

                    }
                } );

                loadingIndicator.startAnimation( fadeOut );
            }
        }, 1000 );
    }

    private void renderActivityIndicator()
    {
        loadingIndicator = new FrameLayout( getContext() );
        loadingIndicator.setLayoutParams( LayoutUtils.createFrameParams( MATCH_PARENT, MATCH_PARENT, Gravity.CENTER ) );
        loadingIndicator.setBackgroundColor( AppLovinCarouselViewSettings.VIEW_PAGER_BACKGROUND_COLOR );
        final ProgressBar progressBar = new ProgressBar( getContext() );
        progressBar.setIndeterminate( true );
        progressBar.setLayoutParams( LayoutUtils.createFrameParams( WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER ) );
        loadingIndicator.addView( progressBar );
        addView( loadingIndicator );
        bringChildToFront( loadingIndicator );
    }

    @Override
    public void onResume(Activity activity)
    {
        if ( parentActivity == null )
        {
            parentActivity = activity;
        }

        if ( wasPaused )
        {
            renderActivityIndicator();

            renderCarousel();

            if ( carouselViewPager != null )
            {
                carouselViewPager.setCurrentItem( lastActiveCardIndex );
                activateCard( carouselViewPager.getCurrentItem() );
            }
        }
    }

    @Override
    public void onStop(Activity activity)
    {
        wasPaused = true;

        try
        {
            adapter.destroyCards();
            adapter = null;

            removeAllViews();

            carouselViewPager = null;
            singleCardView = null;
        }
        catch ( Exception ex )
        {
            Log.w( TAG, "Error during activity stop", ex );
        }
    }

    private Handler getUiHandler()
    {
        if ( uiHandler == null )
        {
            uiHandler = new Handler( Looper.getMainLooper() );
        }

        return uiHandler;
    }

    private void runOnUiThread(final Runnable r)
    {
        if ( parentActivity != null )
        {
            parentActivity.runOnUiThread( r );
        }
        else
        {
            getUiHandler().post( r );
        }
    }
}
