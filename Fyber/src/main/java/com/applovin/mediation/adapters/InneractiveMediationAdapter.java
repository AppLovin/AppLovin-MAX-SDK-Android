package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

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
import com.applovin.mediation.adapters.inneractive.BuildConfig;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.fyber.inneractive.sdk.external.BidTokenProvider;
import com.fyber.inneractive.sdk.external.ImpressionData;
import com.fyber.inneractive.sdk.external.InneractiveAdManager;
import com.fyber.inneractive.sdk.external.InneractiveAdRequest;
import com.fyber.inneractive.sdk.external.InneractiveAdSpot;
import com.fyber.inneractive.sdk.external.InneractiveAdSpotManager;
import com.fyber.inneractive.sdk.external.InneractiveAdViewEventsListenerWithImpressionData;
import com.fyber.inneractive.sdk.external.InneractiveAdViewUnitController;
import com.fyber.inneractive.sdk.external.InneractiveErrorCode;
import com.fyber.inneractive.sdk.external.InneractiveFullScreenAdRewardedListener;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenAdEventsListenerWithImpressionData;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenUnitController;
import com.fyber.inneractive.sdk.external.InneractiveFullscreenVideoContentController;
import com.fyber.inneractive.sdk.external.InneractiveUnitController;
import com.fyber.inneractive.sdk.external.OnFyberMarketplaceInitializedListener;
import com.fyber.inneractive.sdk.external.VideoContentListener;

import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.applovin.sdk.AppLovinSdkUtils.isValidString;

public class InneractiveMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter
{
    private static final AtomicBoolean        initialized = new AtomicBoolean();
    private static       InitializationStatus status;

    private InneractiveAdSpot interstitialSpot;
    private InneractiveAdSpot rewardedSpot;
    private InneractiveAdSpot adViewSpot;
    private ViewGroup         adViewGroup;

    private boolean hasGrantedReward;

    // Explicit default constructor declaration
    public InneractiveMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( initialized.compareAndSet( false, true ) )
        {
            status = InitializationStatus.INITIALIZING;

            final String appId = parameters.getServerParameters().getString( "app_id", null );
            log( "Initializing Inneractive SDK with app id: " + appId + "..." );

            InneractiveAdManager.setUserId( getWrappingSdk().getUserIdentifier() );
            InneractiveAdManager.initialize( getContext( activity ), appId, new OnFyberMarketplaceInitializedListener()
            {
                @Override
                public void onFyberMarketplaceInitialized(final FyberInitStatus fyberInitStatus)
                {
                    if ( fyberInitStatus == FyberInitStatus.SUCCESSFULLY )
                    {
                        log( "Inneractive SDK initialized" );
                        status = InitializationStatus.INITIALIZED_SUCCESS;
                        onCompletionListener.onCompletion( status, null );
                    }
                    else
                    {
                        log( "Inneractive SDK failed to initialize with error: " + fyberInitStatus );
                        status = InitializationStatus.INITIALIZED_FAILURE;
                        onCompletionListener.onCompletion( status, fyberInitStatus.toString() );
                    }
                }
            } );
        }
        else
        {
            if ( InneractiveAdManager.wasInitialized() )
            {
                log( "Inneractive SDK already initialized" );
            }

            onCompletionListener.onCompletion( status, null );
        }
    }

    @Override
    public String getSdkVersion()
    {
        return InneractiveAdManager.getVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        if ( interstitialSpot != null )
        {
            interstitialSpot.destroy();
            interstitialSpot = null;
        }

        if ( rewardedSpot != null )
        {
            rewardedSpot.destroy();
            rewardedSpot = null;
        }

        if ( adViewSpot != null )
        {
            adViewSpot.destroy();
            adViewSpot = null;
        }

        adViewGroup = null;
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        updateUserInfo( parameters );

        String signal = BidTokenProvider.getBidderToken();
        if ( signal != null )
        {
            callback.onSignalCollected( signal );
        }
        else
        {
            log( "Failed to collect signal" );
            callback.onSignalCollectionFailed( null );
        }
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Loading " + ( isValidString( parameters.getBidResponse() ) ? "bidding " : "" ) + "interstitial ad for spot id \"" + parameters.getThirdPartyAdPlacementId() + "\"..." );

        updateUserInfo( parameters );

        final InneractiveFullscreenVideoContentController videoContentController = new InneractiveFullscreenVideoContentController();

        final InneractiveFullscreenUnitController controller = new InneractiveFullscreenUnitController();
        controller.addContentController( videoContentController );
        controller.setEventsListener( new InneractiveFullscreenAdEventsListenerWithImpressionData()
        {
            @Override
            public void onAdImpression(final InneractiveAdSpot inneractiveAdSpot) { }

            @Override
            public void onAdImpression(final InneractiveAdSpot inneractiveAdSpot, final ImpressionData impressionData)
            {
                log( "Interstitial shown" );

                // Passing extra info such as creative id supported in 9.15.0+
                String creativeId = impressionData.getCreativeId();
                if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( creativeId ) )
                {
                    Bundle extraInfo = new Bundle( 1 );
                    extraInfo.putString( "creative_id", creativeId );

                    listener.onInterstitialAdDisplayed( extraInfo );
                }
                else
                {
                    listener.onInterstitialAdDisplayed();
                }
            }

            @Override
            public void onAdClicked(final InneractiveAdSpot inneractiveAdSpot)
            {
                log( "Interstitial clicked" );
                listener.onInterstitialAdClicked();
            }

            @Override
            public void onAdDismissed(final InneractiveAdSpot inneractiveAdSpot)
            {
                log( "Interstitial hidden" );
                listener.onInterstitialAdHidden();
            }

            @Override
            public void onAdEnteredErrorState(final InneractiveAdSpot inneractiveAdSpot, final InneractiveUnitController.AdDisplayError adDisplayError)
            {
                MaxAdapterError adapterError = new MaxAdapterError( -4205, "Ad Display Failed", 0, adDisplayError.toString() );
                log( "Interstitial failed to show: " + adapterError );

                listener.onInterstitialAdDisplayFailed( adapterError );
            }

            @Override
            public void onAdWillOpenExternalApp(final InneractiveAdSpot inneractiveAdSpot) {}

            @Override
            public void onAdWillCloseInternalBrowser(final InneractiveAdSpot inneractiveAdSpot) {}
        } );

        interstitialSpot = InneractiveAdSpotManager.get().createSpot();
        interstitialSpot.addUnitController( controller );
        interstitialSpot.setMediationName( "Max" );
        interstitialSpot.setMediationVersion( AppLovinSdk.VERSION );
        interstitialSpot.setRequestListener( new InneractiveAdSpot.RequestListener()
        {
            @Override
            public void onInneractiveSuccessfulAdRequest(final InneractiveAdSpot inneractiveAdSpot)
            {
                log( "Interstitial loaded" );
                listener.onInterstitialAdLoaded();
            }

            @Override
            public void onInneractiveFailedAdRequest(final InneractiveAdSpot inneractiveAdSpot, final InneractiveErrorCode inneractiveErrorCode)
            {
                MaxAdapterError adapterError = toMaxError( inneractiveErrorCode );
                log( "Interstitial failed to load with Inneractive error: " + adapterError );

                listener.onInterstitialAdLoadFailed( adapterError );
            }
        } );

        if ( isValidString( parameters.getBidResponse() ) )
        {
            interstitialSpot.loadAd( parameters.getBidResponse() );
        }
        else
        {
            InneractiveAdRequest request = new InneractiveAdRequest( parameters.getThirdPartyAdPlacementId() );
            interstitialSpot.requestAd( request );
        }
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad..." );

        if ( interstitialSpot.isReady() )
        {
            final InneractiveFullscreenUnitController controller = (InneractiveFullscreenUnitController) interstitialSpot.getSelectedUnitController();
            controller.show( activity );
        }
        else
        {
            log( "Interstitial ad not ready" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed" ) );
        }
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Loading " + ( isValidString( parameters.getBidResponse() ) ? "bidding " : "" ) + "rewarded ad for spot id \"" + parameters.getThirdPartyAdPlacementId() + "\"..." );

        updateUserInfo( parameters );

        final InneractiveFullscreenVideoContentController videoContentController = new InneractiveFullscreenVideoContentController();
        videoContentController.setEventsListener( new VideoContentListener()
        {
            @Override
            public void onProgress(final int totalDurationMillis, final int positionMillis)
            {
                if ( positionMillis == 0 )
                {
                    log( "Rewarded video started" );
                }
            }

            @Override
            public void onCompleted()
            {
                log( "Rewarded video completed" );
                listener.onRewardedAdVideoCompleted();
            }

            @Override
            public void onPlayerError()
            {
                log( "Rewarded video failed to display for unspecified error" );
                listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed" ) );
            }
        } );

        final InneractiveFullscreenUnitController controller = new InneractiveFullscreenUnitController();
        controller.addContentController( videoContentController );
        controller.setEventsListener( new InneractiveFullscreenAdEventsListenerWithImpressionData()
        {
            @Override
            public void onAdImpression(final InneractiveAdSpot inneractiveAdSpot) { }

            @Override
            public void onAdImpression(final InneractiveAdSpot inneractiveAdSpot, final ImpressionData impressionData)
            {
                log( "Rewarded ad shown" );

                // Passing extra info such as creative id supported in 9.15.0+
                String creativeId = impressionData.getCreativeId();
                if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( creativeId ) )
                {
                    Bundle extraInfo = new Bundle( 1 );
                    extraInfo.putString( "creative_id", creativeId );

                    listener.onRewardedAdDisplayed( extraInfo );
                }
                else
                {
                    listener.onRewardedAdDisplayed();
                }

                // `VideoContentListener.onProgress()` is called before this
                listener.onRewardedAdVideoStarted();
            }

            @Override
            public void onAdClicked(final InneractiveAdSpot inneractiveAdSpot)
            {
                log( "Rewarded ad clicked" );
                listener.onRewardedAdClicked();
            }

            @Override
            public void onAdDismissed(final InneractiveAdSpot inneractiveAdSpot)
            {
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
            public void onAdEnteredErrorState(final InneractiveAdSpot inneractiveAdSpot, final InneractiveUnitController.AdDisplayError adDisplayError)
            {
                MaxAdapterError adapterError = new MaxAdapterError( -4205, "Ad Display Failed", 0, adDisplayError.toString() );
                log( "Rewarded ad failed to show: " + adapterError );

                listener.onRewardedAdDisplayFailed( adapterError );
            }

            @Override
            public void onAdWillOpenExternalApp(final InneractiveAdSpot inneractiveAdSpot) {}

            @Override
            public void onAdWillCloseInternalBrowser(final InneractiveAdSpot inneractiveAdSpot) {}
        } );

        controller.setRewardedListener( new InneractiveFullScreenAdRewardedListener()
        {
            @Override
            public void onAdRewarded(final InneractiveAdSpot inneractiveAdSpot)
            {
                log( "User earned reward." );
                hasGrantedReward = true;
            }
        } );

        rewardedSpot = InneractiveAdSpotManager.get().createSpot();
        rewardedSpot.addUnitController( controller );
        rewardedSpot.setMediationName( "Max" );
        rewardedSpot.setMediationVersion( AppLovinSdk.VERSION );
        rewardedSpot.setRequestListener( new InneractiveAdSpot.RequestListener()
        {
            @Override
            public void onInneractiveSuccessfulAdRequest(final InneractiveAdSpot inneractiveAdSpot)
            {
                log( "Rewarded ad loaded" );
                listener.onRewardedAdLoaded();
            }

            @Override
            public void onInneractiveFailedAdRequest(final InneractiveAdSpot inneractiveAdSpot, final InneractiveErrorCode inneractiveErrorCode)
            {
                MaxAdapterError adapterError = toMaxError( inneractiveErrorCode );
                log( "Rewarded ad failed to load with Inneractive error: " + adapterError );

                listener.onRewardedAdLoadFailed( adapterError );
            }
        } );

        if ( isValidString( parameters.getBidResponse() ) )
        {
            rewardedSpot.loadAd( parameters.getBidResponse() );
        }
        else
        {
            InneractiveAdRequest request = new InneractiveAdRequest( parameters.getThirdPartyAdPlacementId() );
            rewardedSpot.requestAd( request );
        }
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad..." );

        if ( rewardedSpot.isReady() )
        {
            // Configure userReward from server.
            configureReward( parameters );

            final InneractiveFullscreenUnitController controller = (InneractiveFullscreenUnitController) rewardedSpot.getSelectedUnitController();
            controller.show( activity );
        }
        else
        {
            log( "Rewarded ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed" ) );
        }
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        log( "Loading " + ( isValidString( parameters.getBidResponse() ) ? "bidding " : "" ) + adFormat.getLabel() + " ad for spot id \"" + parameters.getThirdPartyAdPlacementId() + "\"..." );

        updateUserInfo( parameters );

        final InneractiveAdViewUnitController controller = new InneractiveAdViewUnitController();
        controller.setEventsListener( new InneractiveAdViewEventsListenerWithImpressionData()
        {
            @Override
            public void onAdImpression(final InneractiveAdSpot inneractiveAdSpot) { }

            @Override
            public void onAdImpression(final InneractiveAdSpot inneractiveAdSpot, final ImpressionData impressionData)
            {
                log( "AdView shown" );

                // Passing extra info such as creative id supported in 9.15.0+
                String creativeId = impressionData.getCreativeId();
                if ( AppLovinSdk.VERSION_CODE >= 9150000 && !TextUtils.isEmpty( creativeId ) )
                {
                    Bundle extraInfo = new Bundle( 1 );
                    extraInfo.putString( "creative_id", creativeId );

                    listener.onAdViewAdDisplayed( extraInfo );
                }
                else
                {
                    listener.onAdViewAdDisplayed();
                }
            }

            @Override
            public void onAdClicked(final InneractiveAdSpot inneractiveAdSpot)
            {
                log( "AdView clicked" );
                listener.onAdViewAdClicked();
            }

            @Override
            public void onAdExpanded(final InneractiveAdSpot inneractiveAdSpot)
            {
                log( "AdView expanded" );
                listener.onAdViewAdExpanded();
            }

            @Override
            public void onAdCollapsed(final InneractiveAdSpot inneractiveAdSpot)
            {
                log( "AdView collapsed" );
                listener.onAdViewAdCollapsed();
            }

            @Override
            public void onAdEnteredErrorState(final InneractiveAdSpot inneractiveAdSpot, final InneractiveUnitController.AdDisplayError adDisplayError)
            {
                MaxAdapterError adapterError = new MaxAdapterError( -4205, "Ad Display Failed", 0, adDisplayError.toString() );
                log( "AdView failed to show: " + adapterError );

                listener.onAdViewAdDisplayFailed( adapterError );
            }

            @Override
            public void onAdResized(final InneractiveAdSpot inneractiveAdSpot) {}

            @Override
            public void onAdWillCloseInternalBrowser(final InneractiveAdSpot inneractiveAdSpot) {}

            @Override
            public void onAdWillOpenExternalApp(final InneractiveAdSpot inneractiveAdSpot) {}
        } );

        adViewGroup = new RelativeLayout( getContext( activity ) );

        adViewSpot = InneractiveAdSpotManager.get().createSpot();
        adViewSpot.addUnitController( controller );
        adViewSpot.setMediationName( "Max" );
        adViewSpot.setMediationVersion( AppLovinSdk.VERSION );
        adViewSpot.setRequestListener( new InneractiveAdSpot.RequestListener()
        {
            @Override
            public void onInneractiveSuccessfulAdRequest(final InneractiveAdSpot inneractiveAdSpot)
            {
                if ( inneractiveAdSpot.isReady() )
                {
                    log( "AdView loaded" );
                    controller.bindView( adViewGroup );
                    listener.onAdViewAdLoaded( adViewGroup );
                }
                else
                {
                    log( "AdView not ready" );
                    listener.onAdViewAdLoadFailed( MaxAdapterError.AD_NOT_READY );
                }
            }

            @Override
            public void onInneractiveFailedAdRequest(final InneractiveAdSpot inneractiveAdSpot, final InneractiveErrorCode inneractiveErrorCode)
            {
                log( "AdView failed to load with Inneractive error: " + inneractiveErrorCode + " " + inneractiveErrorCode.toString() );
                listener.onAdViewAdLoadFailed( toMaxError( inneractiveErrorCode ) );
            }
        } );

        if ( isValidString( parameters.getBidResponse() ) )
        {
            adViewSpot.loadAd( parameters.getBidResponse() );
        }
        else
        {
            InneractiveAdRequest request = new InneractiveAdRequest( parameters.getThirdPartyAdPlacementId() );
            adViewSpot.requestAd( request );
        }
    }

    private void updateUserInfo(final MaxAdapterParameters parameters)
    {
        InneractiveAdManager.setUserId( getWrappingSdk().getUserIdentifier() );

        if ( getWrappingSdk().getConfiguration().getConsentDialogState() == AppLovinSdkConfiguration.ConsentDialogState.APPLIES )
        {
            Boolean hasUserConsent = getPrivacySetting( "hasUserConsent", parameters );
            if ( hasUserConsent != null )
            {
                InneractiveAdManager.setGdprConsent( hasUserConsent );
            }
        }
        else
        {
            InneractiveAdManager.clearGdprConsentData();
        }

        if ( AppLovinSdk.VERSION_CODE >= 11_04_03_99 )
        {
            if ( parameters.getConsentString() != null )
            {
                InneractiveAdManager.setGdprConsentString( parameters.getConsentString() );
            }
        }

        Bundle serverParameters = parameters.getServerParameters();
        // Overwritten by `mute_state` setting, unless `mute_state` is disabled
        if ( serverParameters.containsKey( "is_muted" ) ) // Introduced in 9.10.0
        {
            InneractiveAdManager.setMuteVideo( serverParameters.getBoolean( "is_muted" ) );
        }

        if ( AppLovinSdk.VERSION_CODE >= 91100 )
        {
            Boolean isDoNotSell = getPrivacySetting( "isDoNotSell", parameters );
            if ( isDoNotSell != null )
            {
                InneractiveAdManager.setUSPrivacyString( isDoNotSell ? "1YY-" : "1YN-" );
            }
            else
            {
                InneractiveAdManager.setUSPrivacyString( "1---" );
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

    private static MaxAdapterError toMaxError(final InneractiveErrorCode inneractiveErrorCode)
    {
        MaxAdapterError adapterError = MaxAdapterError.UNSPECIFIED;
        switch ( inneractiveErrorCode )
        {
            case NO_FILL:
                adapterError = MaxAdapterError.NO_FILL;
                break;
            case SERVER_INTERNAL_ERROR:
                adapterError = MaxAdapterError.SERVER_ERROR;
                break;
            case SERVER_INVALID_RESPONSE:
                adapterError = MaxAdapterError.BAD_REQUEST;
                break;
            case SDK_INTERNAL_ERROR:
            case ERROR_CODE_NATIVE_VIDEO_NOT_SUPPORTED:
            case NATIVE_ADS_NOT_SUPPORTED_FOR_OS:
            case UNSUPPORTED_SPOT:
            case NON_SECURE_CONTENT_DETECTED:
                adapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
            case CANCELLED:
                adapterError = MaxAdapterError.AD_NOT_READY;
                break;
            case CONNECTION_TIMEOUT:
            case LOAD_TIMEOUT:
            case IN_FLIGHT_TIMEOUT:
                adapterError = MaxAdapterError.TIMEOUT;
                break;
            case CONNECTION_ERROR:
                adapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case UNKNOWN_APP_ID:
            case INVALID_INPUT:
            case SDK_NOT_INITIALIZED:
            case SDK_NOT_INITIALIZED_OR_CONFIG_ERROR:
                adapterError = MaxAdapterError.NOT_INITIALIZED;
                break;
            case ERROR_CONFIGURATION_MISMATCH:
            case ERROR_CONFIGURATION_NO_SUCH_SPOT:
            case SPOT_DISABLED:
                adapterError = MaxAdapterError.INVALID_CONFIGURATION;
                break;
            case UNSPECIFIED:
                adapterError = MaxAdapterError.UNSPECIFIED;
                break;
        }

        final int adapterErrorCode;
        final String adapterErrorStr;
        if ( inneractiveErrorCode != null )
        {
            adapterErrorCode = inneractiveErrorCode.ordinal();
            adapterErrorStr = inneractiveErrorCode.name();
        }
        else
        {
            adapterErrorCode = 0;
            adapterErrorStr = "";
        }

        return new MaxAdapterError( adapterError.getErrorCode(), adapterError.getErrorMessage(), adapterErrorCode, adapterErrorStr );
    }

    private Context getContext(Activity activity)
    {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
    }
}
