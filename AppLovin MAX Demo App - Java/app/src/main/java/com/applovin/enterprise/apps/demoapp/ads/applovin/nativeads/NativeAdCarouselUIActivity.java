package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads;

import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.enterprise.apps.demoapp.ads.applovin.AdStatusActivity;
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.AppLovinCarouselViewSettings;
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.cards.InlineCarouselCardMediaView;
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.cards.InlineCarouselCardState;
import com.applovin.nativeAds.AppLovinNativeAd;
import com.applovin.nativeAds.AppLovinNativeAdLoadListener;
import com.applovin.nativeAds.AppLovinNativeAdPrecacheListener;
import com.applovin.sdk.AppLovinErrorCodes;
import com.applovin.sdk.AppLovinPostbackListener;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;

import java.util.List;

public class NativeAdCarouselUIActivity
        extends AdStatusActivity
{

    private static final int NUM_ADS_TO_LOAD = 1;

    private AppLovinNativeAd nativeAd;

    private ImageView appIcon;
    private ImageView appRating;
    private TextView  appTitleTextView;
    private TextView  appDescriptionTextView;
    private Button    appDownloadButton;

    private FrameLayout mediaViewPlaceholder;

    private TextView impressionStatusTextView;
    private Button   loadButton;
    private Button   precacheButton;
    private Button   showButton;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_native_ad_carousel_ui );

        adStatusTextView = (TextView) findViewById( R.id.status_label );
        impressionStatusTextView = (TextView) findViewById( R.id.impressionStatusTextView );
        appRating = (ImageView) findViewById( R.id.appRating );
        appTitleTextView = (TextView) findViewById( R.id.appTitleTextView );
        appDescriptionTextView = (TextView) findViewById( R.id.appDescriptionTextView );
        mediaViewPlaceholder = (FrameLayout) findViewById( R.id.mediaViewPlaceholder );

        appIcon = (ImageView) findViewById( R.id.appIcon );
        appIcon.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if ( nativeAd != null )
                {
                    nativeAd.launchClickTarget( findViewById( android.R.id.content ).getContext() );
                }
            }
        } );

        loadButton = (Button) findViewById( R.id.loadButton );
        loadButton.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                log( "Native ad loading..." );

                loadButton.setEnabled( false );
                precacheButton.setEnabled( false );
                showButton.setEnabled( false );

                impressionStatusTextView.setText( "No impression to track" );

                loadNativeAds( NUM_ADS_TO_LOAD );
            }
        } );

        precacheButton = (Button) findViewById( R.id.precacheButton );
        precacheButton.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                log( "Native ad precaching..." );

                final AppLovinSdk sdk = AppLovinSdk.getInstance( getApplicationContext() );
                sdk.getNativeAdService().precacheResources( nativeAd, new AppLovinNativeAdPrecacheListener()
                {
                    @Override
                    public void onNativeAdImagesPrecached(AppLovinNativeAd appLovinNativeAd)
                    {
                        log( "Native ad precached images" );
                    }

                    @Override
                    public void onNativeAdVideoPreceached(AppLovinNativeAd appLovinNativeAd)
                    {
                        // This will get called whether an ad actually has a video to precache or not
                        log( "Native ad done precaching" );

                        runOnUiThread( new Runnable()
                        {
                            @Override
                            public void run()
                            {
                                showButton.setEnabled( true );
                                precacheButton.setEnabled( false );
                            }
                        });
                    }

                    @Override
                    public void onNativeAdImagePrecachingFailed(AppLovinNativeAd appLovinNativeAd, int i)
                    {
                        log( "Native ad failed to precache images with error code " + i );
                    }

                    @Override
                    public void onNativeAdVideoPrecachingFailed(AppLovinNativeAd appLovinNativeAd, int i)
                    {
                        log( "Native ad failed to precache videos with error code " + i );
                    }
                } );
            }
        } );

        showButton = (Button) findViewById( R.id.showButton );
        showButton.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                runOnUiThread( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        log( "Native ad rendered" );

                        loadButton.setEnabled( true );
                        showButton.setEnabled( false );

                        appTitleTextView.setText( nativeAd.getTitle() );
                        appDescriptionTextView.setText( nativeAd.getDescriptionText() );

                        AppLovinSdkUtils.safePopulateImageView( appIcon, Uri.parse( nativeAd.getIconUrl() ), AppLovinSdkUtils.dpToPx( getApplicationContext(), AppLovinCarouselViewSettings.ICON_IMAGE_MAX_SCALE_SIZE ) );

                        final Drawable starRatingDrawable = getStarRatingDrawable( nativeAd.getStarRating() );
                        appRating.setImageDrawable( starRatingDrawable );

                        appDownloadButton.setText( nativeAd.getCtaText() );

                        InlineCarouselCardMediaView mediaView = new InlineCarouselCardMediaView( NativeAdCarouselUIActivity.this );
                        mediaView.setAd( nativeAd );
                        mediaView.setCardState( new InlineCarouselCardState() );
                        mediaView.setSdk( AppLovinSdk.getInstance( getApplicationContext() ) );
                        mediaView.setUiHandler( new Handler( Looper.getMainLooper() ) );
                        mediaView.setUpView();
                        mediaView.autoplayVideo();

                        mediaViewPlaceholder.removeAllViews();
                        mediaViewPlaceholder.addView(mediaView);

                        //
                        // You are responsible for firing impressions
                        //
                        trackImpression( nativeAd );
                    }
                } );
            }
        } );

        appDownloadButton = (Button) findViewById( R.id.appDownloadButton );
        appDownloadButton.setOnClickListener( new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                if ( nativeAd != null )
                {
                    nativeAd.launchClickTarget( findViewById( android.R.id.content ).getContext() );
                }
            }
        } );
    }

    public void loadNativeAds(final int numAdsToLoad)
    {
        final AppLovinSdk sdk = AppLovinSdk.getInstance( getApplicationContext() );
        sdk.getNativeAdService().loadNativeAds( numAdsToLoad, new AppLovinNativeAdLoadListener()
        {
            @Override
            public void onNativeAdsLoaded(final List list)
            {
                // Native ads loaded; do something with this, e.g. render into your custom view.

                runOnUiThread( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        log( "Native ad loaded, assets not retrieved yet." );

                        nativeAd = (AppLovinNativeAd) list.get( 0 );
                        precacheButton.setEnabled( true );
                    }
                } );
            }

            @Override
            public void onNativeAdsFailedToLoad(final int errorCode)
            {
                // Native ads failed to load for some reason, likely a network error.
                // Compare errorCode to the available constants in AppLovinErrorCodes.

                log( "Native ad failed to load with error code " + errorCode );

                if ( errorCode == AppLovinErrorCodes.NO_FILL )
                {
                    // No ad was available for this placement
                }
                // else if (errorCode == .... ) { ... }
            }
        } );
    }

    // Track an impression, though all other postbacks are handled identically
    private void trackImpression(final AppLovinNativeAd nativeAd)
    {
        impressionStatusTextView.setText( "Tracking Impression..." );

        nativeAd.trackImpression( new AppLovinPostbackListener()
        {
            @Override
            public void onPostbackSuccess(String url)
            {
                // Impression tracked!
                runOnUiThread( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        impressionStatusTextView.setText( "Impression Tracked!" );
                    }
                } );
            }

            @Override
            public void onPostbackFailure(String url, int errorCode)
            {
                // Impression could not be tracked. Retry the postback later.
                runOnUiThread( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        impressionStatusTextView.setText( "Impression Failed to Track!" );
                    }
                } );
            }
        } );
    }

    private Drawable getStarRatingDrawable(final float starRating)
    {
        final String sanitizedRating = Float.toString( starRating ).replace( ".", "_" );
        final String resourceName = "applovin_star_sprite_" + sanitizedRating;
        final int drawableId = getApplicationContext().getResources().getIdentifier( resourceName, "drawable", getApplicationContext().getPackageName() );

        return getApplicationContext().getResources().getDrawable( drawableId );
    }
}
