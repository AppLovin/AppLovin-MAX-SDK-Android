package com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.cards;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.RequiresApi;

import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.AppLovinCarouselViewSettings;
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.support.AppLovinTouchToClickListener;
import com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.util.LayoutUtils;
import com.applovin.nativeAds.AppLovinNativeAd;
import com.applovin.nativeAds.AppLovinNativeAdPrecacheListener;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;

import static com.applovin.enterprise.apps.demoapp.ads.applovin.nativeads.carouselui.util.LayoutUtils.WRAP_CONTENT;

/**
 * This view represents a single 'card' within a carousel view.
 */
public class InlineCarouselCardView
        extends FrameLayout
        implements InlineCarouselCardCallbacks, AppLovinNativeAdPrecacheListener
{

    private AppLovinSdk      sdk;
    private AppLovinNativeAd ad;
    private Handler          uiHandler;

    private          boolean                 slotRendered;
    private volatile boolean                 videoPlayerNotificationRequested;
    private          InlineCarouselCardState cardState;
    private          ProgressBar             loadingIndicator;

    private LinearLayout                contentLayout;
    private ImageView                   appIconImageView;
    private TextView                    appTitleTextView;
    private TextView                    appDescriptionTextView;
    private InlineCarouselCardMediaView mediaView;
    private ImageView                   starRatingImageView;
    private TextView                    downloadCTATextView;
    private Button                      downloadButton;

    public InlineCarouselCardView(Context context)
    {
        super( context );
    }

    public InlineCarouselCardView(Context context, AttributeSet attrs)
    {
        super( context, attrs );
    }

    public InlineCarouselCardView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super( context, attrs, defStyleAttr );
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public InlineCarouselCardView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes)
    {
        super( context, attrs, defStyleAttr, defStyleRes );
    }

    public void setUpView()
    {
        this.uiHandler = new Handler( Looper.getMainLooper() );

        final LayoutInflater inflater = (LayoutInflater) getContext().getSystemService( Context.LAYOUT_INFLATER_SERVICE );
        inflater.inflate( R.layout.applovin_card_view, this, true );

        bindViews();

        renderActivityIndicator();

        if ( sdk == null )
        {
            sdk = AppLovinSdk.getInstance( getContext() );
        }

        sdk.getNativeAdService().precacheResources( ad, this );
    }

    public AppLovinSdk getSdk()
    {
        return sdk;
    }

    public void setSdk(AppLovinSdk sdk)
    {
        this.sdk = sdk;
    }

    public AppLovinNativeAd getAd()
    {
        return ad;
    }

    public void setAd(AppLovinNativeAd ad)
    {
        this.ad = ad;
    }

    public InlineCarouselCardState getCardState()
    {
        return cardState;
    }

    public void setCardState(InlineCarouselCardState cardState)
    {
        this.cardState = cardState;
    }

    private void bindViews()
    {
        contentLayout = findViewById( R.id.applovin_card_content_layout );
        appIconImageView = findViewById( R.id.applovin_card_app_icon );
        appTitleTextView = findViewById( R.id.applovin_card_title );
        appDescriptionTextView = findViewById( R.id.applovin_card_app_description_text_view );
        mediaView = findViewById( R.id.applovin_card_video_ad_view );
        starRatingImageView = findViewById( R.id.applovin_card_star_rating );
        downloadCTATextView = findViewById( R.id.applovin_card_caption );
        downloadButton = findViewById( R.id.applovin_card_action_button );
    }

    public void renderCard()
    {
        if ( !slotRendered )
        {
            slotRendered = true;

            if ( loadingIndicator != null )
            {
                removeView( loadingIndicator );
                loadingIndicator = null;
            }

            mediaView.setAd( ad );
            mediaView.setCardState( cardState );
            mediaView.setSdk( sdk );
            mediaView.setUiHandler( uiHandler );
            mediaView.setUpView();

            appTitleTextView.setText( ad.getTitle() );
            appIconImageView.setOnClickListener( new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    ad.launchClickTarget( getContext() );
                }
            } );

            appDescriptionTextView.setText( ad.getDescriptionText() );

            final AppLovinTouchToClickListener clickBridge = new AppLovinTouchToClickListener( getContext(), new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    handleVideoClicked();
                }
            } );

            mediaView.setOnTouchListener( clickBridge );

            downloadCTATextView.setText( ad.getCaptionText() );
            downloadButton.setText( ad.getCtaText() );
            downloadButton.setOnClickListener( new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    ad.launchClickTarget( getContext() );
                }
            } );

            contentLayout.setOnClickListener( new OnClickListener()
            {
                @Override
                public void onClick(View v)
                {
                    ad.launchClickTarget( getContext() );
                }
            } );

            AppLovinSdkUtils.safePopulateImageView( appIconImageView, Uri.parse( ad.getIconUrl() ),
                                                    AppLovinSdkUtils.dpToPx( getContext(), AppLovinCarouselViewSettings.ICON_IMAGE_MAX_SCALE_SIZE ) );

            final Drawable starRatingDrawable = getStarRatingDrawable( ad.getStarRating() );
            starRatingImageView.setImageDrawable( starRatingDrawable );

            if ( videoPlayerNotificationRequested )
            {
                mediaView.onVideoPrecached( ad );
                videoPlayerNotificationRequested = false;
            }
        }
    }

    private Drawable getStarRatingDrawable(final float starRating)
    {
        final String sanitizedRating = Float.toString( starRating ).replace( ".", "_" );
        final String resourceName = "applovin_star_sprite_" + sanitizedRating;
        Log.d( "InlineCarouselCardView", "Looking up resource named: " + resourceName );
        final int drawableId = getContext().getResources().getIdentifier( resourceName, "drawable", getContext().getPackageName() );
        return getContext().getResources().getDrawable( drawableId );
    }

    private void renderActivityIndicator()
    {
        loadingIndicator = new ProgressBar( getContext() );
        loadingIndicator.setIndeterminate( true );
        loadingIndicator.setLayoutParams( LayoutUtils.createFrameParams( WRAP_CONTENT, WRAP_CONTENT, Gravity.CENTER ) );
        addView( loadingIndicator );
        bringChildToFront( loadingIndicator );
    }

    @Override
    public void onCardActivated()
    {
        if ( mediaView != null )
        {
            mediaView.onCardActivated();
        }

        cardState.setPreviouslyActivated( true );
        cardState.setCurrentlyActive( true );

        if ( !cardState.isImpressionTracked() )
        {
            cardState.setImpressionTracked( true );
            ad.trackImpression();
        }
    }

    @Override
    public void onCardDeactivated()
    {
        if ( mediaView != null )
        {
            mediaView.onCardDeactivated();
        }

        cardState.setCurrentlyActive( false );
    }

    public void handleVideoClicked()
    {
        if ( AppLovinCarouselViewSettings.TAP_TO_PAUSE_VIDEO )
        {
            mediaView.togglePlayback();
        }
        else
        {
            ad.launchClickTarget( getContext() );
        }
    }

    @Override
    public void onNativeAdImagesPrecached(AppLovinNativeAd slot)
    {
        uiHandler.post( new Runnable()
        {
            @Override
            public void run()
            {
                renderCard();
            }
        } );
    }

    //    @Overrides
    public void onNativeAdVideoPreceached(final AppLovinNativeAd slot)
    {
        if ( mediaView != null )
        {
            uiHandler.post( new Runnable()
            {
                @Override
                public void run()
                {
                    mediaView.onVideoPrecached( slot );
                }
            } );
        }
        else
        {
            videoPlayerNotificationRequested = true;
        }
    }

    @Override
    public void onNativeAdImagePrecachingFailed(AppLovinNativeAd ad, int errorCode)
    {

    }

    @Override
    public void onNativeAdVideoPrecachingFailed(AppLovinNativeAd ad, int errorCode)
    {

    }

    public void destroy()
    {
        if ( mediaView != null )
        {
            mediaView.destroy();
        }

        removeAllViews();
    }
}