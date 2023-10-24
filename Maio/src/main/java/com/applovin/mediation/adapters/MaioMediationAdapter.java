package com.applovin.mediation.adapters;

import android.app.Activity;

import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapters.maio.BuildConfig;
import com.applovin.sdk.AppLovinSdk;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import jp.maio.sdk.android.FailNotificationReason;
import jp.maio.sdk.android.MaioAds;
import jp.maio.sdk.android.MaioAdsListenerInterface;

/**
 * Created by Harry Arakkal on July 3 2019
 */
public class MaioMediationAdapter
        extends MediationAdapterBase
        implements MaxInterstitialAdapter, MaxRewardedAdapter
{
    private static final MaioMediationAdapterRouter ROUTER;
    private static final AtomicBoolean              INITIALIZED = new AtomicBoolean();

    private static InitializationStatus initializationStatus;
    private static Set<String>          zoneIds;
    private        String               zoneId;

    static
    {
        if ( AppLovinSdk.VERSION_CODE >= 90802 )
        {
            ROUTER = (MaioMediationAdapterRouter) MediationAdapterRouter.getSharedInstance( MaioMediationAdapterRouter.class );
        }
        else
        {
            ROUTER = new MaioMediationAdapterRouter();
        }
    }

    // Explicit default constructor declaration
    public MaioMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public String getSdkVersion()
    {
        return MaioAds.getSdkVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        ROUTER.removeAdapter( this, zoneId );
    }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        // NOTE: `activity` can only be null in 11.1.0+
        if ( activity == null )
        {
            onCompletionListener.onCompletion( InitializationStatus.INITIALIZED_FAILURE, "Activity context required to initialize" );
            return;
        }

        if ( INITIALIZED.compareAndSet( false, true ) )
        {
            final String mediaId = parameters.getServerParameters().getString( "media_id" );
            log( "Initializing Maio SDK with media id: " + mediaId + "..." );

            ROUTER.completionListener = onCompletionListener;
            initializationStatus = InitializationStatus.INITIALIZING;

            zoneIds = new HashSet<>();
            if ( parameters.getServerParameters().containsKey( "zone_ids" ) )
            {
                zoneIds.addAll( parameters.getServerParameters().getStringArrayList( "zone_ids" ) );
            }

            if ( parameters.isTesting() )
            {
                MaioAds.setAdTestMode( true );
            }

            // NOTE: Unlike iOS, Maio will call `onInitialized()` in the event of a failure.
            MaioAds.init( activity, mediaId, ROUTER );
        }
        else
        {
            log( "Maio already Initialized" );

            onCompletionListener.onCompletion( initializationStatus, null );
        }
    }

    // MARK: Interstitial Ad functions

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        zoneId = parameters.getThirdPartyAdPlacementId();

        log( "Loading interstitial ad: " + zoneId + "..." );

        ROUTER.addInterstitialAdapter( this, listener, zoneId );

        if ( MaioAds.canShow( zoneId ) )
        {
            ROUTER.onAdLoaded( zoneId );
        }
        // NOTE: iOS does not have this check, checking of "can show" will NOT callback error on Android
        else if ( !zoneIds.contains( zoneId ) )
        {
            log( "Ad failed to load. zone id = " + zoneId + " is invalid" );
            ROUTER.onAdLoadFailed( zoneId, MaxAdapterError.INVALID_CONFIGURATION );
        }
        // Maio might lose out on the first impression.
        else
        {
            log( "Ad failed to load for this zone: " + zoneId );
            ROUTER.onAdLoadFailed( zoneId, MaxAdapterError.NO_FILL );
        }
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad " + zoneId );

        ROUTER.addShowingAdapter( this );

        if ( MaioAds.canShow( zoneId ) )
        {
            MaioAds.show( zoneId );
        }
        else
        {
            log( "Interstitial not ready" );
            ROUTER.onAdDisplayFailed( zoneId, new MaxAdapterError( -4205, "Ad Display Failed" ) );
        }
    }

    // MARK: Rewarded Ad functions

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        zoneId = parameters.getThirdPartyAdPlacementId();

        log( "Loading rewarded ad for zone id: " + zoneId + "..." );

        ROUTER.addRewardedAdapter( this, listener, zoneId );

        if ( MaioAds.canShow( zoneId ) )
        {
            ROUTER.onAdLoaded( zoneId );
        }
        // NOTE: iOS does not have this check, checking of "can show" will NOT callback error on Android
        else if ( !zoneIds.contains( zoneId ) )
        {
            log( "Ad failed to load. zone id " + zoneId + " is invalid" );
            ROUTER.onAdLoadFailed( zoneId, MaxAdapterError.INVALID_CONFIGURATION );
        }
        // Maio might lose out on the first impression.
        else
        {
            log( "Ad failed to load for this zone: " + zoneId );
            ROUTER.onAdLoadFailed( zoneId, MaxAdapterError.NO_FILL );
        }
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad " + zoneId );

        ROUTER.addShowingAdapter( this );

        if ( MaioAds.canShow( zoneId ) )
        {
            // Configure reward from server.
            configureReward( parameters );
            MaioAds.show( zoneId );
        }
        else
        {
            log( "Rewarded ad not ready" );
            ROUTER.onAdDisplayFailed( zoneId, new MaxAdapterError( -4205, "Ad Display Failed" ) );
        }
    }

    //MARK: Router Class

    private static class MaioMediationAdapterRouter
            extends MediationAdapterRouter
            implements MaioAdsListenerInterface
    {
        private static final AtomicBoolean isShowingAd      = new AtomicBoolean();
        private              boolean       hasGrantedReward = false;

        OnCompletionListener completionListener;

        // MARK: Listener Functions

        @Override
        public void onInitialized()
        {
            log( "Maio SDK Initialized" );

            if ( completionListener != null )
            {
                initializationStatus = InitializationStatus.INITIALIZED_UNKNOWN;

                completionListener.onCompletion( initializationStatus, null );
                completionListener = null;
            }
        }

        // Does not refer to a specific ad, but if ads can show in general.
        @Override
        public void onChangedCanShow(String zoneId, boolean newValue)
        {
            if ( newValue )
            {
                log( "Maio can show ads: " + zoneId );
            }
            else
            {
                log( "Maio cannot show ads: " + zoneId );
            }
        }

        @Override
        public void onOpenAd(String zoneId)
        {
            log( "Ad video will start: " + zoneId );
            onAdDisplayed( zoneId );
        }

        @Override
        public void onStartedAd(String zoneId)
        {
            log( "Ad video started: " + zoneId );
            onRewardedAdVideoStarted( zoneId );
        }

        @Override
        public void onFinishedAd(int playtime, boolean skipped, int duration, String zoneId)
        {
            log( "Did finish ad = " + zoneId + " playtime = " + playtime + " skipped = " + skipped + " duration of ad = " + duration );

            if ( !skipped )
            {
                hasGrantedReward = true;
            }

            onRewardedAdVideoCompleted( zoneId );
        }

        @Override
        public void onClickedAd(String zoneId)
        {
            log( "Ad clicked: " + zoneId );
            onAdClicked( zoneId );
        }

        @Override
        public void onClosedAd(String zoneId)
        {
            log( "Ad closed: " + zoneId );

            if ( hasGrantedReward || shouldAlwaysRewardUser( zoneId ) )
            {
                MaxReward reward = getReward( zoneId );
                log( "Rewarded ad user with reward: " + reward );
                onUserRewarded( zoneId, reward );

                hasGrantedReward = false;
            }

            isShowingAd.set( false );

            onAdHidden( zoneId );
        }

        @Override
        public void onFailed(FailNotificationReason reason, String zoneId)
        {
            if ( isShowingAd.compareAndSet( true, false ) )
            {
                MaxAdapterError error = new MaxAdapterError( -4205, "Ad Display Failed", reason.ordinal(), reason.name() );
                log( "Ad failed to display with Maio reason: " + reason + " Max error: " + error );
                onAdDisplayFailed( zoneId, error );
            }
            else
            {
                MaxAdapterError error = toMaxError( reason );
                log( "Ad failed to load with Maio reason: " + reason + " Max error: " + error );
                onAdLoadFailed( zoneId, error );
            }
        }

        @Override
        void initialize(MaxAdapterInitializationParameters parameters, Activity activity, OnCompletionListener onCompletionListener) { }

        // MARK: Overrides for Ad Show State Management

        @Override
        public void addShowingAdapter(final MaxAdapter adapter)
        {
            super.addShowingAdapter( adapter );

            // Maio uses the same callback for [AD LOAD FAILED] and [AD DISPLAY FAILED] callbacks
            isShowingAd.set( true );
        }

        //MARK: Helper function

        private static MaxAdapterError toMaxError(FailNotificationReason maioError)
        {
            MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
            switch ( maioError )
            {
                case RESPONSE:
                    adapterError = MaxAdapterError.SERVER_ERROR;
                    break;
                case NETWORK_NOT_READY:
                    adapterError = MaxAdapterError.NO_CONNECTION;
                    break;
                case NETWORK:
                    adapterError = MaxAdapterError.TIMEOUT;
                    break;
                case UNKNOWN:
                    adapterError = MaxAdapterError.UNSPECIFIED;
                    break;
                case AD_STOCK_OUT:
                    adapterError = MaxAdapterError.NO_FILL;
                    break;
                case VIDEO:
                    adapterError = MaxAdapterError.INTERNAL_ERROR;
                    break;
            }

            return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), maioError.ordinal(), maioError.name() );
        }
    }
}
