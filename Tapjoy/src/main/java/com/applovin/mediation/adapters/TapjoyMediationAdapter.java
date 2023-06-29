package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.text.TextUtils;

import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.MaxSignalProvider;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.adapters.tapjoy.BuildConfig;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.tapjoy.TJActionRequest;
import com.tapjoy.TJConnectListener;
import com.tapjoy.TJError;
import com.tapjoy.TJPlacement;
import com.tapjoy.TJPlacementListener;
import com.tapjoy.TJPlacementVideoListener;
import com.tapjoy.TJPrivacyPolicy;
import com.tapjoy.Tapjoy;
import com.tapjoy.TapjoyConnectFlag;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;

/**
 * Created by Thomas So on October 20 2018
 */
public class TapjoyMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter
{
    private TJPlacement interstitialPlacement;
    private TJPlacement rewardedPlacement;

    // Explicit default constructor declaration
    public TapjoyMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public String getSdkVersion()
    {
        return Tapjoy.getVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        if ( interstitialPlacement != null )
        {
            interstitialPlacement.setVideoListener( null );
            interstitialPlacement = null;
        }

        if ( rewardedPlacement != null )
        {
            rewardedPlacement.setVideoListener( null );
            rewardedPlacement = null;
        }
    }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( !Tapjoy.isConnected() )
        {
            final String sdkKey = parameters.getServerParameters().getString( "sdk_key" );
            log( "Initializing Tapjoy SDK with sdk key: " + sdkKey + "..." );

            Tapjoy.setDebugEnabled( parameters.isTesting() );

            Hashtable<String, Object> connectFlags = new Hashtable<String, Object>( 1 );
            connectFlags.put( TapjoyConnectFlag.ENABLE_LOGGING, String.valueOf( parameters.isTesting() ) );

            Context context = getContext( activity );

            // Update GDPR settings before initialization
            updateConsentAndActivity( parameters, activity, context );

            Tapjoy.connect( context, sdkKey, connectFlags, new TJConnectListener()
            {
                @Override
                public void onConnectSuccess()
                {
                    log( "Tapjoy SDK initialized" );

                    onCompletionListener.onCompletion( InitializationStatus.INITIALIZED_SUCCESS, null );
                }

                @Override
                public void onConnectFailure()
                {
                    log( "Tapjoy SDK failed to initialized" );

                    onCompletionListener.onCompletion( InitializationStatus.INITIALIZED_FAILURE, null );
                }
            } );
        }
        else
        {
            updateConsentAndActivity( parameters, activity, getContext( activity ) );

            onCompletionListener.onCompletion( InitializationStatus.INITIALIZED_SUCCESS, null );
        }
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        // Update GDPR settings
        updateConsentAndActivity( parameters, activity, getContext( activity ) );

        String token = Tapjoy.getUserToken();
        callback.onSignalCollected( token );
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Loading interstitial..." );

        if ( !Tapjoy.isConnected() )
        {
            log( "Tapjoy SDK is not initialized" );

            listener.onInterstitialAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );
            return;
        }

        // Update GDPR settings
        updateConsentAndActivity( parameters, activity, getContext( activity ) );

        interstitialPlacement = createPlacement( parameters, new InterstitialListener( listener ) );

        if ( interstitialPlacement != null )
        {
            interstitialPlacement.requestContent();
        }
        else
        {
            listener.onInterstitialAdLoadFailed( MaxAdapterError.BAD_REQUEST );
        }
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial..." );

        if ( interstitialPlacement.isContentReady() )
        {
            interstitialPlacement.showContent();
        }
        else
        {
            log( "Interstitial ad not ready" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Interstitial ad not ready" ) );
        }
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Loading rewarded ad..." );

        if ( !Tapjoy.isConnected() )
        {
            log( "Tapjoy SDK is not initialized" );

            listener.onRewardedAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );
            return;
        }

        // Update GDPR settings
        updateConsentAndActivity( parameters, activity, getContext( activity ) );

        rewardedPlacement = createPlacement( parameters, new RewardedListener( listener ) );

        if ( rewardedPlacement != null )
        {
            rewardedPlacement.requestContent();
        }
        else
        {
            listener.onRewardedAdLoadFailed( MaxAdapterError.BAD_REQUEST );
        }
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad..." );

        if ( rewardedPlacement.isContentReady() )
        {
            // Configure userReward from server.
            configureReward( parameters );

            rewardedPlacement.showContent();
        }
        else
        {
            log( "Rewarded ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Rewarded ad not ready" ) );
        }
    }

    //region Utility Methods
    private void updateConsentAndActivity(final MaxAdapterParameters parameters, final Activity activity, final Context context)
    {
        TJPrivacyPolicy tjPrivacyPolicy = Tapjoy.getPrivacyPolicy();

        // NOTE: Adapter / mediated SDK has support for COPPA, but is not approved by Play Store and therefore will be filtered on COPPA traffic
        // https://support.google.com/googleplay/android-developer/answer/9283445?hl=en
        Boolean isAgeRestrictedUser = parameters.isAgeRestrictedUser();
        if ( isAgeRestrictedUser != null )
        {
            tjPrivacyPolicy.setBelowConsentAge( isAgeRestrictedUser );

            // To comply with the Google Families Program rules, where apps targeting children should not access the advertising id
            Tapjoy.optOutAdvertisingID( context, isAgeRestrictedUser );
        }

        Boolean hasUserConsent = parameters.hasUserConsent();
        if ( hasUserConsent != null )
        {
            tjPrivacyPolicy.setUserConsent( hasUserConsent ? "1" : "0" );
        }

        Boolean isDoNotSell = parameters.isDoNotSell();
        if ( isDoNotSell != null )
        {
            tjPrivacyPolicy.setUSPrivacy( isDoNotSell ? "1YY-" : "1YN-" );
        }
        else
        {
            tjPrivacyPolicy.setUSPrivacy( "1---" );
        }

        Tapjoy.setActivity( activity );
    }

    private TJPlacement createPlacement(final MaxAdapterResponseParameters parameters, final TJPlacementListener listener)
    {
        TJPlacement placement = Tapjoy.getPlacement( parameters.getThirdPartyAdPlacementId(), listener );
        placement.setMediationName( "applovin" );
        placement.setAdapterVersion( BuildConfig.VERSION_NAME );

        if ( listener instanceof TJPlacementVideoListener )
        {
            placement.setVideoListener( (TJPlacementVideoListener) listener );
        }

        if ( !TextUtils.isEmpty( parameters.getBidResponse() ) )
        {
            try
            {
                JSONObject auctionData = new JSONObject( parameters.getBidResponse() );
                Map<String, String> auctionDataMap = AppLovinSdkUtils.toMap( auctionData );
                placement.setAuctionData( (HashMap) auctionDataMap );
            }
            catch ( Throwable th )
            {
                log( "Failed to load ad due to JSON deserialization error: ", th );

                return null;
            }
        }

        return placement;
    }

    private static MaxAdapterError toMaxError(TJError tapjoyError)
    {
        final int tapjoyErrorCode;
        final String tapjoyErrorMessage;
        if ( tapjoyError != null )
        {
            tapjoyErrorCode = tapjoyError.code;
            tapjoyErrorMessage = tapjoyError.message;
        }
        else
        {
            tapjoyErrorCode = 0;
            tapjoyErrorMessage = "";
        }

        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( tapjoyErrorCode )
        {
            case 204:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case -1011:
                adapterError = MaxAdapterError.NOT_INITIALIZED;
                break;
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), tapjoyErrorCode, tapjoyErrorMessage );
    }

    private Context getContext(Activity activity)
    {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
    }
    //endregion

    private class InterstitialListener
            implements TJPlacementListener, TJPlacementVideoListener
    {
        final MaxInterstitialAdapterListener listener;

        InterstitialListener(MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onRequestSuccess(final TJPlacement tjPlacement)
        {
            if ( tjPlacement.isContentAvailable() )
            {
                log( "Interstitial request succeeded" );
            }
            else
            {
                log( "Interstitial request failed" );
                listener.onInterstitialAdLoadFailed( MaxAdapterError.NO_FILL );
            }
        }

        @Override
        public void onRequestFailure(final TJPlacement tjPlacement, final TJError tjError)
        {
            MaxAdapterError adapterError = toMaxError( tjError );
            log( "Interstitial failed to load with error: " + adapterError );

            listener.onInterstitialAdLoadFailed( adapterError );
        }

        @Override
        public void onContentReady(final TJPlacement tjPlacement)
        {
            log( "Interstitial loaded" );
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onContentShow(final TJPlacement tjPlacement)
        {
            log( "Interstitial shown" );
        }

        @Override
        public void onClick(final TJPlacement tjPlacement)
        {
            log( "Interstitial clicked" );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onContentDismiss(final TJPlacement tjPlacement)
        {
            log( "Interstitial hidden" );
            listener.onInterstitialAdHidden();
        }

        @Override
        public void onVideoStart(final TJPlacement tjPlacement)
        {
            log( "Interstitial video start" );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onVideoError(final TJPlacement tjPlacement, final String message)
        {
            MaxAdapterError adapterError = new MaxAdapterError( MaxAdapterError.UNSPECIFIED.getErrorCode(), MaxAdapterError.UNSPECIFIED.getErrorMessage(), 0, message );
            log( "Interstitial failed with error message: " + adapterError );

            listener.onInterstitialAdDisplayFailed( adapterError );
        }

        @Override
        public void onVideoComplete(final TJPlacement tjPlacement)
        {
            log( "Interstitial video completed" );
        }

        @Override
        public void onPurchaseRequest(final TJPlacement tjPlacement, final TJActionRequest tjActionRequest, final String s) { }

        @Override
        public void onRewardRequest(final TJPlacement tjPlacement, final TJActionRequest tjActionRequest, final String s, final int i) { }
    }

    private class RewardedListener
            implements TJPlacementListener, TJPlacementVideoListener
    {
        final MaxRewardedAdapterListener listener;

        private boolean hasGrantedReward;

        RewardedListener(MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onRequestSuccess(final TJPlacement tjPlacement)
        {
            if ( tjPlacement.isContentAvailable() )
            {
                log( "Rewarded request succeeded" );
            }
            else
            {
                log( "Rewarded request failed" );
                listener.onRewardedAdLoadFailed( MaxAdapterError.NO_FILL );
            }
        }

        @Override
        public void onRequestFailure(final TJPlacement tjPlacement, final TJError tjError)
        {
            MaxAdapterError adapterError = toMaxError( tjError );
            log( "Rewarded failed to load with error: " + adapterError );

            listener.onRewardedAdLoadFailed( adapterError );
        }

        @Override
        public void onContentReady(final TJPlacement tjPlacement)
        {
            log( "Rewarded loaded" );
            listener.onRewardedAdLoaded();
        }

        @Override
        public void onContentShow(final TJPlacement tjPlacement)
        {
            log( "Rewarded shown" );
        }

        @Override
        public void onClick(final TJPlacement tjPlacement)
        {
            log( "Rewarded clicked" );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onContentDismiss(final TJPlacement tjPlacement)
        {
            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            log( "Rewarded hidden" );
            listener.onRewardedAdHidden();
        }

        @Override
        public void onPurchaseRequest(final TJPlacement tjPlacement, final TJActionRequest tjActionRequest, final String s) { }

        @Override
        public void onRewardRequest(final TJPlacement tjPlacement, final TJActionRequest tjActionRequest, final String s, final int i) { }

        @Override
        public void onVideoStart(final TJPlacement tjPlacement)
        {
            log( "Rewarded video began" );
            listener.onRewardedAdDisplayed();
            listener.onRewardedAdVideoStarted();
        }

        @Override
        public void onVideoError(final TJPlacement tjPlacement, final String message)
        {
            MaxAdapterError adapterError = new MaxAdapterError( MaxAdapterError.UNSPECIFIED.getErrorCode(), MaxAdapterError.UNSPECIFIED.getErrorMessage(), 0, message );
            log( "Rewarded failed with error: " + adapterError );

            listener.onRewardedAdDisplayFailed( adapterError );
        }

        @Override
        public void onVideoComplete(final TJPlacement tjPlacement)
        {
            log( "Rewarded video completed" );
            listener.onRewardedAdVideoCompleted();

            hasGrantedReward = true;
        }
    }
}
