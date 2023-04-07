package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.DisplayMetrics;
import android.view.Display;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;

import com.applovin.impl.sdk.utils.BundleUtils;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.MaxSignalProvider;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxNativeAdAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.adapters.inmobi.BuildConfig;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkUtils;
import com.inmobi.ads.AdMetaInfo;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiBanner;
import com.inmobi.ads.InMobiInterstitial;
import com.inmobi.ads.InMobiNative;
import com.inmobi.ads.listeners.BannerAdEventListener;
import com.inmobi.ads.listeners.InterstitialAdEventListener;
import com.inmobi.ads.listeners.NativeAdEventListener;
import com.inmobi.sdk.InMobiSdk;
import com.inmobi.sdk.SdkInitializationListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
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

    private static final int DEFAULT_IMAGE_TASK_TIMEOUT_SECONDS = 5;

    private static final AtomicBoolean        INITIALIZED = new AtomicBoolean();
    private static       InitializationStatus status;

    private InMobiBanner       adView;
    private InMobiInterstitial interstitialAd;
    private InMobiInterstitial rewardedAd;
    private InMobiNative       nativeAd;

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

        updateAgeRestrictedUser( parameters );
        InMobiSdk.setPartnerGDPRConsent( getConsentJSONObject( parameters ) );

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

        if ( nativeAd != null )
        {
            nativeAd.destroy();
            nativeAd = null;
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

            Context context = getContext( activity );

            status = InitializationStatus.INITIALIZING;

            updateAgeRestrictedUser( parameters );

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

    //region MaxAdViewAdAdapter Methods

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final long placementId = Long.parseLong( parameters.getThirdPartyAdPlacementId() );
        final boolean isNative = parameters.getServerParameters().getBoolean( "is_native" );
        log( "Loading" + ( isNative ? " native " : " " ) + adFormat.getLabel() + " AdView ad for placement: " + placementId + "..." );

        if ( !InMobiSdk.isSDKInitialized() )
        {
            log( "InMobi SDK not successfully initialized: failing " + adFormat.getLabel() + " ad load..." );
            listener.onAdViewAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateAgeRestrictedUser( parameters );
        InMobiSdk.setPartnerGDPRConsent( getConsentJSONObject( parameters ) );

        final String bidResponse = parameters.getBidResponse();
        final boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );

        final Context context = getContext( activity );
        if ( isNative )
        {
            nativeAd = new InMobiNative( context,
                                         placementId,
                                         new NativeAdViewListener( parameters, adFormat, activity, listener ) );
            nativeAd.setExtras( getExtras( parameters ) );

            if ( isBiddingAd )
            {
                nativeAd.load( bidResponse.getBytes() );
            }
            else
            {
                nativeAd.load();
            }
        }
        else
        {
            adView = new InMobiBanner( context, placementId );
            adView.setExtras( getExtras( parameters ) );
            adView.setAnimationType( InMobiBanner.AnimationType.ANIMATION_OFF );
            adView.setEnableAutoRefresh( false ); // By default, refreshes every 60 seconds
            adView.setListener( new AdViewListener( listener ) );

            DisplayMetrics displayMetrics = new DisplayMetrics();
            WindowManager windowManager = (WindowManager) context.getSystemService( Context.WINDOW_SERVICE );
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

            if ( isBiddingAd )
            {
                adView.load( bidResponse.getBytes() );
            }
            else
            {
                adView.load();
            }
        }
    }

    //endregion

    //region MaxInterstitialAdAdapter Methods

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final long placementId = Long.parseLong( parameters.getThirdPartyAdPlacementId() );
        log( "Loading interstitial ad for placement: " + placementId + "..." );

        if ( !InMobiSdk.isSDKInitialized() )
        {
            log( "InMobi SDK not successfully initialized: failing interstitial ad load..." );
            listener.onInterstitialAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateAgeRestrictedUser( parameters );

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
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Interstitial ad not ready" ) );
        }
    }

    //endregion

    //region MaxRewardedAdAdapter Methods

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final long placementId = Long.parseLong( parameters.getThirdPartyAdPlacementId() );
        log( "Loading rewarded ad for placement: " + placementId + "..." );

        if ( !InMobiSdk.isSDKInitialized() )
        {
            log( "InMobi SDK not successfully initialized: failing rewarded ad load..." );
            listener.onRewardedAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateAgeRestrictedUser( parameters );

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
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Rewarded ad not ready" ) );
        }
    }

    //endregion

    //region MaxNativeAdAdapter Methods

    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        if ( !InMobiSdk.isSDKInitialized() )
        {
            log( "InMobi SDK not successfully initialized: failing native ad load..." );
            listener.onNativeAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateAgeRestrictedUser( parameters );

        final long placementId = Long.parseLong( parameters.getThirdPartyAdPlacementId() );

        final String bidResponse = parameters.getBidResponse();
        final boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + "native ad for placement: " + placementId + "..." );

        InMobiSdk.setPartnerGDPRConsent( getConsentJSONObject( parameters ) );

        final Context context = getContext( activity );
        nativeAd = new InMobiNative( context,
                                     placementId,
                                     new NativeAdListener( parameters, context, listener ) );

        nativeAd.setExtras( getExtras( parameters ) );

        if ( isBiddingAd )
        {
            nativeAd.load( bidResponse.getBytes() );
        }
        else
        {
            nativeAd.load();
        }
    }

    //endregion

    //region Helper Methods

    private InMobiInterstitial createFullscreenAd(long placementId, MaxAdapterResponseParameters parameters, InterstitialAdEventListener listener, Activity activity)
    {
        InMobiInterstitial interstitial = new InMobiInterstitial( activity, placementId, listener );
        interstitial.setExtras( getExtras( parameters ) );

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
            Boolean hasUserConsent = getPrivacySetting( "hasUserConsent", parameters );
            if ( hasUserConsent != null )
            {
                consentObject.put( KEY_PARTNER_GDPR_CONSENT, hasUserConsent );
            }
        }
        catch ( JSONException ex )
        {
            log( "Failed to create consent JSON object", ex );
        }

        return consentObject;
    }

    private void updateAgeRestrictedUser(final MaxAdapterParameters parameters)
    {
        // NOTE: Only for family apps and not related to COPPA
        Boolean isAgeRestrictedUser = getPrivacySetting( "isAgeRestrictedUser", parameters );
        if ( isAgeRestrictedUser != null )
        {
            InMobiSdk.setIsAgeRestricted( isAgeRestrictedUser );
        }
    }

    private Context getContext(@Nullable Activity activity)
    {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
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

        if ( AppLovinSdk.VERSION_CODE >= 9_11_00 )
        {
            Boolean isDoNotSell = getPrivacySetting( "isDoNotSell", parameters );
            if ( isDoNotSell != null )
            {
                extras.put( "do_not_sell", isDoNotSell ? "1" : "0" );
            }
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

    private Drawable fetchNativeAdIcon(@NonNull final String iconUrl, final Bundle serverParameters, final Context context)
    {
        if ( TextUtils.isEmpty( iconUrl ) )
        {
            log( "Native ad icon url " + iconUrl + " is not valid" );
            return null;
        }

        log( "Adding native ad icon (" + iconUrl + ") to queue to be fetched" );

        final Future<Drawable> iconDrawableFuture = createDrawableFuture( iconUrl, context.getResources() );
        try
        {
            final int imageTaskTimeoutSeconds = BundleUtils.getInt( "image_task_timeout_seconds", DEFAULT_IMAGE_TASK_TIMEOUT_SECONDS, serverParameters );
            return iconDrawableFuture.get( imageTaskTimeoutSeconds, TimeUnit.SECONDS );
        }
        catch ( Throwable th )
        {
            e( "Failed to fetch icon image from URL: " + iconUrl, th );
        }

        return null;
    }

    private MaxNativeAdView createMaxNativeAdView(final MaxNativeAd maxNativeAd, final String templateName, final Activity activity)
    {
        if ( AppLovinSdk.VERSION_CODE >= 11_01_00_00 )
        {
            return new MaxNativeAdView( maxNativeAd, templateName, getApplicationContext() );
        }
        else
        {
            return new MaxNativeAdView( maxNativeAd, templateName, activity );
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

    private List<View> getClickableViews(final MaxNativeAdView maxNativeAdView)
    {
        if ( AppLovinSdk.VERSION_CODE < 11_05_03_00 )
        {
            List<View> clickableViews = new ArrayList<View>( 5 );
            if ( maxNativeAdView.getTitleTextView() != null ) clickableViews.add( maxNativeAdView.getTitleTextView() );
            if ( maxNativeAdView.getAdvertiserTextView() != null ) clickableViews.add( maxNativeAdView.getAdvertiserTextView() );
            if ( maxNativeAdView.getBodyTextView() != null ) clickableViews.add( maxNativeAdView.getBodyTextView() );
            if ( maxNativeAdView.getIconImageView() != null ) clickableViews.add( maxNativeAdView.getIconImageView() );
            if ( maxNativeAdView.getCallToActionButton() != null ) clickableViews.add( maxNativeAdView.getCallToActionButton() );

            return clickableViews;
        }
        else
        {
            return maxNativeAdView.getClickableViews();
        }
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
        public void onAdImpression(@NonNull final InMobiBanner inMobiBanner)
        {
            log( "AdView impression tracked" );
            listener.onAdViewAdDisplayed();
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
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed" ) );
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
        }

        @Override
        public void onAdImpression(@NonNull final InMobiInterstitial inMobiInterstitial)
        {
            log( "Interstitial impression tracked" );
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
            listener.onRewardedAdVideoStarted();
        }

        @Override
        public void onAdImpression(@NonNull final InMobiInterstitial inMobiInterstitial)
        {
            log( "Rewarded ad impression tracked" );
            listener.onRewardedAdDisplayed();
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

    //region Native Ad View Ad Listener

    private class NativeAdViewListener
            extends NativeAdEventListener
    {
        private final String                   placementId;
        private final WeakReference<Activity>  activityRef;
        private final MaxAdViewAdapterListener listener;
        private final Bundle                   serverParameters;
        private final MaxAdFormat              adFormat;

        NativeAdViewListener(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
        {
            this.placementId = parameters.getThirdPartyAdPlacementId();
            this.serverParameters = parameters.getServerParameters();

            this.adFormat = adFormat;
            this.activityRef = new WeakReference<>( activity );
            this.listener = listener;
        }

        @Override
        public void onAdLoadSucceeded(final InMobiNative inMobiNative, @NonNull final AdMetaInfo adMetaInfo)
        {
            // `nativeAd` may be null if the adapter is destroyed before the ad loaded (timed out). The `inMobiNative` could be null if the user cannot get fill.
            if ( nativeAd == null || inMobiNative == null || nativeAd != inMobiNative )
            {
                log( "Native " + adFormat.getLabel() + " ad failed to load: no fill" );
                listener.onAdViewAdLoadFailed( MaxAdapterError.NO_FILL );

                return;
            }

            if ( TextUtils.isEmpty( inMobiNative.getAdTitle() ) )
            {
                log( "Native " + adFormat.getLabel() + " ad does not have required assets." );
                listener.onAdViewAdLoadFailed( MaxAdapterError.INVALID_CONFIGURATION );

                return;
            }

            log( "Native " + adFormat.getLabel() + " ad loaded: " + placementId );

            final Activity activity = activityRef.get();
            final Context context = getContext( activity );

            getCachingExecutorService().execute( new Runnable()
            {
                @Override
                public void run()
                {
                    final Drawable iconDrawable = fetchNativeAdIcon( inMobiNative.getAdIconUrl(), serverParameters, context );

                    AppLovinSdkUtils.runOnUiThread( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            ImageView imageView = new ImageView( context );
                            imageView.setImageDrawable( iconDrawable );

                            FrameLayout frameLayout = new FrameLayout( context );
                            final MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                                    .setAdFormat( adFormat )
                                    .setTitle( inMobiNative.getAdTitle() )
                                    .setBody( inMobiNative.getAdDescription() )
                                    .setMediaView( frameLayout )
                                    .setIcon( new MaxNativeAd.MaxNativeAdImage( iconDrawable ) )
                                    .setCallToAction( inMobiNative.getAdCtaText() );

                            final MaxInMobiNativeAd maxInMobiNativeAd = new MaxInMobiNativeAd( listener, builder, adFormat );

                            MaxNativeAdView maxNativeAdView;

                            final String templateName = BundleUtils.getString( "template", "", serverParameters );
                            if ( templateName.contains( "vertical" ) )
                            {
                                if ( AppLovinSdk.VERSION_CODE < 9_14_05_00 )
                                {
                                    log( "Vertical native banners are only supported on MAX SDK 9.14.5 and above. Default horizontal native template will be used." );
                                }

                                if ( templateName.equals( "vertical" ) )
                                {
                                    String verticalTemplateName = ( adFormat == MaxAdFormat.LEADER ) ? "vertical_leader_template" : "vertical_media_banner_template";
                                    maxNativeAdView = createMaxNativeAdView( maxInMobiNativeAd, verticalTemplateName, activity );
                                }
                                else
                                {
                                    maxNativeAdView = createMaxNativeAdView( maxInMobiNativeAd, templateName, activity );
                                }
                            }
                            else if ( AppLovinSdk.VERSION_CODE < 9_14_05_00 )
                            {
                                maxNativeAdView = createMaxNativeAdView( maxInMobiNativeAd,
                                                                         AppLovinSdkUtils.isValidString( templateName ) ? templateName : "no_body_banner_template",
                                                                         activity );
                            }
                            else
                            {
                                maxNativeAdView = createMaxNativeAdView( maxInMobiNativeAd,
                                                                         AppLovinSdkUtils.isValidString( templateName ) ? templateName : "media_banner_template",
                                                                         activity );
                            }

                            maxInMobiNativeAd.prepareForInteraction( getClickableViews( maxNativeAdView ), maxNativeAdView );

                            if ( AppLovinSdk.VERSION_CODE >= 9_15_00_00 && AppLovinSdkUtils.isValidString( adMetaInfo.getCreativeID() ) )
                            {
                                Bundle extraInfo = new Bundle( 1 );
                                extraInfo.putString( "creative_id", adMetaInfo.getCreativeID() );

                                listener.onAdViewAdLoaded( maxNativeAdView, extraInfo );
                            }
                            else
                            {
                                listener.onAdViewAdLoaded( maxNativeAdView );
                            }
                        }
                    } );
                }
            } );
        }

        @Override
        public void onAdLoadFailed(@NonNull final InMobiNative inMobiNative, @NonNull final InMobiAdRequestStatus inMobiAdRequestStatus)
        {
            MaxAdapterError adapterError = toMaxError( inMobiAdRequestStatus );
            log( "Native " + adFormat.getLabel() + " ad failed to load with error " + adapterError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onAdImpression(@NonNull final InMobiNative inMobiNative)
        {
            log( "Native " + adFormat.getLabel() + " ad shown" );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdClicked(@NonNull final InMobiNative inMobiNative)
        {
            log( "Native " + adFormat.getLabel() + " ad clicked" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onUserWillLeaveApplication(@NonNull final InMobiNative inMobiNative)
        {
            log( "Native " + adFormat.getLabel() + " ad user will leave application" );
        }

        @Override
        public void onAdFullScreenWillDisplay(@NonNull final InMobiNative inMobiNative)
        {
            log( "Native " + adFormat.getLabel() + " ad fullscreen will display" );
        }

        @Override
        public void onAdFullScreenDisplayed(@NonNull final InMobiNative inMobiNative)
        {
            log( "Native " + adFormat.getLabel() + " ad fullscreen displayed" );
        }

        @Override
        public void onAdFullScreenDismissed(@NonNull final InMobiNative inMobiNative)
        {
            log( "Native " + adFormat.getLabel() + " ad fullscreen dismissed" );
        }

        @Override
        public void onAdStatusChanged(@NonNull final InMobiNative inMobiNative)
        {
            log( "Native " + adFormat.getLabel() + " ad status changed" );
        }
    }

    //region Native Ad Listener

    private class NativeAdListener
            extends NativeAdEventListener
    {
        private final String                     placementId;
        private final Context                    context;
        private final MaxNativeAdAdapterListener listener;
        private final Bundle                     serverParameters;

        NativeAdListener(final MaxAdapterResponseParameters parameters, final Context context, final MaxNativeAdAdapterListener listener)
        {
            placementId = parameters.getThirdPartyAdPlacementId();
            serverParameters = parameters.getServerParameters();

            this.context = context;
            this.listener = listener;
        }

        @Override
        public void onAdLoadSucceeded(final InMobiNative inMobiNative, @NonNull final AdMetaInfo adMetaInfo)
        {
            // `nativeAd` may be null if the adapter is destroyed before the ad loaded (timed out). The `inMobiNative` could be null if the user cannot get fill.
            if ( nativeAd == null || inMobiNative == null || nativeAd != inMobiNative )
            {
                log( "Native ad failed to load: no fill" );
                listener.onNativeAdLoadFailed( MaxAdapterError.NO_FILL );

                return;
            }

            final String templateName = BundleUtils.getString( "template", "", serverParameters );
            final boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );
            if ( isTemplateAd && TextUtils.isEmpty( inMobiNative.getAdTitle() ) )
            {
                e( "Native ad (" + inMobiNative + ") does not have required assets." );
                listener.onNativeAdLoadFailed( MaxAdapterError.MISSING_REQUIRED_NATIVE_AD_ASSETS );

                return;
            }

            log( "Native ad loaded: " + placementId );

            getCachingExecutorService().execute( new Runnable()
            {
                @Override
                public void run()
                {
                    Drawable iconDrawable = fetchNativeAdIcon( inMobiNative.getAdIconUrl(), serverParameters, context );

                    handleNativeAdLoaded( inMobiNative, adMetaInfo, iconDrawable, context );
                }
            } );
        }

        private void handleNativeAdLoaded(@NonNull final InMobiNative inMobiNative, @NonNull final AdMetaInfo adMetaInfo, final Drawable iconDrawable, final Context context)
        {
            AppLovinSdkUtils.runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    ImageView imageView = new ImageView( context );
                    imageView.setImageDrawable( iconDrawable );

                    FrameLayout frameLayout = new FrameLayout( context );
                    final MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                            .setAdFormat( MaxAdFormat.NATIVE )
                            .setTitle( inMobiNative.getAdTitle() )
                            .setBody( inMobiNative.getAdDescription() )
                            .setMediaView( frameLayout )
                            .setIcon( new MaxNativeAd.MaxNativeAdImage( iconDrawable ) )
                            .setCallToAction( inMobiNative.getAdCtaText() );

                    if ( AppLovinSdk.VERSION_CODE >= 11_07_00_00 )
                    {
                        builder.setStarRating( (double) inMobiNative.getAdRating() );
                    }

                    final MaxInMobiNativeAd maxInMobiNativeAd = new MaxInMobiNativeAd( listener, builder, MaxAdFormat.NATIVE );
                    if ( AppLovinSdkUtils.isValidString( adMetaInfo.getCreativeID() ) )
                    {
                        Bundle extraInfo = new Bundle( 1 );
                        extraInfo.putString( "creative_id", adMetaInfo.getCreativeID() );
                        listener.onNativeAdLoaded( maxInMobiNativeAd, extraInfo );
                    }
                    else
                    {
                        listener.onNativeAdLoaded( maxInMobiNativeAd, null );
                    }
                }
            } );
        }

        @Override
        public void onAdLoadFailed(@NonNull final InMobiNative inMobiNative, @NonNull final InMobiAdRequestStatus inMobiAdRequestStatus)
        {
            MaxAdapterError adapterError = toMaxError( inMobiAdRequestStatus );
            log( "Native ad failed to load with error " + adapterError );
            listener.onNativeAdLoadFailed( adapterError );
        }

        @Override
        public void onAdImpression(@NonNull final InMobiNative inMobiNative)
        {
            log( "Native ad shown" );
            listener.onNativeAdDisplayed( null );
        }

        @Override
        public void onAdClicked(@NonNull final InMobiNative inMobiNative)
        {
            log( "Native ad clicked" );
            listener.onNativeAdClicked();
        }

        @Override
        public void onUserWillLeaveApplication(@NonNull final InMobiNative inMobiNative)
        {
            log( "Native ad user will leave application" );
        }

        @Override
        public void onAdFullScreenWillDisplay(@NonNull final InMobiNative inMobiNative)
        {
            log( "Native ad fullscreen will display" );
        }

        @Override
        public void onAdFullScreenDisplayed(@NonNull final InMobiNative inMobiNative)
        {
            log( "Native ad fullscreen displayed" );
        }

        @Override
        public void onAdFullScreenDismissed(@NonNull final InMobiNative inMobiNative)
        {
            log( "Native ad fullscreen dismissed" );
        }

        @Override
        public void onAdStatusChanged(@NonNull final InMobiNative inMobiNative)
        {
            log( "Native ad status changed" );
        }
    }

    //endregion

    private class MaxInMobiNativeAd
            extends MaxNativeAd
    {
        private final MaxAdapterListener listener;
        private final MaxAdFormat        format;

        public MaxInMobiNativeAd(final MaxAdapterListener listener, final Builder builder, final MaxAdFormat format)
        {
            super( builder );

            this.listener = listener;
            this.format = format;
        }

        @Override
        public void prepareViewForInteraction(final MaxNativeAdView maxNativeAdView)
        {
            prepareForInteraction( InMobiMediationAdapter.this.getClickableViews( maxNativeAdView ), maxNativeAdView );
        }

        // @Override
        public boolean prepareForInteraction(final List<View> clickableViews, final ViewGroup container)
        {
            final InMobiNative nativeAd = InMobiMediationAdapter.this.nativeAd;
            if ( nativeAd == null )
            {
                e( "Failed to register native ad views: native ad is null." );
                return false;
            }

            // We don't provide the aspect ratio for InMobi's media view since the media view is rendered after the ad is rendered.
            final FrameLayout mediaView = (FrameLayout) getMediaView();
            mediaView.post( new Runnable()
            {
                @Override
                public void run()
                {
                    int primaryViewWidth = mediaView.getWidth();

                    final boolean isHorizontalBanner = ( format == MaxAdFormat.BANNER ) && ( mediaView.getWidth() > mediaView.getHeight() );

                    // For horizontal banners before AppLovin SDK version 11.6.0, scale primary view appropriately.
                    if ( AppLovinSdk.VERSION_CODE < 11_06_00_00 && isHorizontalBanner )
                    {
                        primaryViewWidth = (int) ( mediaView.getHeight() * ( 16.0 / 9.0 ) );
                    }

                    final View primaryView = nativeAd.getPrimaryViewOfWidth( mediaView.getContext(), null, mediaView, primaryViewWidth );
                    if ( primaryView == null ) return;

                    mediaView.addView( primaryView );

                    // For horizontal banners before AppLovin SDK version 11.6.0, center primary view.
                    if ( AppLovinSdk.VERSION_CODE < 11_06_00_00 && isHorizontalBanner )
                    {
                        ( (FrameLayout.LayoutParams) primaryView.getLayoutParams() ).gravity = Gravity.CENTER;
                    }
                }
            } );

            final View.OnClickListener clickListener = new View.OnClickListener()
            {
                @Override
                public void onClick(final View view)
                {
                    log( "Native ad clicked from click listener" );

                    nativeAd.reportAdClickAndOpenLandingPage();
                    if ( format == MaxAdFormat.NATIVE )
                    {
                        ( (MaxNativeAdAdapterListener) listener ).onNativeAdClicked();
                    }
                    else if ( format.isAdViewAd() )
                    {
                        ( (MaxAdViewAdapterListener) listener ).onAdViewAdClicked();
                    }
                    else
                    {
                        log( "Unsupported ad format: must be adView ad or native ad" );
                    }
                }
            };

            // InMobi does not provide a method to bind views with landing url, so we need to do it manually.
            for ( View clickableView : clickableViews )
            {
                if ( clickableView != null ) clickableView.setOnClickListener( clickListener );
            }

            return true;
        }
    }
}
