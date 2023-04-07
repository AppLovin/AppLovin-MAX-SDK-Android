package com.applovin.mediation.adapters;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.text.TextUtils;

import com.adcolony.sdk.AdColony;
import com.adcolony.sdk.AdColonyAdSize;
import com.adcolony.sdk.AdColonyAdView;
import com.adcolony.sdk.AdColonyAdViewListener;
import com.adcolony.sdk.AdColonyAppOptions;
import com.adcolony.sdk.AdColonyInterstitial;
import com.adcolony.sdk.AdColonyInterstitialListener;
import com.adcolony.sdk.AdColonyReward;
import com.adcolony.sdk.AdColonyRewardListener;
import com.adcolony.sdk.AdColonySignalsListener;
import com.adcolony.sdk.AdColonyZone;
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
import com.applovin.mediation.adapters.adcolony.BuildConfig;
import com.applovin.sdk.AppLovinSdk;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Thomas So on February 16 2019
 */
public class AdColonyMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter
{
    private static final AtomicBoolean initialized = new AtomicBoolean();

    private static InitializationStatus status;

    private AdColonyInterstitial loadedInterstitialAd;
    private AdColonyInterstitial loadedRewardedAd;
    private AdColonyAdView       loadedAdViewAd;

    // Explicit default constructor declaration
    public AdColonyMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public String getSdkVersion()
    {
        // NOTE: AdColony returns an empty string if attempting to retrieve if not initialized
        return AdColony.getSDKVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        if ( loadedInterstitialAd != null )
        {
            loadedInterstitialAd.destroy();
            loadedInterstitialAd = null;
        }

        if ( loadedRewardedAd != null )
        {
            loadedRewardedAd.destroy();
            loadedRewardedAd = null;
        }

        if ( loadedAdViewAd != null )
        {
            loadedAdViewAd.destroy();
            loadedAdViewAd = null;
        }
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal for " + parameters.getAdFormat() + " ad..." );

        AdColony.setAppOptions( getOptions( parameters ) );

        AdColony.collectSignals( new AdColonySignalsListener()
        {
            @Override
            public void onSuccess(final String signal)
            {
                log( "Signal collection successful" );
                callback.onSignalCollected( signal );
            }

            @Override
            public void onFailure()
            {
                log( "Signal collection failed" );
                callback.onSignalCollectionFailed( "AdColony has not yet been configured or there was an error parsing data" ); // The info in the errorMessage is from method's quick documentation
            }
        } );
    }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( initialized.compareAndSet( false, true ) )
        {
            status = InitializationStatus.INITIALIZING;

            final String appId = parameters.getServerParameters().getString( "app_id" );
            log( "Initializing AdColony SDK with app id: " + appId + "..." );

            final AdColonyAppOptions options = getOptions( parameters );

            // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
            Application application = ( activity != null ) ? activity.getApplication() : (Application) getApplicationContext();

            final boolean initialized = AdColony.configure( application, options, appId );

            status = initialized ? InitializationStatus.INITIALIZED_SUCCESS : InitializationStatus.INITIALIZED_FAILURE;
        }

        onCompletionListener.onCompletion( status, null );
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String zoneId = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        log( "Loading " + ( TextUtils.isEmpty( bidResponse ) ? "bidding " : "" ) + " interstitial ad for zone id " + zoneId + "..." );

        if ( !isAdColonyConfigured() )
        {
            log( "AdColony SDK is not initialized" );

            listener.onInterstitialAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );
            return;
        }

        AdColony.setAppOptions( getOptions( parameters ) );

        AdColony.requestInterstitial( zoneId, new InterstitialListener( listener ) );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad..." );

        if ( loadedInterstitialAd == null )
        {
            log( "Interstitial ad not ready" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Interstitial ad not ready" ) );

            return;
        }

        if ( loadedInterstitialAd.isExpired() )
        {
            log( "Interstitial ad is expired" );

            MaxAdapterError adapterError = MaxAdapterError.AD_EXPIRED;
            listener.onInterstitialAdDisplayFailed( adapterError );

            return;
        }

        final boolean success = loadedInterstitialAd.show();
        if ( !success )
        {
            log( "Interstitial ad failed to display" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Interstitial ad failed to display" ) );
        }
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final String zoneId = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();
        log( "Loading " + ( TextUtils.isEmpty( bidResponse ) ? "bidding " : "" ) + " rewarded ad for zone id " + zoneId + "..." );

        if ( !isAdColonyConfigured() )
        {
            log( "AdColony SDK is not initialized" );

            listener.onRewardedAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );
            return;
        }

        AdColony.setAppOptions( getOptions( parameters ) );

        RewardedAdListener rewardedAdListener = new RewardedAdListener( listener );
        AdColony.setRewardListener( rewardedAdListener );
        AdColony.requestInterstitial( zoneId, rewardedAdListener );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad..." );

        if ( loadedRewardedAd == null )
        {
            log( "Rewarded ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Rewarded ad not ready" ) );

            return;
        }

        if ( loadedRewardedAd.isExpired() )
        {
            log( "Rewarded ad is expired" );

            MaxAdapterError adapterError = MaxAdapterError.AD_EXPIRED;
            listener.onRewardedAdDisplayFailed( adapterError );

            return;
        }

        // Configure userReward from server.
        configureReward( parameters );

        final boolean success = loadedRewardedAd.show();
        if ( !success )
        {
            log( "Rewarded ad failed to display" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed" ) );
        }
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final String zoneId = parameters.getThirdPartyAdPlacementId();
        final String bidResponse = parameters.getBidResponse();

        log( "Loading " + ( TextUtils.isEmpty( bidResponse ) ? "bidding " : "" ) + adFormat.getLabel() + " ad for zone id " + zoneId + "..." );

        if ( !isAdColonyConfigured() )
        {
            log( "AdColony SDK is not initialized" );

            listener.onAdViewAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );
            return;
        }

        AdColony.setAppOptions( getOptions( parameters ) );

        AdColony.requestAdView( zoneId, new AdViewAdListener( adFormat, listener ), sizeFromAdFormat( adFormat ) );
    }

    //region Helper Methods

    private boolean isAdColonyConfigured()
    {
        return !AdColony.getSDKVersion().isEmpty();
    }

    private AdColonyAppOptions getOptions(final MaxAdapterParameters parameters)
    {
        final Bundle serverParameters = parameters.getServerParameters();
        final AdColonyAppOptions options = new AdColonyAppOptions();

        //
        // Basic options
        //
        options.setTestModeEnabled( parameters.isTesting() );
        options.setMediationNetwork( "AppLovin", AppLovinSdk.VERSION );

        //
        // GDPR options
        //

        // Set user consent state
        Boolean hasUserConsent = getPrivacySetting( "hasUserConsent", parameters );
        if ( hasUserConsent != null )
        {
            options.setPrivacyConsentString( AdColonyAppOptions.GDPR, hasUserConsent ? "1" : "0" );
        }

        //
        // CCPA options
        //
        if ( AppLovinSdk.VERSION_CODE >= 91100 )
        {
            Boolean isDoNotSell = getPrivacySetting( "isDoNotSell", parameters );
            if ( isDoNotSell != null )
            {
                options.setPrivacyFrameworkRequired( AdColonyAppOptions.CCPA, true );
                options.setPrivacyConsentString( AdColonyAppOptions.CCPA, isDoNotSell ? "0" : "1" ); // isDoNotSell means user has opted out of selling data.
            }
            else
            {
                options.setPrivacyFrameworkRequired( AdColonyAppOptions.CCPA, false );
            }
        }

        //
        // COPPA options
        //
        Boolean isAgeRestrictedUser = getPrivacySetting( "isAgeRestrictedUser", parameters );
        if ( isAgeRestrictedUser != null )
        {
            options.setPrivacyFrameworkRequired( AdColonyAppOptions.COPPA, isAgeRestrictedUser );
        }

        //
        // Bidding options
        //

        // If AdColony wins the auction, network adapters need to send any .adm content via ad_options to the AdColony SDK when making the ad request
        if ( parameters instanceof MaxAdapterResponseParameters )
        {
            final String bidResponse = ( (MaxAdapterResponseParameters) parameters ).getBidResponse();
            if ( !TextUtils.isEmpty( bidResponse ) )
            {
                options.setOption( "adm", bidResponse );
            }
        }

        //
        // Other options
        //
        if ( serverParameters.containsKey( "app_orientation" ) )
        {
            // 0 = PORTRAIT, 1 = LANDSCAPE, 2 = ALL
            options.setAppOrientation( serverParameters.getInt( "app_orientation" ) );
        }

        if ( serverParameters.containsKey( "app_version" ) )
        {
            options.setAppVersion( serverParameters.getString( "app_version" ) );
        }

        if ( serverParameters.containsKey( "keep_screen_on" ) )
        {
            options.setKeepScreenOn( serverParameters.getBoolean( "keep_screen_on" ) );
        }

        if ( serverParameters.containsKey( "multi_window_enabled" ) )
        {
            options.setMultiWindowEnabled( serverParameters.getBoolean( "multi_window_enabled" ) );
        }

        if ( serverParameters.containsKey( "origin_store" ) )
        {
            options.setOriginStore( serverParameters.getString( "origin_store" ) );
        }

        if ( serverParameters.containsKey( "requested_ad_orientation" ) )
        {
            // 0 = PORTRAIT, 1 = LANDSCAPE, 2 = ALL
            options.setRequestedAdOrientation( serverParameters.getInt( "requested_ad_orientation" ) );
        }

        if ( serverParameters.containsKey( "plugin" ) && serverParameters.containsKey( "plugin_version" ) )
        {
            options.setPlugin( serverParameters.getString( "plugin" ), serverParameters.getString( "plugin_version" ) );
        }

        if ( serverParameters.containsKey( "user_id" ) )
        {
            options.setUserID( serverParameters.getString( "user_id" ) );
        }

        return options;
    }

    private AdColonyAdSize sizeFromAdFormat(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return AdColonyAdSize.BANNER;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return AdColonyAdSize.LEADERBOARD;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return AdColonyAdSize.MEDIUM_RECTANGLE;
        }
        else
        {
            throw new IllegalArgumentException( "Invalid ad format: " + adFormat );
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

    //endregion

    private class InterstitialListener
            extends AdColonyInterstitialListener
    {
        private final MaxInterstitialAdapterListener listener;

        InterstitialListener(final MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onRequestFilled(final AdColonyInterstitial adColonyInterstitial)
        {
            log( "Interstitial loaded" );

            loadedInterstitialAd = adColonyInterstitial;
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onRequestNotFilled(final AdColonyZone zone)
        {
            log( "Interstitial failed to fill for zone: " + zone.getZoneID() );
            listener.onInterstitialAdLoadFailed( MaxAdapterError.NO_FILL );
        }

        @Override
        public void onOpened(final AdColonyInterstitial ad)
        {
            log( "Interstitial shown" );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onClosed(final AdColonyInterstitial ad)
        {
            log( "Interstitial hidden" );
            listener.onInterstitialAdHidden();
        }

        @Override
        public void onExpiring(final AdColonyInterstitial ad)
        {
            log( "Interstitial expiring: " + ad.getZoneID() );
        }

        @Override
        public void onLeftApplication(final AdColonyInterstitial ad)
        {
            log( "Interstitial left application" );
        }

        @Override
        public void onClicked(final AdColonyInterstitial ad)
        {
            log( "Interstitial clicked" );
            listener.onInterstitialAdClicked();
        }
    }

    private class RewardedAdListener
            extends AdColonyInterstitialListener
            implements AdColonyRewardListener
    {
        private final MaxRewardedAdapterListener listener;
        private       boolean                    hasGrantedReward;

        RewardedAdListener(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onReward(final AdColonyReward adColonyReward)
        {
            if ( adColonyReward.success() )
            {
                log( "Rewarded ad granted reward" );
                hasGrantedReward = true;
            }
            else
            {
                log( "Rewarded ad did not grant reward" );
            }
        }

        @Override
        public void onRequestFilled(final AdColonyInterstitial adColonyInterstitial)
        {
            log( "Rewarded ad loaded" );

            loadedRewardedAd = adColonyInterstitial;
            listener.onRewardedAdLoaded();
        }

        @Override
        public void onRequestNotFilled(final AdColonyZone zone)
        {
            log( "Rewarded ad failed to fill for zone: " + zone.getZoneID() );
            listener.onRewardedAdLoadFailed( MaxAdapterError.NO_FILL );
        }

        @Override
        public void onOpened(final AdColonyInterstitial ad)
        {
            log( "Rewarded ad shown" );
            listener.onRewardedAdDisplayed();
            listener.onRewardedAdVideoStarted();
        }

        @Override
        public void onClosed(final AdColonyInterstitial ad)
        {
            listener.onRewardedAdVideoCompleted();

            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            log( "Rewarded ad hidden" );
            listener.onRewardedAdHidden();
        }

        @Override
        public void onExpiring(final AdColonyInterstitial ad)
        {
            log( "Rewarded ad expiring: " + ad.getZoneID() );
        }

        @Override
        public void onLeftApplication(final AdColonyInterstitial ad)
        {
            log( "Rewarded ad left application" );
        }

        @Override
        public void onClicked(final AdColonyInterstitial ad)
        {
            log( "Rewarded ad clicked" );
            listener.onRewardedAdClicked();
        }
    }

    private class AdViewAdListener
            extends AdColonyAdViewListener
    {
        private final MaxAdViewAdapterListener listener;
        private final MaxAdFormat              adFormat;

        AdViewAdListener(final MaxAdFormat adFormat, final MaxAdViewAdapterListener listener)
        {
            this.listener = listener;
            this.adFormat = adFormat;
        }

        @Override
        public void onRequestFilled(final AdColonyAdView adColonyAdView)
        {
            log( adFormat.getLabel() + " ad loaded" );

            loadedAdViewAd = adColonyAdView;
            listener.onAdViewAdLoaded( loadedAdViewAd );
        }

        @Override
        public void onShow(final AdColonyAdView ad)
        {
            log( adFormat.getLabel() + " ad shown" );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onLeftApplication(final AdColonyAdView ad)
        {
            log( adFormat.getLabel() + " ad left application" );
        }

        @Override
        public void onClicked(final AdColonyAdView ad)
        {
            log( adFormat.getLabel() + " ad clicked" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onRequestNotFilled(final AdColonyZone zone)
        {
            log( adFormat.getLabel() + " ad failed to fill for zone: " + zone.getZoneID() );
            listener.onAdViewAdLoadFailed( MaxAdapterError.NO_FILL );
        }
    }
}
