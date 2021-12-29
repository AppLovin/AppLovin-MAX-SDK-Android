package com.applovin.mediation.adapters;

import android.app.Activity;
import android.os.Bundle;
import android.text.TextUtils;

import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapters.mopub.BuildConfig;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.mopub.common.MoPub;
import com.mopub.common.MoPubReward;
import com.mopub.common.SdkConfiguration;
import com.mopub.common.SdkInitializationListener;
import com.mopub.common.logging.MoPubLog;
import com.mopub.mobileads.MoPubErrorCode;
import com.mopub.mobileads.MoPubInterstitial;
import com.mopub.mobileads.MoPubRewardedAdListener;
import com.mopub.mobileads.MoPubRewardedAds;
import com.mopub.mobileads.MoPubView;
import com.mopub.network.ImpressionData;
import com.mopub.network.ImpressionListener;
import com.mopub.network.ImpressionsEmitter;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.Nullable;

public class MoPubMediationAdapter
        extends MediationAdapterBase
        implements MaxInterstitialAdapter, MaxAdViewAdapter, MaxRewardedAdapter
{
    private static final MoPubRouter   ROUTER      = new MoPubRouter(); // Note: used only for the rewarded events
    private static final AtomicBoolean INITIALIZED = new AtomicBoolean();

    private String rewardedAdUnitId;

    private MoPubInterstitial    interstitial;
    private InterstitialListener interstitialListener;

    private MoPubView      adView;
    private AdViewListener adViewListener;

    // Explicit default constructor declaration
    public MoPubMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public String getSdkVersion()
    {
        return getVersionString( MoPub.class, "SDK_VERSION" );
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( INITIALIZED.compareAndSet( false, true ) )
        {
            final String adUnitId = parameters.getServerParameters().getString( "init_ad_unit_id", "" );

            log( "Initializing MoPub SDK with adUnitId: " + adUnitId + "..." );

            // As of MoPub SDK 5.0.0 it is required to initialize their SDK (for GDPR, rewarded videos) - https://developers.mopub.com/docs/android/initialization/
            final SdkConfiguration sdkConfiguration = new SdkConfiguration.Builder( adUnitId )
                    .withLogLevel( parameters.isTesting() ? MoPubLog.LogLevel.DEBUG : MoPubLog.LogLevel.INFO )
                    .build();

            MoPub.initializeSdk( activity, sdkConfiguration, new SdkInitializationListener()
            {
                @Override
                public void onInitializationFinished()
                {
                    log( "MoPub SDK initialized" );

                    updateMoPubConsent( parameters );

                    MoPubRewardedAds.setRewardedAdListener( ROUTER );
                    ImpressionsEmitter.addListener( ROUTER );

                    onCompletionListener.onCompletion( InitializationStatus.INITIALIZED_UNKNOWN, null );
                }
            } );
        }
        else
        {
            if ( MoPub.isSdkInitialized() )
            {
                log( "MoPub SDK already initialized" );
                onCompletionListener.onCompletion( InitializationStatus.INITIALIZED_UNKNOWN, null );
            }
            else
            {
                log( "MoPub SDK still initializing" );
                onCompletionListener.onCompletion( InitializationStatus.INITIALIZING, null );
            }
        }
    }

    @Override
    public void onDestroy()
    {
        if ( rewardedAdUnitId != null )
        {
            ROUTER.removeAdapter( this, rewardedAdUnitId );
        }

        if ( interstitial != null )
        {
            interstitial.destroy();
            interstitial = null;
        }

        if ( interstitialListener != null )
        {
            ImpressionsEmitter.removeListener( interstitialListener );
            interstitialListener = null;
        }

        if ( adView != null )
        {
            adView.destroy();
            adView = null;
        }

        if ( adViewListener != null )
        {
            ImpressionsEmitter.removeListener( adViewListener );
            adViewListener = null;
        }
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String adUnitId = parameters.getThirdPartyAdPlacementId();
        log( "Loading interstitial ad: " + adUnitId + "..." );

        if ( !MoPub.isSdkInitialized() )
        {
            log( "MoPub SDK is not initialized" );

            listener.onInterstitialAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );
            return;
        }

        updateMoPubConsent( parameters );

        interstitial = new MoPubInterstitial( activity, adUnitId );
        interstitial.setTesting( parameters.isTesting() );

        interstitialListener = new InterstitialListener( adUnitId, listener );
        interstitial.setInterstitialAdListener( interstitialListener );

        interstitial.load();
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String adUnitId = parameters.getThirdPartyAdPlacementId();
        log( "Showing interstitial ad: " + adUnitId + "..." );

        if ( interstitial != null && interstitial.isReady() )
        {
            ImpressionsEmitter.addListener( interstitialListener );
            interstitial.show();
        }
        else
        {
            log( "Interstitial ad not ready" );
            listener.onInterstitialAdDisplayFailed( MaxAdapterError.AD_NOT_READY );
        }
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        rewardedAdUnitId = parameters.getThirdPartyAdPlacementId();
        log( "Loading rewarded ad: " + rewardedAdUnitId + "..." );

        ROUTER.addRewardedAdapter( this, listener, rewardedAdUnitId );

        if ( !MoPub.isSdkInitialized() )
        {
            log( "MoPub SDK is not initialized" );

            ROUTER.onAdLoadFailed( rewardedAdUnitId, MaxAdapterError.NOT_INITIALIZED );
            return;
        }

        updateMoPubConsent( parameters );

        if ( MoPubRewardedAds.hasRewardedAd( rewardedAdUnitId ) )
        {
            log( "Rewarded ad already available" );
            ROUTER.onAdLoaded( rewardedAdUnitId );
        }
        else
        {
            MoPubRewardedAds.loadRewardedAd( rewardedAdUnitId );
        }
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String adUnitId = parameters.getThirdPartyAdPlacementId();
        log( "Showing rewarded ad: " + adUnitId + "..." );

        ROUTER.addShowingAdapter( this );

        if ( MoPubRewardedAds.hasRewardedAd( adUnitId ) )
        {
            // Configure userReward from server.
            configureReward( parameters );

            MoPubRewardedAds.showRewardedAd( adUnitId );
        }
        else
        {
            log( "Rewarded ad not ready" );
            ROUTER.onAdDisplayFailed( adUnitId, MaxAdapterError.AD_NOT_READY );
        }
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final String adUnitId = parameters.getThirdPartyAdPlacementId();
        log( "Loading AdView ad: " + adUnitId + "..." );

        if ( !MoPub.isSdkInitialized() )
        {
            log( "MoPub SDK is not initialized" );

            listener.onAdViewAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );
            return;
        }

        updateMoPubConsent( parameters );

        adView = new MoPubView( activity );
        adView.setAdUnitId( adUnitId );
        adView.setTesting( parameters.isTesting() );
        adView.setAutorefreshEnabled( false );

        adViewListener = new AdViewListener( adUnitId, listener );
        adView.setBannerAdListener( adViewListener );

        ImpressionsEmitter.addListener( adViewListener );

        // Load ad
        adView.loadAd( toAdSize( adFormat ) );
    }

    private MoPubView.MoPubAdSize toAdSize(MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return MoPubView.MoPubAdSize.HEIGHT_50;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return MoPubView.MoPubAdSize.HEIGHT_90;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return MoPubView.MoPubAdSize.HEIGHT_250;
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported ad format: " + adFormat );
        }
    }

    private void updateMoPubConsent(final MaxAdapterParameters parameters)
    {
        // Set SDK consent
        if ( getWrappingSdk().getConfiguration().getConsentDialogState() == AppLovinSdkConfiguration.ConsentDialogState.APPLIES )
        {
            Boolean hasUserConsent = getPrivacySetting( "hasUserConsent", parameters );
            if ( hasUserConsent != null )
            {
                if ( hasUserConsent )
                {
                    MoPub.getPersonalInformationManager().grantConsent();
                }
                else
                {
                    MoPub.getPersonalInformationManager().revokeConsent();
                }
            }
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

    private static MaxAdapterError toMaxError(final MoPubErrorCode moPubErrorCode)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( moPubErrorCode )
        {
            case UNSPECIFIED:
            case ADAPTER_INITIALIZATION_SUCCESS:
            case AD_SUCCESS:
                adapterError = MaxAdapterError.UNSPECIFIED;
                break;
            case NO_FILL:
            case NETWORK_NO_FILL:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case WARMUP:
            case CANCELLED:
            case ADAPTER_NOT_FOUND:
            case NETWORK_INVALID_STATE:
                adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                break;
            case SERVER_ERROR:
                adapterError = MaxAdapterError.SERVER_ERROR;
                break;
            case TOO_MANY_REQUESTS:
                adapterError = MaxAdapterError.AD_FREQUENCY_CAPPED;
                break;
            case INTERNAL_ERROR:
            case RENDER_PROCESS_GONE_WITH_CRASH:
            case RENDER_PROCESS_GONE_UNSPECIFIED:
            case DO_NOT_TRACK:
            case MRAID_LOAD_ERROR:
            case HTML_LOAD_ERROR:
            case INLINE_LOAD_ERROR:
            case FULLSCREEN_LOAD_ERROR:
            case INLINE_SHOW_ERROR:
            case FULLSCREEN_SHOW_ERROR:
            case VIDEO_CACHE_ERROR:
            case VIDEO_DOWNLOAD_ERROR:
            case GDPR_DOES_NOT_APPLY:
            case REWARDED_CURRENCIES_PARSING_ERROR:
            case REWARD_NOT_SELECTED:
            case VIDEO_NOT_AVAILABLE:
            case VIDEO_PLAYBACK_ERROR:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case MISSING_AD_UNIT_ID:
            case ADAPTER_CONFIGURATION_ERROR:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case NO_CONNECTION:
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case EXPIRED:
                adapterError = MaxAdapterError.AD_EXPIRED;
                break;
            case NETWORK_TIMEOUT:
                adapterError = MaxAdapterError.TIMEOUT;
                break;
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), moPubErrorCode.ordinal(), moPubErrorCode.toString() );
    }

    //region Listeners

    private class InterstitialListener
            implements MoPubInterstitial.InterstitialAdListener, ImpressionListener
    {
        private final String                         adUnitId;
        private final MaxInterstitialAdapterListener listener;

        InterstitialListener(final String adUnitId, final MaxInterstitialAdapterListener listener)
        {
            this.adUnitId = adUnitId;
            this.listener = listener;
        }

        @Override
        public void onInterstitialLoaded(final MoPubInterstitial interstitial)
        {
            log( "Interstitial loaded: " + adUnitId );
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onInterstitialFailed(final MoPubInterstitial interstitial, final MoPubErrorCode errorCode)
        {
            log( "Interstitial (" + adUnitId + ") failed to load with error: " + errorCode );
            listener.onInterstitialAdLoadFailed( toMaxError( errorCode ) );
        }

        @Override
        public void onInterstitialShown(final MoPubInterstitial interstitial)
        {
            log( "Interstitial shown: " + adUnitId );
        }

        @Override
        public void onImpression(final String impressionAdUnitId, @Nullable final ImpressionData impressionData)
        {
            // Note: this is called for any ad format/unit_id impression.
            if ( !impressionAdUnitId.equals( adUnitId ) ) return;

            log( "Interstitial did track impression: " + adUnitId );

            // Passing extra info such as creative id supported in 9.15.0+
            if ( AppLovinSdk.VERSION_CODE >= 9150000 && impressionData != null && !TextUtils.isEmpty( impressionData.getImpressionId() ) )
            {
                Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", impressionData.getImpressionId() );

                listener.onInterstitialAdDisplayed( extraInfo );
            }
            else
            {
                listener.onInterstitialAdDisplayed();
            }
        }

        @Override
        public void onInterstitialClicked(final MoPubInterstitial interstitial)
        {
            log( "Interstitial clicked: " + adUnitId );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onInterstitialDismissed(final MoPubInterstitial interstitial)
        {
            log( "Interstitial hidden: " + adUnitId );
            listener.onInterstitialAdHidden();
        }
    }

    private class AdViewListener
            implements MoPubView.BannerAdListener, ImpressionListener
    {
        private final String                   adUnitId;
        private final MaxAdViewAdapterListener listener;

        AdViewListener(final String adUnitId, final MaxAdViewAdapterListener listener)
        {
            this.adUnitId = adUnitId;
            this.listener = listener;
        }

        @Override
        public void onBannerLoaded(final MoPubView banner)
        {
            log( "AdView loaded: " + adUnitId );
            listener.onAdViewAdLoaded( adView );
        }

        @Override
        public void onBannerFailed(final MoPubView banner, final MoPubErrorCode errorCode)
        {
            log( "AdView (" + adUnitId + ") failed to load with error: " + errorCode );
            listener.onAdViewAdLoadFailed( toMaxError( errorCode ) );
        }

        @Override
        public void onImpression(final String impressionAdUnitId, @Nullable final ImpressionData impressionData)
        {
            // Note: this is called for any ad format/unit_id impression.
            if ( !impressionAdUnitId.equals( adUnitId ) ) return;

            log( "AdView did track impression: " + adUnitId );

            // Passing extra info such as creative id supported in 9.15.0+
            if ( AppLovinSdk.VERSION_CODE >= 9150000 && impressionData != null && !TextUtils.isEmpty( impressionData.getImpressionId() ) )
            {
                Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", impressionData.getImpressionId() );

                listener.onAdViewAdDisplayed( extraInfo );
            }
            else
            {
                listener.onAdViewAdDisplayed();
            }
        }

        @Override
        public void onBannerClicked(final MoPubView banner)
        {
            log( "AdView clicked: " + adUnitId );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onBannerExpanded(final MoPubView banner)
        {
            log( "AdView expanded: " + adUnitId );
            listener.onAdViewAdExpanded();
        }

        @Override
        public void onBannerCollapsed(final MoPubView banner)
        {
            log( "AdView collapsed: " + adUnitId );
            listener.onAdViewAdCollapsed();
        }
    }

    // Used for rewarded events
    private static class MoPubRouter
            extends MediationAdapterRouter
            implements MoPubRewardedAdListener, ImpressionListener
    {
        private boolean hasGrantedReward = false;

        //TODO: marked for deletion, pending SDK change.
        void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final MaxAdapter.OnCompletionListener onCompletionListener) { }

        @Override
        public void onRewardedAdLoadSuccess(final String adUnitId)
        {
            log( "Rewarded ad loaded: " + adUnitId );
            onAdLoaded( adUnitId );
        }

        @Override
        public void onRewardedAdLoadFailure(final String adUnitId, final MoPubErrorCode errorCode)
        {
            log( "Rewarded ad (" + adUnitId + ") failed to load with error: " + errorCode );
            onAdLoadFailed( adUnitId, toMaxError( errorCode ) );
        }

        @Override
        public void onRewardedAdStarted(final String adUnitId)
        {
            log( "Rewarded ad video started: " + adUnitId );
            onRewardedAdVideoStarted( adUnitId );
        }

        @Override
        public void onImpression(final String impressionAdUnitId, @Nullable final ImpressionData impressionData)
        {
            // Note: this is called for any ad format/unit_id impression.
            // The MediationAdapterRouter filters by ad unit, so it is safe to call onAdDisplayed.

            // Passing extra info such as creative id supported in 9.15.0+
            if ( AppLovinSdk.VERSION_CODE >= 9150000 && impressionData != null && !TextUtils.isEmpty( impressionData.getImpressionId() ) )
            {
                Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", impressionData.getImpressionId() );

                onAdDisplayed( impressionAdUnitId, extraInfo );
            }
            else
            {
                onAdDisplayed( impressionAdUnitId );
            }
        }

        @Override
        public void onRewardedAdShowError(final String adUnitId, final MoPubErrorCode errorCode)
        {
            log( "Rewarded ad (" + adUnitId + ") failed to display: " + errorCode );
            onAdDisplayFailed( adUnitId, toMaxError( errorCode ) );
        }

        @Override
        public void onRewardedAdClicked(final String adUnitId)
        {
            log( "Rewarded ad clicked: " + adUnitId );
            onAdClicked( adUnitId );
        }

        @Override
        public void onRewardedAdCompleted(final Set<String> adUnitIds, final MoPubReward reward)
        {
            log( "Rewarded ad video completed: " + adUnitIds );

            for ( String adUnitId : adUnitIds )
            {
                onRewardedAdVideoCompleted( adUnitId );
            }

            if ( reward.isSuccessful() )
            {
                hasGrantedReward = true;
            }
        }

        @Override
        public void onRewardedAdClosed(final String adUnitId)
        {
            log( "Rewarded ad video closed: " + adUnitId );

            if ( hasGrantedReward || shouldAlwaysRewardUser( adUnitId ) )
            {
                MaxReward reward = getReward( adUnitId );
                log( "Rewarded user with reward: " + reward );
                onUserRewarded( adUnitId, reward );

                // clear hasGrantedReward
                hasGrantedReward = false;
            }

            log( "Rewarded ad hidden: " + adUnitId );
            onAdHidden( adUnitId );
        }
    }

    //endregion
}
