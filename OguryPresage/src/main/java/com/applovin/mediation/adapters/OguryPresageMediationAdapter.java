package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;

import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.MaxSignalProvider;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.adapters.ogurypresage.BuildConfig;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.ogury.cm.OguryChoiceManagerExternal;
import com.ogury.core.OguryError;
import com.ogury.ed.OguryAdFormatErrorCode;
import com.ogury.ed.OguryAdImpressionListener;
import com.ogury.ed.OguryBannerAdListener;
import com.ogury.ed.OguryBannerAdSize;
import com.ogury.ed.OguryBannerAdView;
import com.ogury.ed.OguryInterstitialAd;
import com.ogury.ed.OguryInterstitialAdListener;
import com.ogury.ed.OguryOptinVideoAd;
import com.ogury.ed.OguryOptinVideoAdListener;
import com.ogury.ed.OguryReward;
import com.ogury.sdk.Ogury;
import com.ogury.sdk.OguryConfiguration;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import io.presage.common.token.OguryTokenProvider;

/**
 * This is an AppLovin Mediation Adapter for Ogury Presage SDK.
 * <p>
 * Created by santoshbagadi on 11/22/19.
 */
public class OguryPresageMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter
{
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    private OguryInterstitialAd interstitialAd;
    private OguryOptinVideoAd   rewardedAd;
    private OguryBannerAdView   adView;

    // State to track if ad load finished. Unfortunately, Ogury's SDK's onAdError(...) callback is invoked on ad load and ad display errors (including ad expiration)
    private boolean isFinishedLoading;

    // Explicit default constructor declaration
    public OguryPresageMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    //region MaxAdapter methods
    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( INITIALIZED.compareAndSet( false, true ) )
        {
            final String assetKey = parameters.getServerParameters().getString( "asset_key" );
            log( "Initializing Ogury Presage SDK with asset key: " + assetKey + "..." );

            // Pass the user consent before initializing SDK for personalized ads
            updateUserConsent( parameters );

            OguryConfiguration.Builder oguryConfigurationBuilder = new OguryConfiguration.Builder( getContext( activity ), assetKey )
                    .putMonitoringInfo( "max_applovin_mediation_version", AppLovinSdk.VERSION );

            Ogury.start( oguryConfigurationBuilder.build() );
        }

        onCompletionListener.onCompletion( InitializationStatus.DOES_NOT_APPLY, null );
    }

    @Override
    public String getSdkVersion()
    {
        return Ogury.getSdkVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        interstitialAd = null;

        rewardedAd = null;

        if ( adView != null )
        {
            adView.destroy();
            adView = null;
        }
    }
    //endregion

    //region Signal Collection
    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        updateUserConsent( parameters );

        final String bidderToken = OguryTokenProvider.getBidderToken( getContext( activity ) );
        callback.onSignalCollected( bidderToken );
    }
    //endregion

    //region MaxInterstitialAdapter methods
    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + "interstitial ad: " + placementId + "..." );

        interstitialAd = new OguryInterstitialAd( getContext( activity ), placementId );

        InterstitialAdListener adListener = new InterstitialAdListener( placementId, listener );
        interstitialAd.setListener( adListener );
        interstitialAd.setAdImpressionListener( adListener );

        // Update user consent before loading
        updateUserConsent( parameters );

        if ( interstitialAd.isLoaded() )
        {
            log( "Ad is available already" );
            listener.onInterstitialAdLoaded();
        }
        else
        {
            if ( AppLovinSdkUtils.isValidString( bidResponse ) )
            {
                interstitialAd.setAdMarkup( bidResponse );
            }

            interstitialAd.load();
        }
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing interstitial ad: " + placementId + "..." );

        if ( interstitialAd.isLoaded() )
        {
            isFinishedLoading = true;
            interstitialAd.show();
        }
        else
        {
            log( "Interstitial ad not ready" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Interstitial ad not ready" ) );
        }
    }
    //endregion

    //region MaxRewardedAdapter methods
    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + "rewarded ad: " + placementId + "..." );

        rewardedAd = new OguryOptinVideoAd( getContext( activity ), placementId );

        RewardedAdListener adListener = new RewardedAdListener( placementId, listener );
        rewardedAd.setListener( adListener );
        rewardedAd.setAdImpressionListener( adListener );

        // Update user consent before loading
        updateUserConsent( parameters );

        if ( rewardedAd.isLoaded() )
        {
            log( "Ad is available already" );
            listener.onRewardedAdLoaded();
        }
        else
        {
            if ( AppLovinSdkUtils.isValidString( bidResponse ) )
            {
                rewardedAd.setAdMarkup( bidResponse );
            }

            rewardedAd.load();
        }
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing rewarded ad: " + placementId + "..." );

        if ( rewardedAd.isLoaded() )
        {
            // Configure userReward from server
            configureReward( parameters );

            isFinishedLoading = true;
            rewardedAd.show();
        }
        else
        {
            log( "Rewarded ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Rewarded ad not ready" ) );
        }
    }
    //endregion

    //region MaxAdViewAdapter methods
    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        log( "Loading " + ( AppLovinSdkUtils.isValidString( bidResponse ) ? "bidding " : "" ) + adFormat.getLabel() + " ad: " + placementId + "..." );

        adView = new OguryBannerAdView( getContext( activity ) );
        adView.setAdSize( toAdSize( adFormat ) );
        adView.setAdUnit( placementId );

        AdViewListener adListener = new AdViewListener( placementId, listener );
        adView.setListener( adListener );
        adView.setAdImpressionListener( adListener );

        // Update user consent before loading
        updateUserConsent( parameters );

        if ( AppLovinSdkUtils.isValidString( bidResponse ) )
        {
            adView.setAdMarkup( bidResponse );
        }

        adView.loadAd();
    }
    //endregion

    //region Helper Methods
    private void updateUserConsent(final MaxAdapterParameters parameters)
    {
        if ( AppLovinSdk.VERSION_CODE >= 11_04_03_99 )
        {
            if ( parameters.getConsentString() != null )
            {
                OguryChoiceManagerExternal.TcfV2.setConsent( parameters.getConsentString(), new Integer[0] );
            }
        }

        Boolean hasUserConsent = getPrivacySetting( "hasUserConsent", parameters );
        if ( hasUserConsent != null )
        {
            OguryChoiceManagerExternal.setConsent( hasUserConsent, "CUSTOM" );
        }
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

    private static MaxAdapterError toMaxError(OguryError oguryError)
    {
        final int oguryErrorCode = oguryError.getErrorCode();
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( oguryErrorCode )
        {
            case OguryAdFormatErrorCode.NO_INTERNET_CONNECTION: // 0
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case OguryAdFormatErrorCode.LOAD_FAILED: // 2000
                // We are not sure what kind of load error it is - may be misconfigured ad unit id, et al...
                adapterError = MaxAdapterError.UNSPECIFIED;
                break;
            case OguryAdFormatErrorCode.AD_DISABLED: // 2001
            case OguryAdFormatErrorCode.PROFIG_NOT_SYNCED: // 2002
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case OguryAdFormatErrorCode.AD_EXPIRED: // 2003
                adapterError = MaxAdapterError.AD_EXPIRED;
                break;
            case OguryAdFormatErrorCode.SDK_INIT_NOT_CALLED: // 2004
            case OguryAdFormatErrorCode.SDK_INIT_FAILED: // 2006
                adapterError = MaxAdapterError.NOT_INITIALIZED;
                break;
            case OguryAdFormatErrorCode.ANOTHER_AD_ALREADY_DISPLAYED: // 2005
                adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                break;
            case OguryAdFormatErrorCode.ACTIVITY_IN_BACKGROUND: // 2007
            case OguryAdFormatErrorCode.SHOW_FAILED: // 2010
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case OguryAdFormatErrorCode.AD_NOT_AVAILABLE: // 2008
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case OguryAdFormatErrorCode.AD_NOT_LOADED: // 2009
                adapterError = MaxAdapterError.AD_NOT_READY;
                break;
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), oguryErrorCode, oguryError.getMessage() );
    }

    private OguryBannerAdSize toAdSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER || adFormat == MaxAdFormat.LEADER )
        {
            return OguryBannerAdSize.SMALL_BANNER_320x50;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return OguryBannerAdSize.MPU_300x250;
        }
        else
        {
            throw new IllegalArgumentException( "Invalid ad format: " + adFormat );
        }
    }

    private Context getContext(Activity activity)
    {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
    }
    //endregion

    private class InterstitialAdListener
            implements OguryInterstitialAdListener, OguryAdImpressionListener
    {
        private final String                         placementId;
        private final MaxInterstitialAdapterListener listener;

        InterstitialAdListener(final String placementId, final MaxInterstitialAdapterListener listener)
        {
            this.placementId = placementId;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded()
        {
            log( "Interstitial loaded: " + placementId );
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onAdDisplayed()
        {
            log( "Interstitial shown: " + placementId );
        }

        @Override
        public void onAdImpression()
        {
            log( "Interstitial triggered impression: " + placementId );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdClicked()
        {
            log( "Interstitial clicked: " + placementId );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onAdClosed()
        {
            log( "Interstitial hidden: " + placementId );
            listener.onInterstitialAdHidden();
        }

        @Override
        public void onAdError(OguryError oguryError)
        {
            if ( isFinishedLoading )
            {
                log( "Interstitial (" + placementId + ") failed to show with error: " + oguryError );
                listener.onInterstitialAdDisplayFailed( toMaxError( oguryError ) );
            }
            else
            {
                log( "Interstitial (" + placementId + ") failed to load with error: " + oguryError );
                listener.onInterstitialAdLoadFailed( toMaxError( oguryError ) );
            }
        }
    }

    private class RewardedAdListener
            implements OguryOptinVideoAdListener, OguryAdImpressionListener
    {
        private final String                     placementId;
        private final MaxRewardedAdapterListener listener;
        private       boolean                    hasGrantedReward;

        RewardedAdListener(final String placementId, final MaxRewardedAdapterListener listener)
        {
            this.placementId = placementId;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded()
        {
            log( "Rewarded ad loaded: " + placementId );
            listener.onRewardedAdLoaded();
        }

        @Override
        public void onAdDisplayed()
        {
            log( "Rewarded ad shown: " + placementId );
        }

        @Override
        public void onAdImpression()
        {
            log( "Rewarded ad triggered impression: " + placementId );
            listener.onRewardedAdDisplayed();
            listener.onRewardedAdVideoStarted();
        }

        @Override
        public void onAdClicked()
        {
            log( "Rewarded ad clicked: " + placementId );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onAdClosed()
        {
            listener.onRewardedAdVideoCompleted();

            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            log( "Rewarded ad hidden: " + placementId );
            listener.onRewardedAdHidden();
        }

        @Override
        public void onAdRewarded(OguryReward oguryReward)
        {
            log( "Rewarded ad (" + placementId + ") granted reward with rewardName: " + oguryReward.getName() + ", rewardValue: " + oguryReward.getValue() );
            hasGrantedReward = true;
        }

        @Override
        public void onAdError(OguryError oguryError)
        {
            if ( isFinishedLoading )
            {
                log( "Rewarded ad (" + placementId + ") failed to show with error: " + oguryError );
                listener.onRewardedAdDisplayFailed( toMaxError( oguryError ) );
            }
            else
            {
                log( "Rewarded ad (" + placementId + ") failed to load with error: " + oguryError );
                listener.onRewardedAdLoadFailed( toMaxError( oguryError ) );
            }
        }
    }

    private class AdViewListener
            implements OguryBannerAdListener, OguryAdImpressionListener
    {
        private final String                   placementId;
        private final MaxAdViewAdapterListener listener;

        AdViewListener(final String placementId, final MaxAdViewAdapterListener listener)
        {
            this.placementId = placementId;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded()
        {
            log( "AdView loaded: " + placementId );
            isFinishedLoading = true;
            listener.onAdViewAdLoaded( adView );
        }

        @Override
        public void onAdDisplayed()
        {
            log( "AdView shown: " + placementId );
        }

        @Override
        public void onAdImpression()
        {
            log( "AdView triggered impression: " + placementId );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdClicked()
        {
            log( "AdView clicked: " + placementId );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdClosed()
        {
            log( "AdView ad hidden: " + placementId );
        }

        @Override
        public void onAdError(OguryError oguryError)
        {
            if ( isFinishedLoading )
            {
                log( "AdView ad (" + placementId + ") failed to show with error: " + oguryError );
                listener.onAdViewAdDisplayFailed( toMaxError( oguryError ) );
            }
            else
            {
                log( "AdView ad (" + placementId + ") failed to load with error: " + oguryError );
                listener.onAdViewAdLoadFailed( toMaxError( oguryError ) );
            }
        }
    }
}
