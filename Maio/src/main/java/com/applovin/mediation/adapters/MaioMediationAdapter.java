package com.applovin.mediation.adapters;

import android.content.Context;
import android.app.Activity;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapters.maio.BuildConfig;
import com.applovin.mediation.adapters.MediationAdapterBase;
import com.applovin.sdk.AppLovinSdk;

import jp.maio.sdk.android.v2.Version;
import jp.maio.sdk.android.v2.interstitial.IInterstitialLoadCallback;
import jp.maio.sdk.android.v2.interstitial.IInterstitialShowCallback;
import jp.maio.sdk.android.v2.interstitial.Interstitial;
import jp.maio.sdk.android.v2.request.MaioRequest;
import jp.maio.sdk.android.v2.rewarddata.RewardData;
import jp.maio.sdk.android.v2.rewarded.IRewardedLoadCallback;
import jp.maio.sdk.android.v2.rewarded.IRewardedShowCallback;
import jp.maio.sdk.android.v2.rewarded.Rewarded;

public class MaioMediationAdapter
        extends MediationAdapterBase
        implements MaxInterstitialAdapter, MaxRewardedAdapter
{
    private Interstitial interstitialAd;
    private Rewarded rewardedAd;

    private InterstitialAdListener interstitialAdListener;
    private RewardedAdListener rewardedAdListener;

    public MaioMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(MaxAdapterInitializationParameters parameters, Activity activity, OnCompletionListener onCompletionListener)
    {
        // Maio SDK does not have an API for initialization.
        onCompletionListener.onCompletion( InitializationStatus.DOES_NOT_APPLY, null );
    }

    @Override
    public String getSdkVersion()
    {
        return Version.Companion.getInstance().toString();
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
        interstitialAdListener = null;

        rewardedAd = null;
        rewardedAdListener = null;
    }

    //region MAX Interstitial Adapter Methods

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        String zoneId = parameters.getThirdPartyAdPlacementId();

        log( "Loading interstitial ad: " + zoneId );

        interstitialAdListener = new InterstitialAdListener( listener );
        MaioRequest request = new MaioRequest( zoneId, parameters.isTesting(), "" );
        activity.runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                interstitialAd = Interstitial.loadAd( request, getContext( activity ), interstitialAdListener );
            }
        } );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad: " + parameters.getThirdPartyAdPlacementId() );

        if ( interstitialAd == null )
        {
            log( "Unable to show interstitial - ad not ready" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Interstitial ad not ready" ) );

            return;
        }

        interstitialAd.show(getContext( activity ), interstitialAdListener);
    }

    //endregion

    //region MAX Rewarded Adapter Methods

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        String zoneId = parameters.getThirdPartyAdPlacementId();

        log( "Loading rewarded ad: " + zoneId );

        rewardedAdListener = new RewardedAdListener( listener );
        MaioRequest request = new MaioRequest( zoneId, parameters.isTesting(), "" );
        activity.runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                rewardedAd = Rewarded.loadAd( request, getContext( activity ), rewardedAdListener );
            }
        } );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad: " + parameters.getThirdPartyAdPlacementId() );

        if ( rewardedAd == null )
        {
            log( "Unable to show rewarded ad - ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Rewarded ad not ready" ) );

            return;
        }

        rewardedAd.show( getContext( activity ), rewardedAdListener );
    }

    //endregion

    //region Helper Methods

    private MaxAdapterError toMaxError(final int maioErrorCode)
    {
        // Maio's error codes are 5 digits but we need to check the first 3 digits to determine the error
        int maioError = maioErrorCode / 100;
        String maioErrorMessage = "Unknown";
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;

        switch ( maioError )
        {
            case 101:
                maioErrorMessage = "NoNetwork";
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case 102:
                maioErrorMessage = "NetworkTimeout";
                adapterError = MaxAdapterError.TIMEOUT;
                break;
            case 103:
                maioErrorMessage = "AbortedDownload";
                adapterError = MaxAdapterError.AD_NOT_READY;
                break;
            case 104:
                maioErrorMessage = "InvalidResponse";
                adapterError = MaxAdapterError.SERVER_ERROR;
                break;
            case 105:
                maioErrorMessage = "ZoneNotFound";
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case 106:
                maioErrorMessage = "UnavailableZone";
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case 107:
                maioErrorMessage = "NoFill";
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case 108:
                maioErrorMessage = "NullArgMaioRequest";
                adapterError = MaxAdapterError.BAD_REQUEST;
                break;
            case 109:
                maioErrorMessage = "DiskSpaceNotEnough";
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case 110:
                maioErrorMessage = "UnsupportedOsVer";
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case 201:
                maioErrorMessage = "Expired";
                adapterError = MaxAdapterError.AD_EXPIRED;
                break;
            case 202:
                maioErrorMessage = "NotReadyYet";
                adapterError = MaxAdapterError.AD_NOT_READY;
                break;
            case 203:
                maioErrorMessage = "AlreadyShown";
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case 204:
                maioErrorMessage = "FailedPlayback";
                adapterError = MaxAdapterError.WEBVIEW_ERROR;
                break;
            case 205:
                maioErrorMessage = "NullArgViewContext";
                adapterError = MaxAdapterError.MISSING_ACTIVITY;
                break;
        }

        return new MaxAdapterError( adapterError,
                                    maioErrorCode,
                                    maioErrorMessage );
    }

    private Context getContext(@Nullable Activity activity)
    {
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
    }

    //endregion

    private class InterstitialAdListener
            implements IInterstitialLoadCallback, IInterstitialShowCallback
    {
        private final MaxInterstitialAdapterListener listener;

        public InterstitialAdListener(final MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void loaded(@NonNull Interstitial interstitial)
        {
            log( "Interstitial ad loaded" );
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void failed(@NonNull Interstitial interstitial, int errorCode)
        {
            MaxAdapterError adapterError = toMaxError( errorCode );

            // Maio's error code will be 1XXXX for load errors and 2XXXX for display errors.
            if ( 10000 <= errorCode && errorCode < 20000 )
            {
                // Failed to load
                log( "Interstitial ad failed to load with error (" + adapterError + ")" );
                listener.onInterstitialAdLoadFailed( adapterError );
            }
            else if ( 20000 <= errorCode && errorCode < 30000 )
            {
                // Failed to show
                log( "Interstitial ad failed to display with error (" + adapterError + ")" );
                listener.onInterstitialAdDisplayFailed( adapterError );
            }
            else
            {
                // Unknown error code
                log( "Interstitial ad failed to load or show due to an unknown error (" + adapterError + ")" );
                listener.onInterstitialAdLoadFailed( adapterError );
            }
        }

        @Override
        public void opened(@NonNull Interstitial interstitial)
        {
            log( "Interstitial ad displayed" );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void clicked(@NonNull Interstitial interstitial)
        {
            log( "Interstitial ad clicked" );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void closed(@NonNull Interstitial interstitial)
        {
            log( "Interstitial ad hidden" );
            listener.onInterstitialAdHidden();
        }
    }

    private class RewardedAdListener
            implements IRewardedLoadCallback, IRewardedShowCallback
    {
        private final MaxRewardedAdapterListener listener;

        private boolean hasGrantedReward;

        public RewardedAdListener(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void loaded(@NonNull Rewarded rewarded)
        {
            log( "Rewarded ad loaded" );
            listener.onRewardedAdLoaded();
        }

        @Override
        public void failed(@NonNull Rewarded rewarded, int errorCode) {
            MaxAdapterError adapterError = toMaxError( errorCode );

            // Maio's error code will be 1XXXX for load errors and 2XXXX for display errors.
            if ( 10000 <= errorCode && errorCode < 20000 )
            {
                // Failed to load
                log( "Rewarded ad failed to load with error (" + adapterError + ")" );
                listener.onRewardedAdLoadFailed( adapterError );
            }
            else if ( 20000 <= errorCode && errorCode < 30000 )
            {
                // Failed to show
                log( "Rewarded ad failed to display with error (" + adapterError + ")" );
                listener.onRewardedAdDisplayFailed( adapterError );
            }
            else
            {
                // Unknown error code
                log( "Rewarded ad failed to load or show due to an unknown error (" + adapterError + ")" );
                listener.onRewardedAdLoadFailed( adapterError );
            }

        }

        @Override
        public void opened(@NonNull Rewarded rewarded)
        {
            log( "Rewarded ad displayed" );
            listener.onRewardedAdDisplayed();
        }

        @Override
        public void clicked(@NonNull Rewarded rewarded)
        {
            log( "Rewarded ad clicked" );
            listener.onRewardedAdClicked();
        }

        @Override
        public void rewarded(@NonNull Rewarded rewarded, @NonNull RewardData rewardData)
        {
            log( "Rewarded ad should grant reward" );
            hasGrantedReward = true;
        }

        @Override
        public void closed(@NonNull Rewarded rewarded)
        {
            log( "Rewarded ad hidden" );

            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            listener.onRewardedAdHidden();
        }
    }
}
