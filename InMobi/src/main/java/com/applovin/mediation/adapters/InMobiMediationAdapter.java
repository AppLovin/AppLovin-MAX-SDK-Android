package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.WindowManager;
import android.widget.LinearLayout;

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
import com.applovin.mediation.adapters.inmobi.BuildConfig;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.inmobi.ads.AdMetaInfo;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiBanner;
import com.inmobi.ads.InMobiInterstitial;
import com.inmobi.ads.listeners.BannerAdEventListener;
import com.inmobi.ads.listeners.InterstitialAdEventListener;
import com.inmobi.sdk.InMobiSdk;
import com.inmobi.sdk.SdkInitializationListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Created by Thomas So on February 10 2019
 */
public class InMobiMediationAdapter
        extends MediationAdapterBase
        implements MaxAdViewAdapter, MaxInterstitialAdapter, MaxRewardedAdapter, MaxSignalProvider
{
    private static final String KEY_PARTNER_GDPR_CONSENT = "partner_gdpr_consent_available";
    private static final String KEY_PARTNER_GDPR_APPLIES = "partner_gdpr_applies";

    private static final AtomicBoolean        INITIALIZED = new AtomicBoolean();
    private static       InitializationStatus status;

    private InMobiBanner       adView;
    private InMobiInterstitial interstitialAd;
    private InMobiInterstitial rewardedAd;

    // Explicit default constructor declaration
    public InMobiMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public String getSdkVersion()
    {
        return InMobiSdk.getVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        if ( !InMobiSdk.isSDKInitialized() )
        {
            callback.onSignalCollectionFailed( "InMobi SDK initialization failed." );
            return;
        }

        String signal = InMobiSdk.getToken( getExtras( parameters ), null );
        callback.onSignalCollected( signal );
    }

    @Override
    public void onDestroy()
    {
        if ( adView != null )
        {
            adView.destroy();
            adView = null;
        }

        interstitialAd = null;
        rewardedAd = null;
    }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( INITIALIZED.compareAndSet( false, true ) )
        {
            final String accountId = parameters.getServerParameters().getString( "account_id" );
            log( "Initializing InMobi SDK with account id: " + accountId + "..." );

            // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
            Context context = ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();

            status = InitializationStatus.INITIALIZING;

            JSONObject consentObject = getConsentJSONObject( parameters );
            InMobiSdk.init( context, accountId, consentObject, new SdkInitializationListener()
            {
                @Override
                public void onInitializationComplete(@Nullable final Error error)
                {
                    if ( error != null )
                    {
                        log( "InMobi SDK initialization failed with error: " + error.getMessage() );

                        status = InitializationStatus.INITIALIZED_FAILURE;
                        onCompletionListener.onCompletion( status, error.getMessage() );
                    }
                    else
                    {
                        log( "InMobi SDK successfully initialized." );

                        status = InitializationStatus.INITIALIZED_SUCCESS;
                        onCompletionListener.onCompletion( status, null );
                    }
                }
            } );

            InMobiSdk.LogLevel logLevel = parameters.isTesting() ? InMobiSdk.LogLevel.DEBUG : InMobiSdk.LogLevel.ERROR;
            InMobiSdk.setLogLevel( logLevel );
        }
        else
        {
            log( "InMobi SDK already initialized" );

            onCompletionListener.onCompletion( status, null );
        }
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        if ( !InMobiSdk.isSDKInitialized() )
        {
            log( "InMobi SDK not successfully initialized: failing " + adFormat.getLabel() + " ad load..." );
            listener.onAdViewAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        final long placementId = Long.parseLong( parameters.getThirdPartyAdPlacementId() );
        log( "Loading " + adFormat.getLabel() + " AdView ad for placement: " + placementId + "..." );

        adView = new InMobiBanner( activity, placementId );
        adView.setExtras( getExtras( parameters ) );
        adView.setAnimationType( InMobiBanner.AnimationType.ANIMATION_OFF );
        adView.setEnableAutoRefresh( false ); // By default, refreshes every 60 seconds
        adView.setListener( new AdViewListener( listener ) );

        // Update GDPR states
        InMobiSdk.setPartnerGDPRConsent( getConsentJSONObject( parameters ) );

        DisplayMetrics displayMetrics = new DisplayMetrics();
        WindowManager windowManager = (WindowManager) activity.getSystemService( Context.WINDOW_SERVICE );
        Display display = windowManager.getDefaultDisplay();
        display.getMetrics( displayMetrics );

        final int width, height;
        if ( adFormat == MaxAdFormat.BANNER )
        {
            width = 320;
            height = 50;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            width = 728;
            height = 90;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            width = 300;
            height = 250;
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported ad format: " + adFormat );
        }

        adView.setLayoutParams( new LinearLayout.LayoutParams( Math.round( width * displayMetrics.density ),
                                                               Math.round( height * displayMetrics.density ) ) );

        final String bidResponse = parameters.getBidResponse();
        if ( !TextUtils.isEmpty( bidResponse ) )
        {
            adView.load( bidResponse.getBytes() );
        }
        else
        {
            adView.load();
        }
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        if ( !InMobiSdk.isSDKInitialized() )
        {
            log( "InMobi SDK not successfully initialized: failing interstitial ad load..." );
            listener.onInterstitialAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        final long placementId = Long.parseLong( parameters.getThirdPartyAdPlacementId() );
        log( "Loading interstitial ad for placement: " + placementId + "..." );

        interstitialAd = createFullscreenAd( placementId, parameters, new InterstitialListener( listener ), activity );

        final String bidResponse = parameters.getBidResponse();
        if ( !TextUtils.isEmpty( bidResponse ) )
        {
            interstitialAd.load( bidResponse.getBytes() );
        }
        else
        {
            interstitialAd.load();
        }
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad..." );

        final boolean success = showFullscreenAd( interstitialAd );
        if ( !success )
        {
            log( "Interstitial ad not ready" );
            listener.onInterstitialAdDisplayFailed( MaxAdapterError.AD_NOT_READY );
        }
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        if ( !InMobiSdk.isSDKInitialized() )
        {
            log( "InMobi SDK not successfully initialized: failing rewarded ad load..." );
            listener.onRewardedAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        final long placementId = Long.parseLong( parameters.getThirdPartyAdPlacementId() );
        log( "Loading rewarded ad for placement: " + placementId + "..." );

        rewardedAd = createFullscreenAd( placementId, parameters, new RewardedAdListener( listener ), activity );

        final String bidResponse = parameters.getBidResponse();
        if ( !TextUtils.isEmpty( bidResponse ) )
        {
            rewardedAd.load( bidResponse.getBytes() );
        }
        else
        {
            rewardedAd.load();
        }
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad..." );

        // Configure userReward from server.
        configureReward( parameters );

        final boolean success = showFullscreenAd( rewardedAd );
        if ( !success )
        {
            log( "Rewarded ad not ready" );
            listener.onRewardedAdDisplayFailed( MaxAdapterError.AD_NOT_READY );
        }
    }

    //region Helper Methods

    private InMobiInterstitial createFullscreenAd(long placementId, MaxAdapterResponseParameters parameters, InterstitialAdEventListener listener, Activity activity)
    {
        InMobiInterstitial interstitial = new InMobiInterstitial( activity, placementId, listener );
        interstitial.setExtras( getExtras( parameters ) );

        // Update GDPR states
        InMobiSdk.setPartnerGDPRConsent( getConsentJSONObject( parameters ) );

        return interstitial;
    }

    private boolean showFullscreenAd(InMobiInterstitial interstitial)
    {
        if ( interstitial.isReady() )
        {
            interstitial.show();

            return true;
        }
        else
        {
            return false;
        }
    }

    private JSONObject getConsentJSONObject(MaxAdapterParameters parameters)
    {
        JSONObject consentObject = new JSONObject();

        try
        {
            if ( getWrappingSdk().getConfiguration().getConsentDialogState() == AppLovinSdkConfiguration.ConsentDialogState.APPLIES )
            {
                consentObject.put( KEY_PARTNER_GDPR_APPLIES, 1 );

                Boolean hasUserConsent = getPrivacySetting( "hasUserConsent", parameters );
                if ( hasUserConsent != null )
                {
                    consentObject.put( KEY_PARTNER_GDPR_CONSENT, hasUserConsent );
                }
            }
            else if ( getWrappingSdk().getConfiguration().getConsentDialogState() == AppLovinSdkConfiguration.ConsentDialogState.DOES_NOT_APPLY )
            {
                consentObject.put( KEY_PARTNER_GDPR_APPLIES, 0 );
            }
        }
        catch ( JSONException ex )
        {
            log( "Failed to create consent JSON object", ex );
        }

        return consentObject;
    }

    private Map<String, String> getExtras(MaxAdapterParameters parameters)
    {
        Map<String, String> extras = new HashMap<>( 3 );
        extras.put( "tp", "c_applovin" );
        extras.put( "tp-ver", AppLovinSdk.VERSION );

        Boolean isAgeRestrictedUser = getPrivacySetting( "isAgeRestrictedUser", parameters );
        if ( isAgeRestrictedUser != null )
        {
            extras.put( "coppa", isAgeRestrictedUser ? "1" : "0" );
        }

        return extras;
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

    private static MaxAdapterError toMaxError(InMobiAdRequestStatus inMobiError)
    {
        final InMobiAdRequestStatus.StatusCode inMobiErrorCode = inMobiError.getStatusCode();
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( inMobiErrorCode )
        {
            case NO_ERROR:
                adapterError = MaxAdapterError.UNSPECIFIED;
                break;
            case NETWORK_UNREACHABLE:
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case NO_FILL:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case REQUEST_INVALID:
                adapterError = MaxAdapterError.BAD_REQUEST;
                break;
            case REQUEST_TIMED_OUT:
                adapterError = MaxAdapterError.TIMEOUT;
                break;
            case INTERNAL_ERROR:
            case GDPR_COMPLIANCE_ENFORCED:
            case GET_SIGNALS_CALLED_WHILE_LOADING:
            case CALLED_FROM_WRONG_THREAD:
            case LOW_MEMORY:
            case MISSING_REQUIRED_DEPENDENCIES:
            case INVALID_RESPONSE_IN_LOAD:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case SERVER_ERROR:
                adapterError = MaxAdapterError.SERVER_ERROR;
                break;
            case AD_ACTIVE:
            case EARLY_REFRESH_REQUEST:
            case REPETITIVE_LOAD:
            case LOAD_WITH_RESPONSE_CALLED_WHILE_LOADING:
            case REQUEST_PENDING:
                adapterError = MaxAdapterError.INVALID_LOAD_STATE;
                break;
            case AD_NO_LONGER_AVAILABLE:
                adapterError = MaxAdapterError.AD_EXPIRED;
                break;
            case MONETIZATION_DISABLED:
            case CONFIGURATION_ERROR:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), inMobiErrorCode.ordinal(), inMobiError.getMessage() );
    }

    //endregion

    private class AdViewListener
            extends BannerAdEventListener
    {
        final MaxAdViewAdapterListener listener;

        AdViewListener(MaxAdViewAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoadSucceeded(@NonNull final InMobiBanner inMobiBanner, @NonNull final AdMetaInfo adMetaInfo)
        {
            log( "AdView loaded" );

            // Passing extra info such as creative id supported in 9.15.0+
            if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( adMetaInfo.getCreativeID() ) )
            {
                Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", adMetaInfo.getCreativeID() );

                listener.onAdViewAdLoaded( inMobiBanner, extraInfo );
            }
            else
            {
                listener.onAdViewAdLoaded( inMobiBanner );
            }

            // InMobi track impressions in the `onAdLoadSucceeded()` callback
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdLoadFailed(@NonNull final InMobiBanner inMobiBanner, final InMobiAdRequestStatus inMobiAdRequestStatus)
        {
            log( "AdView failed to load with error code " + inMobiAdRequestStatus.getStatusCode() + " and message: " + inMobiAdRequestStatus.getMessage() );

            MaxAdapterError adapterError = toMaxError( inMobiAdRequestStatus );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onAdDisplayed(@NonNull final InMobiBanner inMobiBanner)
        {
            log( "AdView expanded" );
            listener.onAdViewAdExpanded();
        }

        @Override
        public void onAdDismissed(@NonNull final InMobiBanner inMobiBanner)
        {
            log( "AdView collapsed" );
            listener.onAdViewAdCollapsed();
        }

        @Override
        public void onAdClicked(@NonNull final InMobiBanner inMobiBanner, final Map<Object, Object> map)
        {
            // NOTE: InMobi's SDK does not fire this callback on click, rather it fires the onAdDisplayed() callback instead
            log( "AdView clicked" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onUserLeftApplication(@NonNull final InMobiBanner inMobiBanner)
        {
            log( "AdView will leave application" );
        }
    }

    private class InterstitialListener
            extends InterstitialAdEventListener
    {
        final MaxInterstitialAdapterListener listener;

        InterstitialListener(MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdFetchSuccessful(@NonNull final InMobiInterstitial inMobiInterstitial, @NonNull final AdMetaInfo adMetaInfo)
        {
            log( "Interstitial request succeeded" );
        }

        @Override
        public void onAdLoadSucceeded(@NonNull final InMobiInterstitial inMobiInterstitial, @NonNull final AdMetaInfo adMetaInfo)
        {
            log( "Interstitial loaded" );

            // Passing extra info such as creative id supported in 9.15.0+
            if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( adMetaInfo.getCreativeID() ) )
            {
                Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", adMetaInfo.getCreativeID() );

                listener.onInterstitialAdLoaded( extraInfo );
            }
            else
            {
                listener.onInterstitialAdLoaded();
            }
        }

        @Override
        public void onAdLoadFailed(@NonNull final InMobiInterstitial inMobiInterstitial, final InMobiAdRequestStatus inMobiAdRequestStatus)
        {
            log( "Interstitial failed to load with error code " + inMobiAdRequestStatus.getStatusCode() + " and message: " + inMobiAdRequestStatus.getMessage() );

            MaxAdapterError adapterError = toMaxError( inMobiAdRequestStatus );
            listener.onInterstitialAdLoadFailed( adapterError );
        }

        @Override
        public void onAdDisplayFailed(@NonNull final InMobiInterstitial inMobiInterstitial)
        {
            log( "Interstitial failed to display" );
            listener.onInterstitialAdDisplayFailed( MaxAdapterError.UNSPECIFIED );
        }

        @Override
        public void onAdWillDisplay(@NonNull final InMobiInterstitial inMobiInterstitial)
        {
            log( "Interstitial will show" );
        }

        @Override
        public void onAdDisplayed(@NonNull final InMobiInterstitial inMobiInterstitial, @NonNull final AdMetaInfo adMetaInfo)
        {
            log( "Interstitial did show" );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdClicked(@NonNull final InMobiInterstitial inMobiInterstitial, final Map<Object, Object> map)
        {
            log( "Interstitial clicked" );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onAdDismissed(@NonNull final InMobiInterstitial inMobiInterstitial)
        {
            log( "Interstitial hidden" );
            listener.onInterstitialAdHidden();
        }

        @Override
        public void onUserLeftApplication(@NonNull final InMobiInterstitial inMobiInterstitial)
        {
            log( "Interstitial will leave application" );
        }
    }

    private class RewardedAdListener
            extends InterstitialAdEventListener
    {
        final MaxRewardedAdapterListener listener;

        private boolean hasGrantedReward;

        RewardedAdListener(MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdFetchSuccessful(@NonNull final InMobiInterstitial inMobiInterstitial, @NonNull final AdMetaInfo adMetaInfo)
        {
            log( "Rewarded ad request succeeded" );
        }

        @Override
        public void onAdLoadSucceeded(@NonNull final InMobiInterstitial inMobiInterstitial, @NonNull final AdMetaInfo adMetaInfo)
        {
            log( "Rewarded ad loaded" );

            // Passing extra info such as creative id supported in 9.15.0+
            if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( adMetaInfo.getCreativeID() ) )
            {
                Bundle extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", adMetaInfo.getCreativeID() );

                listener.onRewardedAdLoaded( extraInfo );
            }
            else
            {
                listener.onRewardedAdLoaded();
            }
        }

        @Override
        public void onAdLoadFailed(@NonNull final InMobiInterstitial inMobiInterstitial, final InMobiAdRequestStatus inMobiAdRequestStatus)
        {
            log( "Rewarded ad failed to load with error code " + inMobiAdRequestStatus.getStatusCode() + " and message: " + inMobiAdRequestStatus.getMessage() );

            MaxAdapterError adapterError = toMaxError( inMobiAdRequestStatus );
            listener.onRewardedAdLoadFailed( adapterError );
        }

        @Override
        public void onAdDisplayFailed(@NonNull final InMobiInterstitial inMobiInterstitial)
        {
            log( "Rewarded ad failed to display" );
            listener.onRewardedAdDisplayFailed( MaxAdapterError.UNSPECIFIED );
        }

        @Override
        public void onAdWillDisplay(@NonNull final InMobiInterstitial inMobiInterstitial)
        {
            log( "Rewarded ad did show" );
        }

        @Override
        public void onAdDisplayed(@NonNull final InMobiInterstitial inMobiInterstitial, @NonNull final AdMetaInfo adMetaInfo)
        {
            log( "Rewarded ad did show" );
            listener.onRewardedAdDisplayed();
            listener.onRewardedAdVideoStarted();
        }

        @Override
        public void onAdClicked(@NonNull final InMobiInterstitial inMobiInterstitial, final Map<Object, Object> map)
        {
            log( "Rewarded ad clicked" );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onAdDismissed(@NonNull final InMobiInterstitial inMobiInterstitial)
        {
            log( "Rewarded ad hidden" );

            listener.onRewardedAdVideoCompleted();

            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            listener.onRewardedAdHidden();
        }

        @Override
        public void onUserLeftApplication(@NonNull final InMobiInterstitial inMobiInterstitial)
        {
            log( "Rewarded ad will leave application" );
        }

        @Override
        public void onRewardsUnlocked(@NonNull final InMobiInterstitial inMobiInterstitial, final Map<Object, Object> map)
        {
            log( "Rewarded ad granted reward" );
            hasGrantedReward = true;
        }
    }
}
