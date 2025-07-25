package com.applovin.mediation.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;

import com.applovin.impl.sdk.utils.BundleUtils;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxAppOpenAdapter;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxNativeAdAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxAppOpenAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxNativeAdAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapters.googleadmanager.BuildConfig;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MediaContent;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.admanager.AdManagerAdRequest;
import com.google.android.gms.ads.admanager.AdManagerAdView;
import com.google.android.gms.ads.admanager.AdManagerInterstitialAd;
import com.google.android.gms.ads.admanager.AdManagerInterstitialAdLoadCallback;
import com.google.android.gms.ads.appopen.AppOpenAd;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAd.OnNativeAdLoadedListener;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

import java.lang.ref.WeakReference;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.applovin.sdk.AppLovinSdkUtils.runOnUiThread;

public class GoogleAdManagerMediationAdapter
        extends MediationAdapterBase
        implements MaxInterstitialAdapter, MaxAppOpenAdapter, MaxRewardedAdapter, MaxAdViewAdapter, MaxNativeAdAdapter
{
    private static final int TITLE_LABEL_TAG          = 1;
    private static final int MEDIA_VIEW_CONTAINER_TAG = 2;
    private static final int ICON_VIEW_TAG            = 3;
    private static final int BODY_VIEW_TAG            = 4;
    private static final int CALL_TO_ACTION_VIEW_TAG  = 5;
    private static final int ADVERTISER_VIEW_TAG      = 8;

    private static final String ADAPTIVE_BANNER_TYPE_INLINE = "inline";

    private static final AtomicBoolean initialized = new AtomicBoolean();

    private AdManagerInterstitialAd interstitialAd;
    private AppOpenAd               appOpenAd;
    private RewardedAd              rewardedAd;
    private AdManagerAdView         adView;
    private NativeAd                nativeAd;
    private NativeAdView            nativeAdView;

    private AppOpenAdListener  appOpenAdListener;
    private RewardedAdListener rewardedAdListener;

    // Explicit default constructor declaration
    public GoogleAdManagerMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    //region MaxAdapter methods

    @SuppressLint("MissingPermission")
    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener)
    {
        log( "Initializing Google Ad Manager SDK..." );

        if ( initialized.compareAndSet( false, true ) )
        {
            MobileAds.initialize( getContext( activity ) );
        }

        onCompletionListener.onCompletion( InitializationStatus.DOES_NOT_APPLY, null );
    }

    @Override
    public String getSdkVersion()
    {
        return String.valueOf( MobileAds.getVersion() );
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        log( "Destroy called for adapter " + this );

        if ( interstitialAd != null )
        {
            interstitialAd.setFullScreenContentCallback( null );
            interstitialAd = null;
        }

        if ( appOpenAd != null )
        {
            appOpenAd.setFullScreenContentCallback( null );
            appOpenAd = null;
            appOpenAdListener = null;
        }

        if ( rewardedAd != null )
        {
            rewardedAd.setFullScreenContentCallback( null );
            rewardedAd = null;
            rewardedAdListener = null;
        }

        if ( adView != null )
        {
            adView.destroy();
            adView = null;
        }

        if ( nativeAd != null )
        {
            nativeAd.destroy();
            nativeAd = null;
        }

        if ( nativeAdView != null )
        {
            nativeAdView.destroy();
            nativeAdView = null;
        }
    }

    //endregion

    //region MaxInterstitialAdapter Methods

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading interstitial ad: " + placementId + "..." );

        updateMuteState( parameters.getServerParameters() );

        Context context = getContext( activity );
        AdManagerAdRequest adRequest = createAdRequestWithParameters( parameters, context );

        AdManagerInterstitialAd.load( context, placementId, adRequest, new AdManagerInterstitialAdLoadCallback()
        {
            @Override
            public void onAdLoaded(@NonNull final AdManagerInterstitialAd ad)
            {
                log( "Interstitial ad loaded: " + placementId + "..." );

                interstitialAd = ad;
                interstitialAd.setFullScreenContentCallback( new InterstitialAdListener( placementId, listener ) );

                ResponseInfo responseInfo = interstitialAd.getResponseInfo();
                String responseId = ( responseInfo != null ) ? responseInfo.getResponseId() : null;
                if ( AppLovinSdkUtils.isValidString( responseId ) )
                {
                    Bundle extraInfo = new Bundle( 1 );
                    extraInfo.putString( "creative_id", responseId );

                    listener.onInterstitialAdLoaded( extraInfo );
                }
                else
                {
                    listener.onInterstitialAdLoaded();
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull final LoadAdError loadAdError)
            {
                MaxAdapterError adapterError = toMaxError( loadAdError );
                log( "Interstitial ad (" + placementId + ") failed to load with error: " + adapterError );
                listener.onInterstitialAdLoadFailed( adapterError );
            }
        } );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing interstitial ad: " + placementId + "..." );

        if ( interstitialAd != null )
        {
            // Tested that ad still successfully shows with a `null` Activity
            interstitialAd.show( activity );
        }
        else
        {
            log( "Interstitial ad failed to show: " + placementId );

            MaxAdapterError error = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                         MaxAdapterError.AD_NOT_READY.getCode(),
                                                         MaxAdapterError.AD_NOT_READY.getMessage() );
            listener.onInterstitialAdDisplayFailed( error );
        }
    }

    //endregion

    //region MaxAppOpenAdapter Methods

    // @Override
    public void loadAppOpenAd(@NonNull final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, @NonNull final MaxAppOpenAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading app open ad: " + placementId + "..." );

        updateMuteState( parameters.getServerParameters() );

        Context context = getContext( activity );
        AdManagerAdRequest adRequest = createAdRequestWithParameters( parameters, context );
        AppOpenAd.load( context, placementId, adRequest, new AppOpenAd.AppOpenAdLoadCallback()
        {
            @Override
            public void onAdLoaded(@NonNull final AppOpenAd ad)
            {
                log( "App open ad loaded: " + placementId + "..." );

                appOpenAd = ad;
                appOpenAdListener = new AppOpenAdListener( placementId, listener );
                ad.setFullScreenContentCallback( appOpenAdListener );

                ResponseInfo responseInfo = appOpenAd.getResponseInfo();
                String responseId = ( responseInfo != null ) ? responseInfo.getResponseId() : null;
                if ( AppLovinSdkUtils.isValidString( responseId ) )
                {
                    Bundle extraInfo = new Bundle( 1 );
                    extraInfo.putString( "creative_id", responseId );

                    listener.onAppOpenAdLoaded( extraInfo );
                }
                else
                {
                    listener.onAppOpenAdLoaded();
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull final LoadAdError loadAdError)
            {
                MaxAdapterError adapterError = toMaxError( loadAdError );
                log( "App open ad (" + placementId + ") failed to load with error: " + adapterError );
                listener.onAppOpenAdLoadFailed( adapterError );
            }
        } );
    }

    // @Override
    public void showAppOpenAd(@NonNull final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, @NonNull final MaxAppOpenAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing app open ad: " + placementId + "..." );

        // Shows ad with null activity properly as tested in SDK version 21.1.0
        if ( appOpenAd != null )
        {
            appOpenAd.show( activity );
        }
        else
        {
            log( "App open ad failed to show: " + placementId );

            MaxAdapterError error = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                         MaxAdapterError.AD_NOT_READY.getCode(),
                                                         MaxAdapterError.AD_NOT_READY.getMessage() );
            listener.onAppOpenAdDisplayFailed( error );
        }
    }

    //endregion

    //region MaxRewardedAdapter Methods

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading rewarded ad: " + placementId + "..." );

        updateMuteState( parameters.getServerParameters() );

        Context context = getContext( activity );
        AdManagerAdRequest adRequest = createAdRequestWithParameters( parameters, context );

        RewardedAd.load( context, placementId, adRequest, new RewardedAdLoadCallback()
        {
            @Override
            public void onAdLoaded(@NonNull final RewardedAd ad)
            {
                log( "Rewarded ad loaded: " + placementId + "..." );

                rewardedAd = ad;
                rewardedAdListener = new RewardedAdListener( placementId, listener );
                rewardedAd.setFullScreenContentCallback( rewardedAdListener );

                ResponseInfo responseInfo = rewardedAd.getResponseInfo();
                String responseId = ( responseInfo != null ) ? responseInfo.getResponseId() : null;
                if ( AppLovinSdkUtils.isValidString( responseId ) )
                {
                    Bundle extraInfo = new Bundle( 1 );
                    extraInfo.putString( "creative_id", responseId );

                    listener.onRewardedAdLoaded( extraInfo );
                }
                else
                {
                    listener.onRewardedAdLoaded();
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull final LoadAdError loadAdError)
            {
                MaxAdapterError adapterError = toMaxError( loadAdError );
                log( "Rewarded ad (" + placementId + ") failed to load with error: " + adapterError );
                listener.onRewardedAdLoadFailed( adapterError );
            }
        } );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing rewarded ad: " + placementId + "..." );

        if ( rewardedAd != null )
        {
            configureReward( parameters );

            // Tested that ad still successfully shows with a `null` Activity
            rewardedAd.show( activity, rewardItem -> {
                log( "Rewarded ad user earned reward: " + placementId );
                rewardedAdListener.hasGrantedReward = true;
            } );
        }
        else
        {
            log( "Rewarded ad failed to show: " + placementId );

            MaxAdapterError error = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                         MaxAdapterError.AD_NOT_READY.getCode(),
                                                         MaxAdapterError.AD_NOT_READY.getMessage() );
            listener.onRewardedAdDisplayFailed( error );
        }
    }

    //endregion

    //region MaxAdViewAdapter Methods

    @SuppressLint("MissingPermission")
    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, @Nullable final Activity activity, final MaxAdViewAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        boolean isNative = parameters.getServerParameters().getBoolean( "is_native" );
        log( "Loading " + ( isNative ? "native " : "" ) + adFormat.getLabel() + " ad for placement id: " + placementId + "..." );

        Context context = getContext( activity );
        AdManagerAdRequest adRequest = createAdRequestWithParameters( parameters, context );

        if ( isNative )
        {
            NativeAdOptions.Builder optionsBuilder = new NativeAdOptions.Builder();
            optionsBuilder.setAdChoicesPlacement( getAdChoicesPlacement( parameters ) );
            optionsBuilder.setRequestMultipleImages( adFormat == MaxAdFormat.MREC ); // MRECs can handle multiple images via AdMob's media view

            // NOTE: Activity context needed on older SDKs
            NativeAdViewListener nativeAdViewListener = new NativeAdViewListener( parameters, adFormat, activity, listener );
            AdLoader adLoader = new AdLoader.Builder( context, placementId )
                    .withNativeAdOptions( optionsBuilder.build() )
                    .forNativeAd( nativeAdViewListener )
                    .withAdListener( nativeAdViewListener )
                    .build();

            adLoader.loadAd( adRequest );
        }
        else
        {
            adView = new AdManagerAdView( context );
            adView.setAdUnitId( placementId );
            adView.setAdListener( new AdViewListener( placementId, adFormat, listener ) );

            // Check if adaptive banner sizes should be used
            final boolean isAdaptiveBanner = parameters.getServerParameters().getBoolean( "adaptive_banner", false );
            adView.setAdSize( toAdSize( adFormat, isAdaptiveBanner, parameters, context ) );

            adView.loadAd( adRequest );
        }
    }

    //endregion

    //region MaxNativeAdAdapter Methods

    @Override
    @SuppressLint("MissingPermission")
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading native ad for placement id: " + placementId + "..." );

        Context context = getContext( activity );
        AdRequest adRequest = createAdRequestWithParameters( parameters, context );

        NativeAdOptions.Builder nativeAdOptionsBuilder = new NativeAdOptions.Builder();
        nativeAdOptionsBuilder.setAdChoicesPlacement( getAdChoicesPlacement( parameters ) );

        // Medium templates can handle multiple images via AdMob's media view
        String template = BundleUtils.getString( "template", "", parameters.getServerParameters() );
        nativeAdOptionsBuilder.setRequestMultipleImages( template.contains( "medium" ) );

        NativeAdListener nativeAdListener = new NativeAdListener( parameters, context, listener );
        AdLoader adLoader = new AdLoader.Builder( context, placementId )
                .withNativeAdOptions( nativeAdOptionsBuilder.build() )
                .forNativeAd( nativeAdListener )
                .withAdListener( nativeAdListener )
                .build();

        adLoader.loadAd( adRequest );
    }

    //endregion

    //region Helper Methods

    private static MaxAdapterError toMaxError(final AdError googleAdManagerError)
    {
        int googleAdManagerErrorCode = googleAdManagerError.getCode();
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( googleAdManagerErrorCode )
        {
            case AdRequest.ERROR_CODE_NO_FILL:
            case AdRequest.ERROR_CODE_MEDIATION_NO_FILL:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case AdRequest.ERROR_CODE_NETWORK_ERROR:
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case AdRequest.ERROR_CODE_INTERNAL_ERROR:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case AdRequest.ERROR_CODE_INVALID_REQUEST:
            case AdRequest.ERROR_CODE_REQUEST_ID_MISMATCH:
                adapterError = MaxAdapterError.BAD_REQUEST;
                break;
            case AdRequest.ERROR_CODE_APP_ID_MISSING:
            case AdRequest.ERROR_CODE_INVALID_AD_STRING:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
        }

        return new MaxAdapterError( adapterError,
                                    googleAdManagerErrorCode,
                                    googleAdManagerError.getMessage() );
    }

    private AdSize toAdSize(final MaxAdFormat adFormat,
                            final boolean isAdaptiveBanner,
                            final MaxAdapterParameters parameters,
                            final Context context)
    {
        if ( isAdaptiveBanner && isAdaptiveAdFormat( adFormat, parameters ) )
        {
            return getAdaptiveAdSize( parameters, context );
        }

        if ( adFormat == MaxAdFormat.BANNER )
        {
            return AdSize.BANNER;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return AdSize.LEADERBOARD;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return AdSize.MEDIUM_RECTANGLE;
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported ad format: " + adFormat );
        }
    }

    private AdSize getAdaptiveAdSize(final MaxAdapterParameters parameters, final Context context)
    {
        final int bannerWidth = getAdaptiveBannerWidth( parameters, context );

        if ( isInlineAdaptiveBanner( parameters ) )
        {
            final int inlineMaxHeight = getInlineAdaptiveBannerMaxHeight( parameters );
            if ( inlineMaxHeight > 0 )
            {
                return AdSize.getInlineAdaptiveBannerAdSize( bannerWidth, inlineMaxHeight );
            }

            return AdSize.getCurrentOrientationInlineAdaptiveBannerAdSize( context, bannerWidth );
        }

        // Return anchored size by default
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize( context, bannerWidth );
    }

    private boolean isAdaptiveAdFormat(final MaxAdFormat adFormat, final MaxAdapterParameters parameters)
    {
        // Adaptive banners must be inline for MRECs
        final boolean isInlineAdaptiveMRec = ( adFormat == MaxAdFormat.MREC ) && isInlineAdaptiveBanner( parameters );
        return isInlineAdaptiveMRec || adFormat == MaxAdFormat.BANNER || adFormat == MaxAdFormat.LEADER;
    }

    private boolean isInlineAdaptiveBanner(final MaxAdapterParameters parameters)
    {
        final Map<String, Object> localExtraParameters = parameters.getLocalExtraParameters();
        final Object adaptiveBannerType = localExtraParameters.get( "adaptive_banner_type" );
        return ( adaptiveBannerType instanceof String ) && ADAPTIVE_BANNER_TYPE_INLINE.equalsIgnoreCase( (String) adaptiveBannerType );
    }

    private int getInlineAdaptiveBannerMaxHeight(final MaxAdapterParameters parameters)
    {
        final Map<String, Object> localExtraParameters = parameters.getLocalExtraParameters();
        final Object inlineMaxHeight = localExtraParameters.get( "inline_adaptive_banner_max_height" );
        return ( inlineMaxHeight instanceof Integer ) ? (int) inlineMaxHeight : 0;
    }

    private int getAdaptiveBannerWidth(final MaxAdapterParameters parameters, final Context context)
    {
        final Map<String, Object> localExtraParameters = parameters.getLocalExtraParameters();
        Object widthObj = localExtraParameters.get( "adaptive_banner_width" );
        if ( widthObj instanceof Integer )
        {
            return (int) widthObj;
        }
        else if ( widthObj != null )
        {
            e( "Expected parameter \"adaptive_banner_width\" to be of type Integer, received: " + widthObj.getClass() );
        }

        int deviceWidthPx = getApplicationWindowWidth( context );
        return AppLovinSdkUtils.pxToDp( context, deviceWidthPx );
    }

    public static int getApplicationWindowWidth(final Context context)
    {
        WindowManager windowManager = (WindowManager) context.getSystemService( Context.WINDOW_SERVICE );
        Display display = windowManager.getDefaultDisplay();
        DisplayMetrics outMetrics = new DisplayMetrics();
        display.getMetrics( outMetrics );
        return outMetrics.widthPixels;
    }

    private AdManagerAdRequest createAdRequestWithParameters(final MaxAdapterParameters parameters, final Context context)
    {
        AdManagerAdRequest.Builder requestBuilder = new AdManagerAdRequest.Builder();

        Bundle serverParameters = parameters.getServerParameters();
        requestBuilder.setRequestAgent( mediationTag() );

        Bundle networkExtras = new Bundle();

        // Use event id as AdMob's placement request id
        String eventId = BundleUtils.getString( "event_id", serverParameters );
        if ( AppLovinSdkUtils.isValidString( eventId ) )
        {
            networkExtras.putString( "placement_req_id", eventId );
        }

        Boolean hasUserConsent = parameters.hasUserConsent();
        if ( hasUserConsent != null && !hasUserConsent )
        {
            networkExtras.putString( "npa", "1" ); // Non-personalized ads
        }

        Boolean isDoNotSell = parameters.isDoNotSell();
        if ( isDoNotSell != null && isDoNotSell )
        {
            networkExtras.putInt( "rdp", 1 ); // Restrict data processing - https://developers.google.com/admob/android/ccpa

            PreferenceManager.getDefaultSharedPreferences( context )
                    .edit()
                    .putInt( "gad_rdp", 1 )
                    .apply();
        }
        else
        {
            PreferenceManager.getDefaultSharedPreferences( context )
                    .edit()
                    .remove( "gad_rdp" )
                    .apply();
        }

        Map<String, Object> localExtraParameters = parameters.getLocalExtraParameters();

        Object maxContentRating = localExtraParameters.get( "google_max_ad_content_rating" );
        if ( maxContentRating instanceof String )
        {
            networkExtras.putString( "max_ad_content_rating", (String) maxContentRating );
        }

        Object contentUrlString = localExtraParameters.get( "google_content_url" );
        if ( contentUrlString instanceof String )
        {
            requestBuilder.setContentUrl( (String) contentUrlString );
        }

        Object neighbouringContentUrlStringsObject = localExtraParameters.get( "google_neighbouring_content_url_strings" );
        if ( neighbouringContentUrlStringsObject instanceof List )
        {
            // try-catching unsafe cast to List<String> in case an incorrect list type is set.
            try
            {
                requestBuilder.setNeighboringContentUrls( (List<String>) neighbouringContentUrlStringsObject );
            }
            catch ( Throwable th )
            {
                e( "Neighbouring content URL strings extra param needs to be of type List<String>.", th );
            }
        }

        Object publisherProvidedId = localExtraParameters.get( "ppid" );
        if ( publisherProvidedId instanceof String )
        {
            requestBuilder.setPublisherProvidedId( (String) publisherProvidedId );
        }

        Object customTargetingDataObject = localExtraParameters.get( "custom_targeting" );
        if ( customTargetingDataObject instanceof Map )
        {
            // try-catching unsafe cast to List<String> or Map<String, Object> in case an incorrect type is set.
            try
            {
                Map<String, Object> customTargetingDataMap = (Map<String, Object>) customTargetingDataObject;
                for ( String key : customTargetingDataMap.keySet() )
                {
                    Object value = customTargetingDataMap.get( key );
                    if ( value instanceof String )
                    {
                        requestBuilder.addCustomTargeting( key, (String) value );
                    }
                    else if ( value instanceof List )
                    {
                        requestBuilder.addCustomTargeting( key, (List<String>) value );
                    }
                    else
                    {
                        e( "Object in the map needs to be either of type String or List<String>." );
                    }
                }
            }
            catch ( Throwable th )
            {
                e( "Custom targeting extra param value needs to be of type Map<String, Object>.", th );
            }
        }

        requestBuilder.addNetworkExtrasBundle( AdMobAdapter.class, networkExtras );

        return requestBuilder.build();
    }

    /**
     * Update the global mute state for Play Services Ads - must be done _before_ ad load to restrict inventory which requires playing with volume.
     */
    private static void updateMuteState(final Bundle serverParameters)
    {
        if ( serverParameters.containsKey( "is_muted" ) )
        {
            MobileAds.setAppMuted( serverParameters.getBoolean( "is_muted" ) );
        }
    }

    private int getAdChoicesPlacement(MaxAdapterResponseParameters parameters)
    {
        // Publishers can set via nativeAdLoader.setLocalExtraParameter( "gam_ad_choices_placement", ADCHOICES_BOTTOM_LEFT );
        final Map<String, Object> localExtraParams = parameters.getLocalExtraParameters();
        final Object adChoicesPlacementObj = localExtraParams != null ? localExtraParams.get( "gam_ad_choices_placement" ) : null;

        return isValidAdChoicesPlacement( adChoicesPlacementObj ) ? (Integer) adChoicesPlacementObj : NativeAdOptions.ADCHOICES_TOP_RIGHT;
    }

    private boolean isValidAdChoicesPlacement(Object placementObj)
    {
        return ( placementObj instanceof Integer ) &&
                ( (Integer) placementObj == NativeAdOptions.ADCHOICES_TOP_LEFT ||
                        (Integer) placementObj == NativeAdOptions.ADCHOICES_TOP_RIGHT ||
                        (Integer) placementObj == NativeAdOptions.ADCHOICES_BOTTOM_LEFT ||
                        (Integer) placementObj == NativeAdOptions.ADCHOICES_BOTTOM_RIGHT );
    }

    private Context getContext(@Nullable final Activity activity)
    {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
    }

    //endregion

    private class InterstitialAdListener
            extends FullScreenContentCallback
    {
        private final String                         placementId;
        private final MaxInterstitialAdapterListener listener;

        InterstitialAdListener(final String placementId, final MaxInterstitialAdapterListener listener)
        {
            this.placementId = placementId;
            this.listener = listener;
        }

        @Override
        public void onAdShowedFullScreenContent()
        {
            log( "Interstitial ad shown: " + placementId );
        }

        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull final AdError adError)
        {
            MaxAdapterError adapterError = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                adError.getCode(),
                                                                adError.getMessage() );
            log( "Interstitial ad (" + placementId + ") failed to show with error: " + adapterError );
            listener.onInterstitialAdDisplayFailed( adapterError );
        }

        @Override
        public void onAdImpression()
        {
            log( "Interstitial ad impression recorded: " + placementId );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdClicked()
        {
            log( "Interstitial ad clicked: " + placementId );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onAdDismissedFullScreenContent()
        {
            log( "Interstitial ad hidden: " + placementId );
            listener.onInterstitialAdHidden();
        }
    }

    private class AppOpenAdListener
            extends FullScreenContentCallback
    {
        private final String                    placementId;
        private final MaxAppOpenAdapterListener listener;

        AppOpenAdListener(final String placementId, final MaxAppOpenAdapterListener listener)
        {
            this.placementId = placementId;
            this.listener = listener;
        }

        @Override
        public void onAdShowedFullScreenContent()
        {
            log( "App open ad shown: " + placementId );
        }

        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull final AdError adError)
        {
            MaxAdapterError adapterError = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                adError.getCode(),
                                                                adError.getMessage() );
            log( "App open ad (" + placementId + ") failed to show with error: " + adapterError );
            listener.onAppOpenAdDisplayFailed( adapterError );
        }

        @Override
        public void onAdImpression()
        {
            log( "App open ad impression recorded: " + placementId );
            listener.onAppOpenAdDisplayed();
        }

        @Override
        public void onAdClicked()
        {
            log( "App open ad clicked: " + placementId );
            listener.onAppOpenAdClicked();
        }

        @Override
        public void onAdDismissedFullScreenContent()
        {
            log( "App open ad hidden: " + placementId );
            listener.onAppOpenAdHidden();
        }
    }

    private class RewardedAdListener
            extends FullScreenContentCallback
    {
        private final String                     placementId;
        private final MaxRewardedAdapterListener listener;

        private boolean hasGrantedReward;

        RewardedAdListener(final String placementId, final MaxRewardedAdapterListener listener)
        {
            this.placementId = placementId;
            this.listener = listener;
        }

        @Override
        public void onAdShowedFullScreenContent()
        {
            log( "Rewarded ad shown: " + placementId );
        }

        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull final AdError adError)
        {
            MaxAdapterError adapterError = new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                adError.getCode(),
                                                                adError.getMessage() );
            log( "Rewarded ad (" + placementId + ") failed to show with error: " + adapterError );
            listener.onRewardedAdDisplayFailed( adapterError );
        }

        @Override
        public void onAdImpression()
        {
            log( "Rewarded ad impression recorded: " + placementId );
            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onAdClicked()
        {
            log( "Rewarded ad clicked: " + placementId );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onAdDismissedFullScreenContent()
        {
            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            log( "Rewarded ad hidden: " + placementId );
            listener.onRewardedAdHidden();
        }
    }

    private class AdViewListener
            extends AdListener
    {
        final String                   placementId;
        final MaxAdFormat              adFormat;
        final MaxAdViewAdapterListener listener;

        AdViewListener(final String placementId, final MaxAdFormat adFormat, final MaxAdViewAdapterListener listener)
        {
            this.placementId = placementId;
            this.adFormat = adFormat;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded()
        {
            log( adFormat.getLabel() + " ad loaded: " + placementId );

            Bundle extraInfo = new Bundle( 3 );

            ResponseInfo responseInfo = adView.getResponseInfo();
            String responseId = ( responseInfo != null ) ? responseInfo.getResponseId() : null;
            if ( AppLovinSdkUtils.isValidString( responseId ) )
            {
                extraInfo.putString( "creative_id", responseId );
            }

            AdSize adSize = adView.getAdSize();
            if ( adSize != null )
            {
                extraInfo.putInt( "ad_width", adSize.getWidth() );
                extraInfo.putInt( "ad_height", adSize.getHeight() );
            }

            listener.onAdViewAdLoaded( adView, extraInfo );
        }

        @Override
        public void onAdFailedToLoad(@NonNull final LoadAdError loadAdError)
        {
            MaxAdapterError adapterError = toMaxError( loadAdError );
            log( adFormat.getLabel() + " ad (" + placementId + ") failed to load with error code: " + adapterError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onAdImpression()
        {
            log( adFormat.getLabel() + " ad shown: " + placementId );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdOpened()
        {
            // Do not track ad view ad opened events (besides clicks) on Android, but do so on iOS
            log( adFormat.getLabel() + " ad opened" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdClosed()
        {
            // Do not track ad view ad opened events (besides clicks) on Android, but do so on iOS
            log( adFormat.getLabel() + " ad closed" );
        }
    }

    private class NativeAdViewListener
            extends AdListener
            implements OnNativeAdLoadedListener
    {
        final String                   placementId;
        final MaxAdFormat              adFormat;
        final Bundle                   serverParameters;
        final WeakReference<Activity>  activityRef;
        final MaxAdViewAdapterListener listener;

        NativeAdViewListener(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, @Nullable final Activity activity, final MaxAdViewAdapterListener listener)
        {
            placementId = parameters.getThirdPartyAdPlacementId();
            serverParameters = parameters.getServerParameters();
            activityRef = new WeakReference<>( activity );

            this.adFormat = adFormat;
            this.listener = listener;
        }

        @Override
        public void onNativeAdLoaded(@NonNull final com.google.android.gms.ads.nativead.NativeAd nativeAd)
        {
            log( "Native " + adFormat.getLabel() + " ad loaded: " + placementId );

            GoogleAdManagerMediationAdapter.this.nativeAd = nativeAd;

            final Activity activity = activityRef.get();
            final Context context = getContext( activity );

            final MediaView mediaView = new MediaView( context );
            MediaContent mediaContent = nativeAd.getMediaContent();
            if ( mediaContent != null )
            {
                mediaView.setMediaContent( mediaContent );
            }

            final NativeAd.Image icon = nativeAd.getIcon();
            MaxNativeAd.MaxNativeAdImage maxNativeAdImage = null;
            if ( icon != null )
            {
                if ( icon.getDrawable() != null )
                {
                    maxNativeAdImage = new MaxNativeAd.MaxNativeAdImage( icon.getDrawable() );
                }
                else
                {
                    maxNativeAdImage = new MaxNativeAd.MaxNativeAdImage( icon.getUri() );
                }
            }

            final MaxNativeAd maxNativeAd = new MaxNativeAd.Builder()
                    .setAdFormat( adFormat )
                    .setTitle( nativeAd.getHeadline() )
                    .setBody( nativeAd.getBody() )
                    .setCallToAction( nativeAd.getCallToAction() )
                    .setIcon( maxNativeAdImage )
                    .setMediaView( mediaView )
                    .build();

            runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    final String templateName = BundleUtils.getString( "template", "", serverParameters );
                    MaxNativeAdView maxNativeAdView = new MaxNativeAdView( maxNativeAd, templateName, context );

                    nativeAdView = new NativeAdView( context );
                    nativeAdView.setIconView( maxNativeAdView.getIconImageView() );
                    nativeAdView.setHeadlineView( maxNativeAdView.getTitleTextView() );
                    nativeAdView.setBodyView( maxNativeAdView.getBodyTextView() );
                    nativeAdView.setMediaView( mediaView );
                    nativeAdView.setCallToActionView( maxNativeAdView.getCallToActionButton() );
                    nativeAdView.setNativeAd( nativeAd );

                    nativeAdView.addView( maxNativeAdView );

                    ResponseInfo responseInfo = nativeAd.getResponseInfo();
                    String responseId = ( responseInfo != null ) ? responseInfo.getResponseId() : null;
                    if ( AppLovinSdkUtils.isValidString( responseId ) )
                    {
                        Bundle extraInfo = new Bundle( 1 );
                        extraInfo.putString( "creative_id", responseId );

                        listener.onAdViewAdLoaded( nativeAdView, extraInfo );
                    }
                    else
                    {
                        listener.onAdViewAdLoaded( nativeAdView );
                    }
                }
            } );
        }

        @Override
        public void onAdFailedToLoad(@NonNull final LoadAdError loadAdError)
        {
            MaxAdapterError adapterError = toMaxError( loadAdError );
            log( "Native " + adFormat.getLabel() + " ad (" + placementId + ") failed to load with error: " + adapterError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onAdImpression()
        {
            log( "Native " + adFormat.getLabel() + " ad shown" );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdClicked()
        {
            log( "Native " + adFormat.getLabel() + " ad clicked" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdOpened()
        {
            log( "Native " + adFormat.getLabel() + " ad opened" );
            listener.onAdViewAdExpanded();
        }

        @Override
        public void onAdClosed()
        {
            log( "Native " + adFormat.getLabel() + " ad closed" );
            listener.onAdViewAdCollapsed();
        }
    }

    private class NativeAdListener
            extends AdListener
            implements OnNativeAdLoadedListener
    {
        final String                     placementId;
        final Bundle                     serverParameters;
        final Context                    context;
        final MaxNativeAdAdapterListener listener;

        public NativeAdListener(final MaxAdapterResponseParameters parameters, final Context context, final MaxNativeAdAdapterListener listener)
        {
            placementId = parameters.getThirdPartyAdPlacementId();
            serverParameters = parameters.getServerParameters();

            this.context = context;
            this.listener = listener;
        }

        @Override
        public void onNativeAdLoaded(@NonNull final NativeAd nativeAd)
        {
            log( "Native ad loaded: " + placementId );

            GoogleAdManagerMediationAdapter.this.nativeAd = nativeAd;

            String templateName = BundleUtils.getString( "template", "", serverParameters );
            final boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );
            if ( isTemplateAd && TextUtils.isEmpty( nativeAd.getHeadline() ) )
            {
                e( "Native ad (" + nativeAd + ") does not have required assets." );
                listener.onNativeAdLoadFailed( MaxAdapterError.MISSING_REQUIRED_NATIVE_AD_ASSETS );

                return;
            }

            runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    View mediaView = null;
                    MediaContent mediaContent = nativeAd.getMediaContent();
                    List<NativeAd.Image> images = nativeAd.getImages();
                    Drawable mainImage = null;
                    float mediaContentAspectRatio = 0.0f;

                    if ( mediaContent != null )
                    {
                        MediaView googleMediaView = new MediaView( context );
                        googleMediaView.setMediaContent( mediaContent );
                        mediaView = googleMediaView;

                        mainImage = mediaContent.getMainImage();
                        mediaContentAspectRatio = mediaContent.getAspectRatio();
                    }
                    else if ( images != null && images.size() > 0 )
                    {
                        NativeAd.Image mediaImage = images.get( 0 );
                        ImageView mediaImageView = new ImageView( context );
                        Drawable mediaImageDrawable = mediaImage.getDrawable();

                        if ( mediaImageDrawable != null )
                        {
                            mediaImageView.setImageDrawable( mediaImage.getDrawable() );
                            mediaView = mediaImageView;

                            mediaContentAspectRatio = (float) mediaImageDrawable.getIntrinsicWidth() / (float) mediaImageDrawable.getIntrinsicHeight();
                        }
                    }

                    NativeAd.Image icon = nativeAd.getIcon();
                    MaxNativeAd.MaxNativeAdImage iconImage = null;
                    if ( icon != null )
                    {
                        if ( icon.getDrawable() != null )
                        {
                            iconImage = new MaxNativeAd.MaxNativeAdImage( icon.getDrawable() );
                        }
                        else
                        {
                            iconImage = new MaxNativeAd.MaxNativeAdImage( icon.getUri() );
                        }
                    }

                    MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                            .setAdFormat( MaxAdFormat.NATIVE )
                            .setTitle( nativeAd.getHeadline() )
                            .setAdvertiser( nativeAd.getAdvertiser() )
                            .setBody( nativeAd.getBody() )
                            .setCallToAction( nativeAd.getCallToAction() )
                            .setIcon( iconImage )
                            .setMediaView( mediaView )
                            .setMainImage( new MaxNativeAd.MaxNativeAdImage( mainImage ) )
                            .setMediaContentAspectRatio( mediaContentAspectRatio )
                            .setStarRating( nativeAd.getStarRating() );

                    MaxNativeAd maxNativeAd = new MaxGoogleAdManagerNativeAd( builder );

                    ResponseInfo responseInfo = nativeAd.getResponseInfo();
                    String responseId = ( responseInfo != null ) ? responseInfo.getResponseId() : null;
                    Bundle extraInfo = new Bundle( 1 );
                    extraInfo.putString( "creative_id", responseId );

                    listener.onNativeAdLoaded( maxNativeAd, extraInfo );
                }
            } );
        }

        @Override
        public void onAdFailedToLoad(@NonNull final LoadAdError loadAdError)
        {
            MaxAdapterError adapterError = toMaxError( loadAdError );
            log( "Native ad (" + placementId + ") failed to load with error: " + adapterError );
            listener.onNativeAdLoadFailed( adapterError );
        }

        @Override
        public void onAdImpression()
        {
            log( "Native ad shown" );
            listener.onNativeAdDisplayed( null );
        }

        @Override
        public void onAdClicked()
        {
            log( "Native ad clicked" );
            listener.onNativeAdClicked();
        }

        @Override
        public void onAdOpened()
        {
            log( "Native ad opened" );
        }

        @Override
        public void onAdClosed()
        {
            log( "Native ad closed" );
        }
    }

    private class MaxGoogleAdManagerNativeAd
            extends MaxNativeAd
    {
        public MaxGoogleAdManagerNativeAd(final Builder builder) { super( builder ); }

        @Override
        public boolean prepareForInteraction(final List<View> clickableViews, final ViewGroup container)
        {
            final NativeAd nativeAd = GoogleAdManagerMediationAdapter.this.nativeAd;
            if ( nativeAd == null )
            {
                e( "Failed to register native ad views: native ad is null." );
                return false;
            }

            nativeAdView = new NativeAdView( container.getContext() );

            // Native integrations
            if ( container instanceof MaxNativeAdView )
            {
                MaxNativeAdView maxNativeAdView = (MaxNativeAdView) container;

                // The Google Native Ad View needs to be wrapped around the main native ad view.
                View mainView = maxNativeAdView.getMainView();
                maxNativeAdView.removeView( mainView );
                nativeAdView.addView( mainView );
                maxNativeAdView.addView( nativeAdView );

                nativeAdView.setIconView( maxNativeAdView.getIconImageView() );
                nativeAdView.setHeadlineView( maxNativeAdView.getTitleTextView() );
                nativeAdView.setAdvertiserView( maxNativeAdView.getAdvertiserTextView() );
                nativeAdView.setBodyView( maxNativeAdView.getBodyTextView() );
                nativeAdView.setCallToActionView( maxNativeAdView.getCallToActionButton() );

                View mediaView = getMediaView();
                if ( mediaView instanceof MediaView )
                {
                    nativeAdView.setMediaView( (MediaView) mediaView );
                }
                else if ( mediaView instanceof ImageView )
                {
                    nativeAdView.setImageView( mediaView );
                }

                nativeAdView.setNativeAd( nativeAd );
            }
            // Plugins
            else
            {
                View mediaView = null;

                for ( View view : clickableViews )
                {
                    Object viewTag = view.getTag();
                    if ( viewTag == null ) continue;

                    int tag = (int) viewTag;

                    if ( tag == TITLE_LABEL_TAG )
                    {
                        nativeAdView.setHeadlineView( view );
                    }
                    else if ( tag == ICON_VIEW_TAG )
                    {
                        nativeAdView.setIconView( view );
                    }
                    else if ( tag == BODY_VIEW_TAG )
                    {
                        nativeAdView.setBodyView( view );
                    }
                    else if ( tag == CALL_TO_ACTION_VIEW_TAG )
                    {
                        nativeAdView.setCallToActionView( view );
                    }
                    else if ( tag == ADVERTISER_VIEW_TAG )
                    {
                        nativeAdView.setAdvertiserView( view );
                    }
                    else if ( tag == MEDIA_VIEW_CONTAINER_TAG )
                    {
                        mediaView = getMediaView();
                    }
                }

                //
                // Logic required for proper media view rendering in plugins (e.g. Flutter / React Native)
                //

                ViewGroup pluginContainer = ( mediaView != null ) ? (ViewGroup) mediaView.getParent() : container;
                if ( pluginContainer == null ) return true;

                // NOTE: Will be false for React Native (will extend `ReactViewGroup`), but true for Flutter
                boolean hasPluginLayout = ( pluginContainer instanceof RelativeLayout || pluginContainer instanceof FrameLayout );

                // Handle re-parenting of mediaView for enabling clicks on other asset views
                if ( mediaView != null )
                {
                    // Remove mediaView from the plugin container
                    pluginContainer.removeView( mediaView );

                    // Special handling for Google MediaView on React Native
                    if ( !hasPluginLayout && mediaView instanceof MediaView )
                    {
                        MediaView googleMediaView = (MediaView) mediaView;
                        MediaContent googleMediaContent = googleMediaView.getMediaContent();
                        if ( googleMediaContent != null && googleMediaContent.hasVideoContent() )
                        {
                            mediaView = new AutoMeasuringMediaView( pluginContainer.getContext() );
                            googleMediaView.setMediaContent( nativeAd.getMediaContent() );
                        }
                    }

                    // Add mediaView to the NativeAdView
                    ViewGroup.LayoutParams mediaViewLayout = new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT );
                    nativeAdView.addView( mediaView, mediaViewLayout );

                    // Set mediaView or imageView based on the instance type
                    if ( mediaView instanceof MediaView )
                    {
                        nativeAdView.setMediaView( (MediaView) mediaView );
                    }
                    else if ( mediaView instanceof ImageView )
                    {
                        nativeAdView.setImageView( (ImageView) mediaView );
                    }
                }
                // Insert a placeholder view for enabling clicks on other asset views
                else
                {
                    ViewGroup.LayoutParams layoutParams = new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT );
                    View view = new View( pluginContainer.getContext() );

                    nativeAdView.addView( view, layoutParams );
                    nativeAdView.setStoreView( view );
                }

                nativeAdView.setNativeAd( nativeAd );

                // Add the NativeAdView back to the plugin container

                if ( hasPluginLayout )
                {
                    ViewGroup.LayoutParams nativeAdViewLayout = new ViewGroup.LayoutParams( ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT );
                    pluginContainer.addView( nativeAdView, nativeAdViewLayout );
                }
                else
                {
                    nativeAdView.measure(
                            View.MeasureSpec.makeMeasureSpec( pluginContainer.getWidth(), View.MeasureSpec.EXACTLY ),
                            View.MeasureSpec.makeMeasureSpec( pluginContainer.getHeight(), View.MeasureSpec.EXACTLY ) );
                    nativeAdView.layout( 0, 0, pluginContainer.getWidth(), pluginContainer.getHeight() );
                    pluginContainer.addView( nativeAdView );
                }
            }

            return true;
        }
    }

    private static class AutoMeasuringMediaView
            extends MediaView
    {
        AutoMeasuringMediaView(final Context context) { super( context ); }

        @Override
        protected void onAttachedToWindow()
        {
            super.onAttachedToWindow();
            requestLayout();
        }

        @Override
        public void requestLayout()
        {
            super.requestLayout();
            post( () -> {
                measure(
                        MeasureSpec.makeMeasureSpec( getWidth(), MeasureSpec.EXACTLY ),
                        MeasureSpec.makeMeasureSpec( getHeight(), MeasureSpec.EXACTLY ) );
                layout( getLeft(), getTop(), getRight(), getBottom() );
            } );
        }
    }
}
