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
import android.view.WindowManager;
import android.widget.ImageView;

import com.applovin.impl.sdk.utils.BundleUtils;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.MaxRewardedInterstitialAdapter;
import com.applovin.mediation.adapter.MaxSignalProvider;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxNativeAdAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.adapters.google.BuildConfig;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkUtils;
import com.google.ads.mediation.admob.AdMobAdapter;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdFormat;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdLoader;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdSize;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MediaContent;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.OnUserEarnedRewardListener;
import com.google.android.gms.ads.RequestConfiguration;
import com.google.android.gms.ads.ResponseInfo;
import com.google.android.gms.ads.initialization.AdapterStatus;
import com.google.android.gms.ads.initialization.OnInitializationCompleteListener;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;
import com.google.android.gms.ads.nativead.MediaView;
import com.google.android.gms.ads.nativead.NativeAd;
import com.google.android.gms.ads.nativead.NativeAd.OnNativeAdLoadedListener;
import com.google.android.gms.ads.nativead.NativeAdOptions;
import com.google.android.gms.ads.nativead.NativeAdView;
import com.google.android.gms.ads.query.QueryInfo;
import com.google.android.gms.ads.query.QueryInfoGenerationCallback;
import com.google.android.gms.ads.rewarded.RewardItem;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAd;
import com.google.android.gms.ads.rewardedinterstitial.RewardedInterstitialAdLoadCallback;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.applovin.sdk.AppLovinSdkUtils.runOnUiThread;

/**
 * This is a mediation adapterWrapper for Google Play Services
 * <p>
 * Created by basil on 12/7/16.
 */
public class GoogleMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter /* MaxNativeAdAdapter */
{
    private static final AtomicBoolean        initialized = new AtomicBoolean();
    private static       InitializationStatus status;

    private InterstitialAd         interstitialAd;
    private RewardedInterstitialAd rewardedInterstitialAd;
    private RewardedAd             rewardedAd;
    private AdView                 adView;
    private NativeAd               nativeAd;
    private NativeAdView           nativeAdView;

    private RewardedInterstitialAdListener rewardedInterstitialAdListener;
    private RewardedAdListener             rewardedAdListener;

    // Explicit default constructor declaration
    public GoogleMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    //region MaxAdapter Methods

    @SuppressLint("MissingPermission")
    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        log( "Initializing Google SDK..." );

        if ( initialized.compareAndSet( false, true ) )
        {
            Context context = getContext( activity );

            // Prevent AdMob SDK from auto-initing its adapters in AB testing environments.
            MobileAds.disableMediationAdapterInitialization( context );

            if ( parameters.getServerParameters().getBoolean( "init_without_callback", false ) )
            {
                status = InitializationStatus.DOES_NOT_APPLY;

                MobileAds.initialize( context );

                onCompletionListener.onCompletion( status, null );
            }
            else
            {
                status = InitializationStatus.INITIALIZING;

                MobileAds.initialize( context, new OnInitializationCompleteListener()
                {
                    @Override
                    public void onInitializationComplete(@NonNull final com.google.android.gms.ads.initialization.InitializationStatus initializationStatus)
                    {
                        final AdapterStatus googleAdsStatus = initializationStatus.getAdapterStatusMap().get( "com.google.android.gms.ads.MobileAds" );
                        final AdapterStatus.State googleAdsState = googleAdsStatus != null ? googleAdsStatus.getInitializationState() : null;
                        log( "Initialization complete with status " + googleAdsState );

                        // NOTE: We were able to load ads even when SDK is in "not ready" init state...
                        // AdMob SDK when status "not ready": "The mediation adapter is LESS likely to fill ad requests."
                        status = ( AdapterStatus.State.READY == googleAdsState ) ? InitializationStatus.INITIALIZED_SUCCESS : InitializationStatus.INITIALIZED_UNKNOWN;
                        onCompletionListener.onCompletion( status, null );
                    }
                } );
            }
        }
        else
        {
            onCompletionListener.onCompletion( status, null );
        }
    }

    @Override
    public String getSdkVersion()
    {
        return status != null ? String.valueOf( MobileAds.getVersion() ) :
                getAdapterVersion().substring( 0, getAdapterVersion().lastIndexOf( '.' ) );
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

        if ( rewardedInterstitialAd != null )
        {
            rewardedInterstitialAd.setFullScreenContentCallback( null );
            rewardedInterstitialAd = null;
            rewardedInterstitialAdListener = null;
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

    //region MaxSignalProvider Methods

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        setRequestConfiguration( parameters );

        Context context = getContext( activity );

        AdRequest adRequest = createAdRequestWithParameters( true, parameters.getAdFormat(), parameters, context );

        QueryInfo.generate( context, toAdFormat( parameters ), adRequest, new QueryInfoGenerationCallback()
        {
            @Override
            public void onSuccess(@NonNull final QueryInfo queryInfo)
            {
                log( "Signal collection successful" );
                callback.onSignalCollected( queryInfo.getQuery() );
            }

            @Override
            public void onFailure(@NonNull final String errorMessage)
            {
                log( "Signal collection failed with error: " + errorMessage );
                callback.onSignalCollectionFailed( errorMessage );
            }
        } );
    }

    //endregion

    //region MaxInterstitialAdapter Methods

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( parameters.getBidResponse() );
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + "interstitial ad: " + placementId + "..." );

        updateMuteState( parameters );
        setRequestConfiguration( parameters );
        AdRequest adRequest = createAdRequestWithParameters( isBiddingAd, MaxAdFormat.INTERSTITIAL, parameters, activity );

        InterstitialAd.load( activity, placementId, adRequest, new InterstitialAdLoadCallback()
        {
            @Override
            public void onAdLoaded(@NonNull final InterstitialAd ad)
            {
                log( "Interstitial ad loaded: " + placementId + "..." );

                interstitialAd = ad;
                interstitialAd.setFullScreenContentCallback( new InterstitialAdListener( placementId, listener ) );

                ResponseInfo responseInfo = interstitialAd.getResponseInfo();
                String responseId = ( responseInfo != null ) ? responseInfo.getResponseId() : null;
                if ( AppLovinSdk.VERSION_CODE >= 9150000 && AppLovinSdkUtils.isValidString( responseId ) )
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
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing interstitial ad: " + placementId + "..." );

        if ( interstitialAd != null )
        {
            interstitialAd.show( activity );
        }
        else
        {
            log( "Interstitial ad failed to show: " + placementId );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed" ) );
        }
    }

    //endregion

    //region MaxRewardedInterstitialAdapter Methods

    @Override
    public void loadRewardedInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedInterstitialAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( parameters.getBidResponse() );
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + "rewarded interstitial ad: " + placementId + "..." );

        updateMuteState( parameters );
        setRequestConfiguration( parameters );
        AdRequest adRequest = createAdRequestWithParameters( isBiddingAd, MaxAdFormat.REWARDED_INTERSTITIAL, parameters, activity );

        RewardedInterstitialAd.load( activity, placementId, adRequest, new RewardedInterstitialAdLoadCallback()
        {
            @Override
            public void onAdLoaded(@NonNull final RewardedInterstitialAd ad)
            {
                log( "Rewarded interstitial ad loaded: " + placementId );

                rewardedInterstitialAd = ad;
                rewardedInterstitialAdListener = new RewardedInterstitialAdListener( placementId, listener );
                rewardedInterstitialAd.setFullScreenContentCallback( rewardedInterstitialAdListener );

                ResponseInfo responseInfo = rewardedInterstitialAd.getResponseInfo();
                String responseId = ( responseInfo != null ) ? responseInfo.getResponseId() : null;
                if ( AppLovinSdk.VERSION_CODE > 9150000 && AppLovinSdkUtils.isValidString( responseId ) )
                {
                    Bundle extraInfo = new Bundle( 1 );
                    extraInfo.putString( "creative_id", responseId );

                    listener.onRewardedInterstitialAdLoaded( extraInfo );
                }
                else
                {
                    listener.onRewardedInterstitialAdLoaded();
                }
            }

            @Override
            public void onAdFailedToLoad(@NonNull final LoadAdError loadAdError)
            {
                MaxAdapterError adapterError = toMaxError( loadAdError );
                log( "Rewarded interstitial ad (" + placementId + ") failed to load with error: " + adapterError );
                listener.onRewardedInterstitialAdLoadFailed( adapterError );
            }
        } );
    }

    @Override
    public void showRewardedInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedInterstitialAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing rewarded interstitial ad: " + placementId + "..." );

        if ( rewardedInterstitialAd != null )
        {
            configureReward( parameters );
            rewardedInterstitialAd.show( activity, new OnUserEarnedRewardListener()
            {
                @Override
                public void onUserEarnedReward(@NonNull final RewardItem rewardItem)
                {
                    log( "Rewarded interstitial ad user earned reward: " + placementId );
                    rewardedInterstitialAdListener.hasGrantedReward = true;
                }
            } );
        }
        else
        {
            log( "Rewarded interstitial ad failed to show: " + placementId );
            listener.onRewardedInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed" ) );
        }
    }

    //endregion

    //region MaxRewardedAdapter Methods

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( parameters.getBidResponse() );
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + "rewarded ad: " + placementId + "..." );

        updateMuteState( parameters );
        setRequestConfiguration( parameters );
        AdRequest adRequest = createAdRequestWithParameters( isBiddingAd, MaxAdFormat.REWARDED, parameters, activity );

        RewardedAd.load( activity, placementId, adRequest, new RewardedAdLoadCallback()
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
                if ( AppLovinSdk.VERSION_CODE >= 9150000 && AppLovinSdkUtils.isValidString( responseId ) )
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
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing rewarded ad: " + placementId + "..." );

        if ( rewardedAd != null )
        {
            configureReward( parameters );

            rewardedAd.show( activity, new OnUserEarnedRewardListener()
            {
                @Override
                public void onUserEarnedReward(@NonNull final RewardItem rewardItem)
                {
                    log( "Rewarded ad user earned reward: " + placementId );
                    rewardedAdListener.hasGrantedReward = true;
                }
            } );
        }
        else
        {
            log( "Rewarded ad failed to show: " + placementId );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed" ) );
        }
    }

    //endregion

    //region MaxAdViewAdapter Methods

    @SuppressLint("MissingPermission")
    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( parameters.getBidResponse() );
        boolean isNative = parameters.getServerParameters().getBoolean( "is_native" );
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + ( isNative ? "native " : "" ) + adFormat.getLabel() + " ad for placement id: " + placementId + "..." );

        setRequestConfiguration( parameters );

        Context context = getContext( activity );
        AdRequest adRequest = createAdRequestWithParameters( isBiddingAd, adFormat, parameters, context );

        if ( isNative )
        {
            NativeAdOptions.Builder nativeAdOptionsBuilder = new NativeAdOptions.Builder();
            nativeAdOptionsBuilder.setAdChoicesPlacement( getAdChoicesPlacement( parameters ) );
            nativeAdOptionsBuilder.setRequestMultipleImages( adFormat == MaxAdFormat.MREC ); // MRECs can handle multiple images via AdMob's media view

            // NOTE: Activity context needed on older SDKs
            NativeAdViewListener nativeAdViewListener = new NativeAdViewListener( parameters, adFormat, activity, listener );
            AdLoader adLoader = new AdLoader.Builder( context, placementId )
                    .withNativeAdOptions( nativeAdOptionsBuilder.build() )
                    .forNativeAd( nativeAdViewListener )
                    .withAdListener( nativeAdViewListener )
                    .build();

            adLoader.loadAd( adRequest );
        }
        else
        {
            adView = new AdView( context );
            adView.setAdUnitId( placementId );
            adView.setAdListener( new AdViewListener( placementId, adFormat, listener ) );

            // Check if adaptive banner sizes should be used
            boolean isAdaptiveBanner = parameters.getServerParameters().getBoolean( "adaptive_banner", false );
            adView.setAdSize( toAdSize( adFormat, isAdaptiveBanner, context ) );

            adView.loadAd( adRequest );
        }
    }

    //endregion

    //region MaxNativeAdAdapter Methods

    @Override
    @SuppressLint("MissingPermission")
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        boolean isBiddingAd = AppLovinSdkUtils.isValidString( parameters.getBidResponse() );
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + " native ad for placement id: " + placementId + "..." );

        setRequestConfiguration( parameters );

        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        Context applicationContext = ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
        AdRequest adRequest = createAdRequestWithParameters( isBiddingAd, MaxAdFormat.NATIVE, parameters, applicationContext );

        NativeAdOptions.Builder nativeAdOptionsBuilder = new NativeAdOptions.Builder();
        nativeAdOptionsBuilder.setAdChoicesPlacement( getAdChoicesPlacement( parameters ) );

        // Medium templates can handle multiple images via AdMob's media view
        String template = BundleUtils.getString( "template", "", parameters.getServerParameters() );
        nativeAdOptionsBuilder.setRequestMultipleImages( template.contains( "medium" ) );

        NativeAdListener nativeAdListener = new NativeAdListener( parameters, applicationContext, listener );
        AdLoader adLoader = new AdLoader.Builder( applicationContext, placementId )
                .withNativeAdOptions( nativeAdOptionsBuilder.build() )
                .forNativeAd( nativeAdListener )
                .withAdListener( nativeAdListener )
                .build();

        adLoader.loadAd( adRequest );
    }

    //endregion

    //region Helper Methods

    private static MaxAdapterError toMaxError(final AdError googleAdsError)
    {
        int googleErrorCode = googleAdsError.getCode();
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( googleErrorCode )
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

        return new MaxAdapterError( adapterError.getErrorCode(),
                                    adapterError.getErrorMessage(),
                                    googleErrorCode,
                                    googleAdsError.getMessage() );
    }

    private AdSize toAdSize(final MaxAdFormat adFormat, boolean isAdaptiveBanner, final Context context)
    {
        if ( adFormat == MaxAdFormat.BANNER || adFormat == MaxAdFormat.LEADER )
        {
            if ( isAdaptiveBanner )
            {
                WindowManager windowManager = (WindowManager) context.getSystemService( Context.WINDOW_SERVICE );
                Display display = windowManager.getDefaultDisplay();
                DisplayMetrics outMetrics = new DisplayMetrics();
                display.getMetrics( outMetrics );
                int screenWidthDp = AppLovinSdkUtils.pxToDp( context, outMetrics.widthPixels );

                return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize( context, screenWidthDp );
            }
            else
            {
                return adFormat == MaxAdFormat.BANNER ? AdSize.BANNER : AdSize.LEADERBOARD;
            }
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

    private AdFormat toAdFormat(final MaxAdapterSignalCollectionParameters parameters)
    {
        MaxAdFormat adFormat = parameters.getAdFormat();
        boolean isNative = parameters.getServerParameters().getBoolean( "is_native" ) || adFormat == MaxAdFormat.NATIVE;
        if ( isNative )
        {
            return AdFormat.NATIVE;
        }
        else if ( adFormat.isAdViewAd() )
        {
            return AdFormat.BANNER;
        }
        else if ( adFormat == MaxAdFormat.INTERSTITIAL )
        {
            return AdFormat.INTERSTITIAL;
        }
        else if ( adFormat == MaxAdFormat.REWARDED )
        {
            return AdFormat.REWARDED;
        }
        else if ( adFormat == MaxAdFormat.REWARDED_INTERSTITIAL )
        {
            return AdFormat.REWARDED_INTERSTITIAL;
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported ad format: " + adFormat );
        }
    }

    private void setRequestConfiguration(final MaxAdapterParameters parameters)
    {
        RequestConfiguration.Builder requestConfigurationBuilder = MobileAds.getRequestConfiguration().toBuilder();

        Boolean isAgeRestrictedUser = getPrivacySetting( "isAgeRestrictedUser", parameters );
        if ( isAgeRestrictedUser != null )
        {
            int ageRestrictedUserTag = isAgeRestrictedUser ? RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_TRUE : RequestConfiguration.TAG_FOR_CHILD_DIRECTED_TREATMENT_FALSE;
            requestConfigurationBuilder.setTagForChildDirectedTreatment( ageRestrictedUserTag );
        }

        Bundle serverParameters = parameters.getServerParameters();
        String testDevicesString = serverParameters.getString( "test_device_ids", null );
        if ( !TextUtils.isEmpty( testDevicesString ) )
        {
            String[] testDeviceList = testDevicesString.split( "," );
            requestConfigurationBuilder.setTestDeviceIds( Arrays.asList( testDeviceList ) );
        }

        MobileAds.setRequestConfiguration( requestConfigurationBuilder.build() );
    }

    private AdRequest createAdRequestWithParameters(final boolean isBiddingAd, final MaxAdFormat adFormat, final MaxAdapterParameters parameters, final Context context)
    {
        AdRequest.Builder requestBuilder = new AdRequest.Builder();
        Bundle networkExtras = new Bundle( 6 );

        Bundle serverParameters = parameters.getServerParameters();
        if ( isBiddingAd )
        {
            // MAX specific
            networkExtras.putString( "query_info_type", "requester_type_2" );

            if ( AppLovinSdk.VERSION_CODE >= 11_00_00_00 && adFormat.isAdViewAd() )
            {
                Object isAdaptiveBanner = parameters.getLocalExtraParameters().get( "adaptive_banner" );
                if ( isAdaptiveBanner instanceof String && "true".equalsIgnoreCase( (String) isAdaptiveBanner ) )
                {
                    AdSize adaptiveAdSize = toAdSize( adFormat, true, context );
                    networkExtras.putInt( "adaptive_banner_w", adaptiveAdSize.getWidth() );
                    networkExtras.putInt( "adaptive_banner_h", adaptiveAdSize.getHeight() );
                }
            }

            if ( parameters instanceof MaxAdapterResponseParameters )
            {
                String bidResponse = ( (MaxAdapterResponseParameters) parameters ).getBidResponse();
                if ( AppLovinSdkUtils.isValidString( bidResponse ) )
                {
                    requestBuilder.setAdString( bidResponse );
                }
            }
        }

        if ( serverParameters.getBoolean( "set_mediation_identifier", true ) )
        {
            // MAX specific
            requestBuilder.setRequestAgent( "applovin" );
        }

        // Use event id as AdMob's placement request id
        String eventId = BundleUtils.getString( "event_id", serverParameters );
        if ( AppLovinSdkUtils.isValidString( eventId ) )
        {
            networkExtras.putString( "placement_req_id", eventId );
        }

        if ( getWrappingSdk().getConfiguration().getConsentDialogState() == AppLovinSdkConfiguration.ConsentDialogState.APPLIES )
        {
            Boolean hasUserConsent = getPrivacySetting( "hasUserConsent", parameters );
            if ( hasUserConsent != null && !hasUserConsent )
            {
                networkExtras.putString( "npa", "1" ); // Non-personalized ads
            }
        }

        if ( AppLovinSdk.VERSION_CODE >= 91100 ) // Pre-beta versioning (9.14.0)
        {
            Boolean isDoNotSell = getPrivacySetting( "isDoNotSell", parameters );
            if ( isDoNotSell != null && isDoNotSell )
            {
                networkExtras.putInt( "rdp", 1 ); // Restrict data processing - https://developers.google.com/admob/android/ccpa

                PreferenceManager.getDefaultSharedPreferences( context )
                        .edit()
                        .putInt( "gad_rdp", 1 )
                        .commit();
            }
        }

        if ( AppLovinSdk.VERSION_CODE >= 11_00_00_00 )
        {
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
        }

        requestBuilder.addNetworkExtrasBundle( AdMobAdapter.class, networkExtras );

        return requestBuilder.build();
    }

    private Boolean getPrivacySetting(final String privacySetting, final MaxAdapterParameters parameters)
    {
        try
        {
            // Use reflection because compiled adapters have trouble fetching `boolean` from old SDKs and `Boolean` from new SDKs (above 9.14.0)
            Class<?> parametersClass = parameters.getClass();
            Method privacyMethod = parametersClass.getMethod( privacySetting );
            return (Boolean) privacyMethod.invoke( parameters );
        }
        catch ( Exception exception )
        {
            log( "Error getting privacy setting " + privacySetting + " with exception: ", exception );
            return ( AppLovinSdk.VERSION_CODE >= 9140000 ) ? null : false;
        }
    }

    /**
     * Update the global mute state for AdMob - must be done _before_ ad load to restrict inventory which requires playing with volume.
     */
    private static void updateMuteState(final MaxAdapterResponseParameters parameters)
    {
        Bundle serverParameters = parameters.getServerParameters();
        // Overwritten by `mute_state` setting, unless `mute_state` is disabled
        if ( serverParameters.containsKey( "is_muted" ) ) // Introduced in 9.10.0
        {
            MobileAds.setAppMuted( serverParameters.getBoolean( "is_muted" ) );
        }
    }

    private int getAdChoicesPlacement(MaxAdapterResponseParameters parameters)
    {
        // Publishers can set via nativeAdLoader.setLocalExtraParameter( "admob_ad_choices_placement", ADCHOICES_BOTTOM_LEFT );
        // Note: This feature requires AppLovin v11.0.0+
        if ( AppLovinSdk.VERSION_CODE >= 11_00_00_00 )
        {
            final Map<String, Object> localExtraParams = parameters.getLocalExtraParameters();
            final Object adChoicesPlacementObj = localExtraParams != null ? localExtraParams.get( "admob_ad_choices_placement" ) : null;

            return isValidAdChoicesPlacement( adChoicesPlacementObj ) ? (Integer) adChoicesPlacementObj : NativeAdOptions.ADCHOICES_TOP_RIGHT;
        }

        return NativeAdOptions.ADCHOICES_TOP_RIGHT;
    }

    private boolean isValidAdChoicesPlacement(Object placementObj)
    {
        return ( placementObj instanceof Integer ) &&
                ( (Integer) placementObj == NativeAdOptions.ADCHOICES_TOP_LEFT ||
                        (Integer) placementObj == NativeAdOptions.ADCHOICES_TOP_RIGHT ||
                        (Integer) placementObj == NativeAdOptions.ADCHOICES_BOTTOM_LEFT ||
                        (Integer) placementObj == NativeAdOptions.ADCHOICES_BOTTOM_RIGHT );
    }

    private Context getContext(@Nullable Activity activity)
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
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull final AdError adError)
        {
            MaxAdapterError adapterError = new MaxAdapterError( -4205, "Ad Display Failed", adError.getCode(), adError.getMessage() );
            log( "Interstitial ad (" + placementId + ") failed to show with error: " + adapterError );
            listener.onInterstitialAdDisplayFailed( adapterError );
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

    private class RewardedInterstitialAdListener
            extends FullScreenContentCallback
    {
        private final String                                 placementId;
        private final MaxRewardedInterstitialAdapterListener listener;

        private boolean hasGrantedReward;

        private RewardedInterstitialAdListener(final String placementId, final MaxRewardedInterstitialAdapterListener listener)
        {
            this.placementId = placementId;
            this.listener = listener;
        }

        @Override
        public void onAdShowedFullScreenContent()
        {
            log( "Rewarded interstitial ad shown: " + placementId );

            listener.onRewardedInterstitialAdDisplayed();
            listener.onRewardedInterstitialAdVideoStarted();
        }

        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull final AdError adError)
        {
            MaxAdapterError adapterError = new MaxAdapterError( -4205, "Ad Display Failed", adError.getCode(), adError.getMessage() );
            log( "Rewarded interstitial ad (" + placementId + ") failed to show with error: " + adapterError );
            listener.onRewardedInterstitialAdDisplayFailed( adapterError );
        }

        @Override
        public void onAdClicked()
        {
            log( "Rewarded interstitial ad clicked: " + placementId );
            listener.onRewardedInterstitialAdClicked();
        }

        @Override
        public void onAdDismissedFullScreenContent()
        {
            listener.onRewardedInterstitialAdVideoCompleted();

            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                MaxReward reward = getReward();
                log( "Rewarded interstitial ad rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            log( "Rewarded interstitial ad hidden: " + placementId );
            listener.onRewardedInterstitialAdHidden();
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

            listener.onRewardedAdDisplayed();
            listener.onRewardedAdVideoStarted();
        }

        @Override
        public void onAdFailedToShowFullScreenContent(@NonNull final AdError adError)
        {
            MaxAdapterError adapterError = new MaxAdapterError( -4205, "Ad Display Failed", adError.getCode(), adError.getMessage() );
            log( "Rewarded ad (" + placementId + ") failed to show with error: " + adapterError );
            listener.onRewardedAdDisplayFailed( adapterError );
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
            listener.onRewardedAdVideoCompleted();

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

            if ( AppLovinSdk.VERSION_CODE >= 9150000 )
            {
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
            else
            {
                listener.onAdViewAdLoaded( adView );
            }
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
            // NOTE: Do not track ad view ad closed events on Android, but do so on iOS
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

        NativeAdViewListener(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
        {
            placementId = parameters.getThirdPartyAdPlacementId();
            serverParameters = parameters.getServerParameters();
            activityRef = new WeakReference<>( activity );

            this.adFormat = adFormat;
            this.listener = listener;
        }

        @Override
        public void onNativeAdLoaded(@NonNull final NativeAd nativeAd)
        {
            log( "Native " + adFormat.getLabel() + " ad loaded: " + placementId );

            if ( TextUtils.isEmpty( nativeAd.getHeadline() ) )
            {
                log( "Native " + adFormat.getLabel() + " ad failed to load: Google native ad is missing one or more required assets" );
                listener.onAdViewAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );

                nativeAd.destroy();

                return;
            }

            GoogleMediationAdapter.this.nativeAd = nativeAd;

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
                    .setIcon( maxNativeAdImage )
                    .setTitle( nativeAd.getHeadline() )
                    .setBody( nativeAd.getBody() )
                    .setMediaView( mediaView )
                    .setCallToAction( nativeAd.getCallToAction() )
                    .build();

            final String templateName = BundleUtils.getString( "template", "", serverParameters );
            if ( templateName.contains( "vertical" ) && AppLovinSdk.VERSION_CODE < 9140500 )
            {
                log( "Vertical native banners are only supported on MAX SDK 9.14.5 and above. Default horizontal native template will be used." );
            }

            runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    MaxNativeAdView maxNativeAdView;
                    if ( AppLovinSdk.VERSION_CODE < 9140000 )
                    {
                        log( "Native ads with media views are only supported on MAX SDK version 9.14.0 and above. Default native template will be used." );
                        maxNativeAdView = new MaxNativeAdView( maxNativeAd, activity );
                    }
                    else
                    {
                        if ( AppLovinSdk.VERSION_CODE >= 11010000 )
                        {
                            maxNativeAdView = new MaxNativeAdView( maxNativeAd, templateName, context );
                        }
                        else
                        {
                            maxNativeAdView = new MaxNativeAdView( maxNativeAd, templateName, activity );
                        }
                    }

                    nativeAdView = new NativeAdView( context );
                    nativeAdView.setIconView( maxNativeAdView.getIconContentView() );
                    nativeAdView.setHeadlineView( maxNativeAdView.getTitleTextView() );
                    nativeAdView.setBodyView( maxNativeAdView.getBodyTextView() );
                    nativeAdView.setMediaView( mediaView );
                    nativeAdView.setCallToActionView( maxNativeAdView.getCallToActionButton() );
                    nativeAdView.setNativeAd( nativeAd );

                    nativeAdView.addView( maxNativeAdView );

                    ResponseInfo responseInfo = nativeAd.getResponseInfo();
                    String responseId = ( responseInfo != null ) ? responseInfo.getResponseId() : null;
                    if ( AppLovinSdk.VERSION_CODE >= 9150000 && AppLovinSdkUtils.isValidString( responseId ) )
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

            GoogleMediationAdapter.this.nativeAd = nativeAd;

            String templateName = BundleUtils.getString( "template", "", serverParameters );
            final boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );
            if ( isTemplateAd && TextUtils.isEmpty( nativeAd.getHeadline() ) )
            {
                e( "Native ad (" + nativeAd + ") does not have required assets." );
                listener.onNativeAdLoadFailed( new MaxAdapterError( -5400, "Missing Native Ad Assets" ) );

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
                            mediaImageView.setImageDrawable( mediaImageDrawable );
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
                            .setIcon( iconImage )
                            .setTitle( nativeAd.getHeadline() )
                            .setAdvertiser( nativeAd.getAdvertiser() )
                            .setBody( nativeAd.getBody() )
                            .setMediaView( mediaView )
                            .setCallToAction( nativeAd.getCallToAction() );

                    if ( AppLovinSdk.VERSION_CODE >= 11_04_03_99 )
                    {
                        builder.setMainImage( new MaxNativeAd.MaxNativeAdImage( mainImage ) );
                    }

                    if ( AppLovinSdk.VERSION_CODE >= 11_04_00_00 )
                    {
                        builder.setMediaContentAspectRatio( mediaContentAspectRatio );
                    }

                    MaxNativeAd maxNativeAd = new MaxGoogleNativeAd( builder );

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

    private class MaxGoogleNativeAd
            extends MaxNativeAd
    {
        public MaxGoogleNativeAd(final Builder builder)
        {
            super( builder );
        }

        @Override
        public void prepareViewForInteraction(final MaxNativeAdView maxNativeAdView)
        {
            if ( nativeAd == null )
            {
                e( "Failed to register native ad views: native ad is null." );
                return;
            }

            nativeAdView = new NativeAdView( maxNativeAdView.getContext() );

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
    }
}
