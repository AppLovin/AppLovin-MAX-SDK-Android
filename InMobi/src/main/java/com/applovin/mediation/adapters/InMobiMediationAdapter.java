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
import com.applovin.sdk.AppLovinSdkUtils;
import com.inmobi.ads.AdMetaInfo;
import com.inmobi.ads.InMobiAdRequestStatus;
import com.inmobi.ads.InMobiBanner;
import com.inmobi.ads.InMobiInterstitial;
import com.inmobi.ads.InMobiNative;
import com.inmobi.ads.listeners.BannerAdEventListener;
import com.inmobi.ads.listeners.InterstitialAdEventListener;
import com.inmobi.ads.listeners.NativeAdEventListener;
import com.inmobi.compliance.InMobiPrivacyCompliance;
import com.inmobi.media.ads.nativeAd.InMobiNativeImage;
import com.inmobi.media.ads.nativeAd.InMobiNativeViewData;
import com.inmobi.sdk.InMobiSdk;
import com.inmobi.sdk.SdkInitializationListener;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.ref.WeakReference;
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

    private static final int TITLE_LABEL_TAG          = 1;
    private static final int ICON_VIEW_TAG            = 3;
    private static final int BODY_VIEW_TAG            = 4;
    private static final int CALL_TO_ACTION_VIEW_TAG  = 5;
    private static final int ADVERTISER_VIEW_TAG      = 8;
    private static final String KEY_PARTNER_GDPR_CONSENT = "partner_gdpr_consent_available";
    private static final String KEY_PARTNER_GDPR_APPLIES = "partner_gdpr_applies";

    // https://support.inmobi.com/monetize/android-guidelines/native-ads-for-android/#set-up-native-ad
    // The default setting is an in-Feed ad layout, an aspect ratio ranging between 256:135 - 1200x627
    private static final float DEFAULT_MEDIA_CONTENT_ASPECT_RATIO = (float) ( ( 256.0 / 135.0 + 1200.0 / 627.0 ) / 2.0 );

    private static final int DEFAULT_IMAGE_TASK_TIMEOUT_SECONDS = 5;

    private static final AtomicBoolean        initialized = new AtomicBoolean();
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
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity, final MaxSignalCollectionListener callback)
    {
        if ( !InMobiSdk.isSDKInitialized() )
        {
            callback.onSignalCollectionFailed( "InMobi SDK initialization failed." );
            return;
        }

        updatePrivacySettings( parameters );

        String signal = InMobiSdk.getToken( getExtras(), null );
        callback.onSignalCollected( signal );
    }

    // @Override
    public Boolean shouldInitializeOnUiThread()
    {
        // InMobi requires SDK to be initialized on UI thread.
        return true;
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
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( initialized.compareAndSet( false, true ) )
        {
            final String accountId = parameters.getServerParameters().getString( "account_id" );
            log( "Initializing InMobi SDK with account id: " + accountId + "..." );

            final Context context = getContext( activity );

            status = InitializationStatus.INITIALIZING;

            final JSONObject consentObject = getConsentJSONObject( parameters );

            if ( accountId == null )
            {
                log( "InMobi SDK initialization failed with null account id" );

                status = InitializationStatus.INITIALIZED_FAILURE;
                onCompletionListener.onCompletion( status, "Account id is null" );

                return;
            }

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
            onCompletionListener.onCompletion( status, null );
        }
    }

    //region MaxAdViewAdAdapter Methods

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, @Nullable final Activity activity, final MaxAdViewAdapterListener listener)
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

        updatePrivacySettings( parameters );

        final String bidResponse = parameters.getBidResponse();
        final boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );

        final Context context = getContext( activity );
        if ( isNative )
        {
            nativeAd = new InMobiNative( context,
                    placementId,
                    new NativeAdViewListener( parameters, adFormat, activity, listener ) );
            nativeAd.setExtras( getExtras() );

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
            adView.setExtras( getExtras() );
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
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final long placementId = Long.parseLong( parameters.getThirdPartyAdPlacementId() );
        log( "Loading interstitial ad for placement: " + placementId + "..." );

        if ( !InMobiSdk.isSDKInitialized() )
        {
            log( "InMobi SDK not successfully initialized: failing interstitial ad load..." );
            listener.onInterstitialAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        interstitialAd = loadFullscreenAd( placementId, parameters, new InterstitialListener( listener ), activity );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad..." );

        final boolean success = showFullscreenAd( interstitialAd );
        if ( !success )
        {
            log( "Interstitial ad not ready" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                    MaxAdapterError.AD_NOT_READY.getCode(),
                    MaxAdapterError.AD_NOT_READY.getMessage() ) );
        }
    }

    //endregion

    //region MaxRewardedAdAdapter Methods

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final long placementId = Long.parseLong( parameters.getThirdPartyAdPlacementId() );
        log( "Loading rewarded ad for placement: " + placementId + "..." );

        if ( !InMobiSdk.isSDKInitialized() )
        {
            log( "InMobi SDK not successfully initialized: failing rewarded ad load..." );
            listener.onRewardedAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        rewardedAd = loadFullscreenAd( placementId, parameters, new RewardedAdListener( listener ), activity );
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad..." );

        // Configure userReward from server.
        configureReward( parameters );

        final boolean success = showFullscreenAd( rewardedAd );
        if ( !success )
        {
            log( "Rewarded ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                    MaxAdapterError.AD_NOT_READY.getCode(),
                    MaxAdapterError.AD_NOT_READY.getMessage() ) );
        }
    }

    //endregion

    //region MaxNativeAdAdapter Methods

    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        if ( !InMobiSdk.isSDKInitialized() )
        {
            log( "InMobi SDK not successfully initialized: failing native ad load..." );
            listener.onNativeAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updatePrivacySettings( parameters );

        final long placementId = Long.parseLong( parameters.getThirdPartyAdPlacementId() );

        final String bidResponse = parameters.getBidResponse();
        final boolean isBiddingAd = AppLovinSdkUtils.isValidString( bidResponse );
        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + "native ad for placement: " + placementId + "..." );

        final Context context = getContext( activity );
        nativeAd = new InMobiNative( context,
                placementId,
                new NativeAdListener( parameters, context, listener ) );

        nativeAd.setExtras( getExtras() );

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

    private InMobiInterstitial loadFullscreenAd(long placementId, MaxAdapterResponseParameters parameters, InterstitialAdEventListener listener, @Nullable final Activity activity)
    {
        InMobiInterstitial interstitial = new InMobiInterstitial( getContext( activity ), placementId, listener );
        interstitial.setExtras( getExtras() );

        updatePrivacySettings( parameters );

        final String bidResponse = parameters.getBidResponse();
        if ( !TextUtils.isEmpty( bidResponse ) )
        {
            interstitial.load( bidResponse.getBytes() );
        }
        else
        {
            interstitial.load();
        }

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
            Boolean hasUserConsent = parameters.hasUserConsent();
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

    private void updatePrivacySettings(final MaxAdapterParameters parameters)
    {
        InMobiSdk.setPartnerGDPRConsent( getConsentJSONObject( parameters ) );

        Boolean isDoNotSell = parameters.isDoNotSell();
        if ( isDoNotSell != null )
        {
            InMobiPrivacyCompliance.setDoNotSell( isDoNotSell );
        }
    }

    private Context getContext(@Nullable final Activity activity)
    {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
    }

    private Map<String, String> getExtras()
    {
        Map<String, String> extras = new HashMap<>( 2 );
        extras.put( "tp", "c_applovin" );
        extras.put( "tp-ver", AppLovinSdk.VERSION );

        return extras;
    }

    private float getNativeAdMediaContentAspectRatio(final MaxAdapterParameters parameters)
    {
        final Map<String, Object> localExtraParameters = parameters.getLocalExtraParameters();
        Object aspectRatioObj = localExtraParameters.get( "native_ad_media_content_aspect_ratio" );
        if ( aspectRatioObj instanceof Number )
        {
            return ( (Number) aspectRatioObj ).floatValue();
        }

        return DEFAULT_MEDIA_CONTENT_ASPECT_RATIO;
    }

    private Drawable fetchNativeAdIcon(final InMobiNativeImage image, final Bundle serverParameters, final Context context) {
        if (image == null) {
            log("Native ad icon Object is NULL");
            return null;
        }

        String iconUrl = image.getUrl();

        if (TextUtils.isEmpty(iconUrl)) {
            log("Native ad icon url " + iconUrl + " is not valid");
            return null;
        }

        log("Adding native ad icon (" + iconUrl + ") to queue to be fetched");

        final Future<Drawable> iconDrawableFuture = createDrawableFuture(iconUrl, context.getResources());
        try {
            final int imageTaskTimeoutSeconds = BundleUtils.getInt( "image_task_timeout_seconds", DEFAULT_IMAGE_TASK_TIMEOUT_SECONDS, serverParameters );
            return iconDrawableFuture.get(imageTaskTimeoutSeconds, TimeUnit.SECONDS);
        } catch (Throwable th) {
            e("Failed to fetch icon image from URL: " + iconUrl, th);
        }

        return null;
    }

    private MaxNativeAdView createMaxNativeAdView(final MaxNativeAd maxNativeAd, final String templateName)
    {
        return new MaxNativeAdView( maxNativeAd, templateName, getApplicationContext() );
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

        return new MaxAdapterError( adapterError, inMobiErrorCode.ordinal(), inMobiError.getMessage() );
    }

    private List<View> getClickableViews(final MaxNativeAdView maxNativeAdView)
    {
        return maxNativeAdView.getClickableViews();
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

            if ( !TextUtils.isEmpty( adMetaInfo.getCreativeID() ) )
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

            if ( !TextUtils.isEmpty( adMetaInfo.getCreativeID() ) )
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
            listener.onInterstitialAdDisplayFailed( MaxAdapterError.AD_DISPLAY_FAILED );
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

            if ( !TextUtils.isEmpty( adMetaInfo.getCreativeID() ) )
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
            listener.onRewardedAdDisplayFailed( MaxAdapterError.AD_DISPLAY_FAILED );
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
        private final float                    mediaContentAspectRatio;

        NativeAdViewListener(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, @Nullable final Activity activity, final MaxAdViewAdapterListener listener)
        {
            this.placementId = parameters.getThirdPartyAdPlacementId();
            this.serverParameters = parameters.getServerParameters();
            this.mediaContentAspectRatio = getNativeAdMediaContentAspectRatio( parameters );

            this.adFormat = adFormat;
            this.activityRef = new WeakReference<>( activity );
            this.listener = listener;
        }

        @Override
        public void onAdLoadSucceeded(final InMobiNative inMobiNative, @NonNull final AdMetaInfo adMetaInfo) {
            // `nativeAd` may be null if the adapter is destroyed before the ad loaded (timed out). The `inMobiNative` could be null if the user cannot get fill.
            if (nativeAd == null || inMobiNative == null || nativeAd != inMobiNative) {
                log("Native " + adFormat.getLabel() + " ad failed to load: no fill");
                listener.onAdViewAdLoadFailed(MaxAdapterError.NO_FILL);

                return;
            }

            log("Native " + adFormat.getLabel() + " ad loaded: " + placementId);

            final Activity activity = activityRef.get();
            final Context context = getContext(activity);

            getCachingExecutorService().execute(new Runnable() {
                @Override
                public void run() {
                    final Drawable iconDrawable = fetchNativeAdIcon(inMobiNative.getAdIcon(), serverParameters, context);

                    AppLovinSdkUtils.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                                    .setAdFormat(adFormat)
                                    .setTitle(inMobiNative.getAdTitle())
                                    .setBody(inMobiNative.getAdDescription())
                                    .setCallToAction(inMobiNative.getCtaText())
                                    .setIcon(new MaxNativeAd.MaxNativeAdImage(iconDrawable))
                                    .setMediaView(inMobiNative.getMediaView())
                                    .setAdvertiser(inMobiNative.getAdvertiserName())
                                    .setMediaContentAspectRatio(mediaContentAspectRatio);

                            final MaxInMobiNativeAd maxInMobiNativeAd = new MaxInMobiNativeAd(listener, builder, adFormat);
                            MaxNativeAdView maxNativeAdView;

                            final String templateName = BundleUtils.getString("template", "", serverParameters);
                            if (templateName.contains("vertical")) {
                                if (templateName.equals("vertical")) {
                                    String verticalTemplateName = (adFormat == MaxAdFormat.LEADER) ? "vertical_leader_template" : "vertical_media_banner_template";
                                    maxNativeAdView = createMaxNativeAdView(maxInMobiNativeAd, verticalTemplateName);
                                } else {
                                    maxNativeAdView = createMaxNativeAdView(maxInMobiNativeAd, templateName);
                                }
                            } else {
                                maxNativeAdView = createMaxNativeAdView(maxInMobiNativeAd, AppLovinSdkUtils.isValidString(templateName) ? templateName : "media_banner_template");
                            }

                            maxInMobiNativeAd.prepareForInteraction(getClickableViews(maxNativeAdView), maxNativeAdView);

                            if (AppLovinSdkUtils.isValidString(adMetaInfo.getCreativeID())) {
                                Bundle extraInfo = new Bundle(1);
                                extraInfo.putString("creative_id", adMetaInfo.getCreativeID());

                                listener.onAdViewAdLoaded(maxNativeAdView, extraInfo);
                            } else {
                                listener.onAdViewAdLoaded(maxNativeAdView);
                            }
                        }
                    });
                }
            });
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
        public void onAdFullScreenDisplayed(@NonNull final InMobiNative inMobiNative)
        {
            log( "Native " + adFormat.getLabel() + " ad fullscreen displayed" );
        }

        @Override
        public void onAdFullScreenDismissed(@NonNull final InMobiNative inMobiNative)
        {
            log( "Native " + adFormat.getLabel() + " ad fullscreen dismissed" );
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
        private final float                      mediaContentAspectRatio;

        NativeAdListener(final MaxAdapterResponseParameters parameters, final Context context, final MaxNativeAdAdapterListener listener)
        {
            placementId = parameters.getThirdPartyAdPlacementId();
            serverParameters = parameters.getServerParameters();
            mediaContentAspectRatio = getNativeAdMediaContentAspectRatio( parameters );

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
                    Drawable iconDrawable = fetchNativeAdIcon( inMobiNative.getAdIcon(), serverParameters, context );

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
                    final MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                            .setAdFormat( MaxAdFormat.NATIVE )
                            .setTitle( inMobiNative.getAdTitle() )
                            .setBody( inMobiNative.getAdDescription() )
                            .setCallToAction( inMobiNative.getCtaText() )
                            .setAdvertiser( inMobiNative.getAdvertiserName() )
                            .setIcon( new MaxNativeAd.MaxNativeAdImage( iconDrawable ) )
                            .setMediaView( inMobiNative.getMediaView() )
                            .setMediaContentAspectRatio( mediaContentAspectRatio )
                            .setStarRating( (double) inMobiNative.getAdRating() );

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
        public void onAdFullScreenDisplayed(@NonNull final InMobiNative inMobiNative)
        {
            log( "Native ad fullscreen displayed" );
        }

        @Override
        public void onAdFullScreenDismissed(@NonNull final InMobiNative inMobiNative)
        {
            log( "Native ad fullscreen dismissed" );
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

        public boolean prepareForInteraction(final List<View> clickableViews, final ViewGroup container) {
            final InMobiNative nativeAd = InMobiMediationAdapter.this.nativeAd;
            if ( nativeAd == null )
            {
                e( "Failed to register native ad views: native ad is null." );
                return false;
            }

            InMobiNativeViewData.Builder viewDataBuilder = new InMobiNativeViewData.Builder(container);

            // Native integrations
            if ( container instanceof MaxNativeAdView )
            {
                MaxNativeAdView maxNativeAdView = (MaxNativeAdView) container;

                if (maxNativeAdView.getIconImageView() != null) {
                    viewDataBuilder.setIconView( maxNativeAdView.getIconImageView() );
                }
                if (maxNativeAdView.getAdvertiserTextView() != null) {
                    viewDataBuilder.setAdvertiserView( maxNativeAdView.getAdvertiserTextView() );
                }
                if (maxNativeAdView.getTitleTextView() != null) {
                    viewDataBuilder.setTitleView( maxNativeAdView.getTitleTextView() );
                }
                if (maxNativeAdView.getBodyTextView() != null) {
                    viewDataBuilder.setDescriptionView( maxNativeAdView.getBodyTextView() );
                }
                if (maxNativeAdView.getCallToActionButton() != null) {
                    viewDataBuilder.setCTAView( maxNativeAdView.getCallToActionButton() );
                }
                if (maxNativeAdView.getStarRatingContentViewGroup() != null) {
                    viewDataBuilder.setRatingView( maxNativeAdView.getStarRatingContentViewGroup() );
                }
            }
            // Plugins
            else
            {
                for ( View view : clickableViews )
                {
                    Object viewTag = view.getTag();
                    if ( viewTag == null ) continue;

                    int tag = (int) viewTag;

                    if ( tag == TITLE_LABEL_TAG )
                    {
                        viewDataBuilder.setTitleView( view );
                    }
                    else if ( tag == ICON_VIEW_TAG )
                    {
                        if ( view instanceof ImageView ) {
                            viewDataBuilder.setIconView( (ImageView) view );
                        }
                    }
                    else if ( tag == BODY_VIEW_TAG )
                    {
                        viewDataBuilder.setDescriptionView( view );
                    }
                    else if ( tag == CALL_TO_ACTION_VIEW_TAG )
                    {
                        viewDataBuilder.setCTAView( view );
                    }
                    else if ( tag == ADVERTISER_VIEW_TAG )
                    {
                        viewDataBuilder.setAdvertiserView( view );
                    }
                }
            }

            nativeAd.registerViewForTracking(viewDataBuilder.build());

            return true;
        }
    }
}
