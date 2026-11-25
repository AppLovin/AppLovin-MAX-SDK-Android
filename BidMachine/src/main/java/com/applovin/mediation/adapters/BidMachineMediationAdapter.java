package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import com.applovin.impl.sdk.utils.BundleUtils;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxNativeAdAdapter;
import com.applovin.mediation.adapter.MaxRewardedAdapter;
import com.applovin.mediation.adapter.MaxSignalProvider;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxNativeAdAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxRewardedAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.adapters.bidmachine.BuildConfig;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.bidmachine.Ad;
import io.bidmachine.AdLoadCallback;
import io.bidmachine.BMError;
import io.bidmachine.BidMachine;
import io.bidmachine.BidMachineInstance;
import io.bidmachine.BidTokenCallback;
import io.bidmachine.format.bannerad.BannerListener;
import io.bidmachine.format.bannerad.BannerView;
import io.bidmachine.format.interstitialad.InterstitialAd;
import io.bidmachine.format.interstitialad.InterstitialListener;
import io.bidmachine.format.nativead.NativeAd;
import io.bidmachine.format.nativead.NativeClickableView;
import io.bidmachine.format.nativead.NativeListener;
import io.bidmachine.format.rewardedad.RewardedAd;
import io.bidmachine.format.rewardedad.RewardedListener;
import io.bidmachine.model.BannerAdSize;
import io.bidmachine.model.BidMachineConfiguration;
import io.bidmachine.model.ConsentData;
import io.bidmachine.model.ImageData;
import io.bidmachine.model.MediationInfo;
import io.bidmachine.model.Placement;

public class BidMachineMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter, MaxNativeAdAdapter
{
    private static final    int                DEFAULT_IMAGE_TASK_TIMEOUT_SECONDS = 10;
    private static final    AtomicBoolean      initialized                        = new AtomicBoolean();
    private static volatile BidMachineInstance bidMachineInstance;

    private final Object adLock = new Object();

    private volatile InterstitialAd interstitialAd;
    private volatile RewardedAd     rewardedAd;
    private volatile BannerView     adView;
    private volatile NativeAd       nativeAd;
    private          boolean        isDestroyed;

    public BidMachineMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( initialized.compareAndSet( false, true ) )
        {
            final String sourceId = parameters.getServerParameters().getString( "source_id" );
            if ( !AppLovinSdkUtils.isValidString( sourceId ) )
            {
                String errorMessage = "source id is missing";
                log( "BidMachine SDK failed to initialize: " + errorMessage );

                initialized.set( false );
                onCompletionListener.onCompletion( InitializationStatus.INITIALIZED_FAILURE, errorMessage );

                return;
            }

            log( "Initializing BidMachine SDK with source id: " + sourceId );

            BidMachine.setLoggingEnabled( parameters.isTesting() );
            BidMachine.setTestMode( parameters.isTesting() );

            updateSettings( parameters );

            final BidMachineConfiguration configuration = new BidMachineConfiguration( new MediationInfo( "applovin_max", AppLovinSdk.VERSION, getAdapterVersion() ) );
            bidMachineInstance = BidMachine.initialize( getApplicationContext(), sourceId, configuration );
        }

        onCompletionListener.onCompletion( InitializationStatus.DOES_NOT_APPLY, null );
    }

    @Override
    public String getSdkVersion()
    {
        return getVersionString( BidMachine.class, "VERSION" );
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        final InterstitialAd interstitialAd;
        final RewardedAd rewardedAd;
        final BannerView adView;
        final NativeAd nativeAd;
        synchronized ( adLock )
        {
            isDestroyed = true;

            interstitialAd = this.interstitialAd;
            rewardedAd = this.rewardedAd;
            adView = this.adView;
            nativeAd = this.nativeAd;

            this.interstitialAd = null;
            this.rewardedAd = null;
            this.adView = null;
            this.nativeAd = null;
        }

        if ( interstitialAd != null )
        {
            interstitialAd.setListener( null );
            interstitialAd.destroy();
        }

        if ( rewardedAd != null )
        {
            rewardedAd.setListener( null );
            rewardedAd.destroy();
        }

        if ( adView != null )
        {
            adView.setListener( null );
            adView.destroy();
        }

        if ( nativeAd != null )
        {
            nativeAd.setListener( null );
            nativeAd.destroy();
        }
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, @Nullable final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal for " + parameters.getAdFormat().getLabel() + " ad..." );

        if ( bidMachineInstance == null )
        {
            String errorMessage = "BidMachine SDK is not initialized";
            log( "Signal collection failed with error: " + errorMessage );
            callback.onSignalCollectionFailed( errorMessage );

            return;
        }

        updateSettings( parameters );

        List<Placement> placements = new ArrayList<>( 1 );
        String adUnitId = parameters.getAdUnitId();
        Bundle credentials = BundleUtils.getBundle( "placement_ids", Bundle.EMPTY, parameters.getServerParameters() );

        if ( AppLovinSdkUtils.isValidString( adUnitId ) )
        {
            String slotId = credentials.getString( adUnitId );
            if ( AppLovinSdkUtils.isValidString( slotId ) )
            {
                placements.add( new Placement( slotId ) );
            }
            else
            {
                log( "No valid slot ID found during signal collection" );
            }
        }

        bidMachineInstance.collectToken( placements, new BidTokenCallback()
        {
            @Override
            public void onCollected(@NonNull final String bidToken)
            {
                log( "Signal collection successful" );
                callback.onSignalCollected( bidToken );
            }

            @Override
            public void onCollectionFailed(@NonNull final BMError bmError)
            {
                log( "Signal collection failed with error: " + bmError );
                callback.onSignalCollectionFailed( bmError.getMessage() );
            }
        } );
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Loading interstitial ad..." );

        if ( bidMachineInstance == null )
        {
            log( "Interstitial ad failed to load: BidMachine SDK is not initialized" );
            listener.onInterstitialAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateSettings( parameters );

        final InterstitialAdLoadCallback loadCallback = new InterstitialAdLoadCallback( listener );
        final String bidResponse = parameters.getBidResponse();
        if ( bidResponse != null )
        {
            bidMachineInstance.loadInterstitial( bidResponse, loadCallback );
        }
        else
        {
            Placement placement = createPlacementOrNull( parameters );
            bidMachineInstance.loadInterstitial( placement, loadCallback );
        }
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad..." );

        final InterstitialAd interstitialAd = this.interstitialAd;
        if ( interstitialAd == null || !interstitialAd.canShow() )
        {
            log( "Unable to show interstitial - ad not ready" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                         MaxAdapterError.AD_NOT_READY.getCode(),
                                                                         MaxAdapterError.AD_NOT_READY.getMessage() ) );

            return;
        }

        interstitialAd.show();
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Loading rewarded ad..." );

        if ( bidMachineInstance == null )
        {
            log( "Rewarded ad failed to load: BidMachine SDK is not initialized" );
            listener.onRewardedAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateSettings( parameters );

        final RewardedAdLoadCallback loadCallback = new RewardedAdLoadCallback( listener );
        final String bidResponse = parameters.getBidResponse();
        if ( bidResponse != null )
        {
            bidMachineInstance.loadRewarded( bidResponse, loadCallback );
        }
        else
        {
            Placement placement = createPlacementOrNull( parameters );
            bidMachineInstance.loadRewarded( placement, loadCallback );
        }
    }

    @Override
    public void showRewardedAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad..." );

        final RewardedAd rewardedAd = this.rewardedAd;
        if ( rewardedAd == null || !rewardedAd.canShow() )
        {
            log( "Unable to show rewarded ad - ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                                                     MaxAdapterError.AD_NOT_READY.getCode(),
                                                                     MaxAdapterError.AD_NOT_READY.getMessage() ) );

            return;
        }

        configureReward( parameters );

        rewardedAd.show();
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, @Nullable final Activity activity, final MaxAdViewAdapterListener listener)
    {
        log( "Loading " + adFormat.getLabel() + " ad..." );

        if ( bidMachineInstance == null )
        {
            log( "AdView ad failed to load: BidMachine SDK is not initialized" );
            listener.onAdViewAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateSettings( parameters );

        // Check if adaptive banner sizes should be used
        final boolean isAdaptiveBanner = parameters.getServerParameters().getBoolean( "adaptive_banner", false );
        final BannerAdSize adSize = toAdSize( adFormat, isAdaptiveBanner, parameters, getApplicationContext() );
        final AdViewLoadCallback loadCallback = new AdViewLoadCallback( listener );
        final String bidResponse = parameters.getBidResponse();
        if ( bidResponse != null )
        {
            bidMachineInstance.loadBanner( bidResponse, adSize, loadCallback );
        }
        else
        {
            Placement placement = createPlacementOrNull( parameters );
            bidMachineInstance.loadBanner( placement, adSize, loadCallback );
        }
    }

    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, @Nullable final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        log( "Loading native ad..." );

        if ( bidMachineInstance == null )
        {
            log( "Native ad failed to load: BidMachine SDK is not initialized" );
            listener.onNativeAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );

            return;
        }

        updateSettings( parameters );

        final NativeAdLoadCallback loadCallback = new NativeAdLoadCallback( parameters.getServerParameters(), listener );
        final String bidResponse = parameters.getBidResponse();
        if ( bidResponse != null )
        {
            bidMachineInstance.loadNative( bidResponse, loadCallback );
        }
        else
        {
            Placement placement = createPlacementOrNull( parameters );
            bidMachineInstance.loadNative( placement, loadCallback );
        }
    }

    private MaxAdapterError toMaxError(BMError bidMachineError)
    {
        int bidMachineErrorCode = bidMachineError.getCode();
        MaxAdapterError maxAdapterError = MaxAdapterError.UNSPECIFIED;

        switch ( bidMachineErrorCode )
        {
            case BMError.NO_CONNECTION:
                maxAdapterError = MaxAdapterError.NO_CONNECTION;
                break;
            case BMError.TIMEOUT:
                maxAdapterError = MaxAdapterError.TIMEOUT;
                break;
            case BMError.HTTP_BAD_REQUEST:
                maxAdapterError = MaxAdapterError.BAD_REQUEST;
                break;
            case BMError.HTTP_SERVER_ERROR:
            case BMError.ADAPTER:
                maxAdapterError = MaxAdapterError.SERVER_ERROR;
                break;
            case BMError.NO_CONTENT:
                maxAdapterError = MaxAdapterError.NO_FILL;
                break;
            case BMError.WAS_EXPIRED:
                maxAdapterError = MaxAdapterError.AD_EXPIRED;
                break;
            case BMError.NOT_INITIALIZED:
                maxAdapterError = MaxAdapterError.NOT_INITIALIZED;
                break;
            case BMError.WAS_USED_ALREADY:
                maxAdapterError = MaxAdapterError.INVALID_LOAD_STATE;
                break;
            case BMError.BAD_CONTENT:
            case BMError.INTERNAL:
            case BMError.WAS_DESTROYED:
            case BMError.ENCRYPTION:
                maxAdapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
        }

        return new MaxAdapterError( maxAdapterError.getCode(),
                                    maxAdapterError.getMessage(),
                                    bidMachineErrorCode,
                                    bidMachineError.getMessage() );
    }

    private MaxAdapterError toMaxDisplayError(BMError bidMachineError)
    {
        return new MaxAdapterError( MaxAdapterError.AD_DISPLAY_FAILED,
                                    bidMachineError.getCode(),
                                    bidMachineError.getMessage() );
    }

    @Nullable
    private Placement createPlacementOrNull(MaxAdapterResponseParameters parameters)
    {
        String placementId = parameters.getThirdPartyAdPlacementId();
        if ( AppLovinSdkUtils.isValidString( placementId ) )
        {
            return new Placement( placementId );
        }
        else
        {
            return null;
        }
    }

    private BannerAdSize toAdSize(final MaxAdFormat adFormat, final boolean isAdaptiveBanner, final MaxAdapterParameters parameters, final Context context)
    {
        if ( isAdaptiveBanner && isAdaptiveAdViewFormat( adFormat, parameters ) )
        {
            return getAdaptiveAdSize( parameters, context );
        }

        if ( adFormat == MaxAdFormat.BANNER )
        {
            return BannerAdSize.Banner;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return BannerAdSize.Leaderboard;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return BannerAdSize.MediumRectangle;
        }
        else
        {
            throw new IllegalArgumentException( "Invalid ad format: " + adFormat );
        }
    }

    private BannerAdSize getAdaptiveAdSize(final MaxAdapterParameters parameters, final Context context)
    {
        int adaptiveAdWidth = getAdaptiveAdViewWidth( parameters, context );

        if ( isInlineAdaptiveAdView( parameters ) )
        {
            int inlineMaximumHeight = getInlineAdaptiveAdViewMaximumHeight( parameters );
            if ( inlineMaximumHeight > 0 )
            {
                return BannerAdSize.adaptive( adaptiveAdWidth, inlineMaximumHeight );
            }
        }

        // Return anchored size by default
        final int anchoredHeight = MaxAdFormat.BANNER.getAdaptiveSize( adaptiveAdWidth, context ).getHeight();
        return BannerAdSize.adaptive( adaptiveAdWidth, anchoredHeight );
    }

    private void updateSettings(MaxAdapterParameters parameters)
    {
        Boolean hasUserConsent = parameters.hasUserConsent();
        if ( hasUserConsent != null )
        {
            BidMachine.setConsentData( new ConsentData.Builder()
                                               .setGdprConsent( hasUserConsent )
                                               .build() );
        }
    }

    @Nullable
    private Bundle createExtraInfo(final Ad ad)
    {
        final String creativeId = ad.getAuctionInfo().getCreativeId();
        if ( TextUtils.isEmpty( creativeId ) )
        {
            return null;
        }

        Bundle extraInfo = new Bundle( 1 );
        extraInfo.putString( "creative_id", creativeId );

        return extraInfo;
    }

    private class InterstitialAdLoadCallback
            implements AdLoadCallback<InterstitialAd>
    {
        private final MaxInterstitialAdapterListener listener;

        public InterstitialAdLoadCallback(final MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull InterstitialAd ad)
        {
            final boolean isAdapterDestroyed;
            synchronized ( adLock )
            {
                isAdapterDestroyed = isDestroyed;
                if ( !isAdapterDestroyed )
                {
                    ad.setListener( new InterstitialAdListener( listener ) );
                    interstitialAd = ad;
                }
            }

            if ( isAdapterDestroyed )
            {
                log( "Interstitial ad loaded after the adapter was destroyed" );
                ad.destroy();

                return;
            }

            log( "Interstitial ad loaded" );

            listener.onInterstitialAdLoaded( createExtraInfo( ad ) );
        }

        @Override
        public void onAdLoadFailed(@NonNull BMError bmError)
        {
            MaxAdapterError maxAdapterError = toMaxError( bmError );
            log( "Interstitial ad failed to load with error (" + maxAdapterError + ")" );
            listener.onInterstitialAdLoadFailed( maxAdapterError );
        }
    }

    private class InterstitialAdListener
            implements InterstitialListener
    {
        private final MaxInterstitialAdapterListener listener;

        public InterstitialAdListener(final MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdPresented(@NonNull InterstitialAd interstitialAd)
        {
            log( "Interstitial ad presented" );
        }

        @Override
        public void onAdShowFailed(@NonNull InterstitialAd interstitialAd, @NonNull BMError bmError)
        {
            MaxAdapterError maxAdapterError = toMaxDisplayError( bmError );
            log( "Interstitial ad failed to show with error (" + maxAdapterError + ")" );
            listener.onInterstitialAdDisplayFailed( maxAdapterError );
        }

        @Override
        public void onAdImpression(@NonNull InterstitialAd interstitialAd)
        {
            log( "Interstitial ad impression" );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdClicked(@NonNull InterstitialAd interstitialAd)
        {
            log( "Interstitial ad clicked" );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onAdClosed(@NonNull InterstitialAd interstitialAd)
        {
            log( "Interstitial ad closed" );
            listener.onInterstitialAdHidden();
        }

        @Override
        public void onAdExpired(@NonNull InterstitialAd interstitialAd)
        {
            log( "Interstitial ad expired" );
        }
    }

    private class RewardedAdLoadCallback
            implements AdLoadCallback<RewardedAd>
    {
        private final MaxRewardedAdapterListener listener;

        public RewardedAdLoadCallback(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull RewardedAd ad)
        {
            final boolean isAdapterDestroyed;
            synchronized ( adLock )
            {
                isAdapterDestroyed = isDestroyed;
                if ( !isAdapterDestroyed )
                {
                    ad.setListener( new RewardedAdListener( listener ) );
                    rewardedAd = ad;
                }
            }

            if ( isAdapterDestroyed )
            {
                log( "Rewarded ad loaded after the adapter was destroyed" );
                ad.destroy();

                return;
            }

            log( "Rewarded ad loaded" );

            listener.onRewardedAdLoaded( createExtraInfo( ad ) );
        }

        @Override
        public void onAdLoadFailed(@NonNull BMError bmError)
        {
            MaxAdapterError maxAdapterError = toMaxError( bmError );
            log( "Rewarded ad failed to load with error (" + maxAdapterError + ")" );
            listener.onRewardedAdLoadFailed( maxAdapterError );
        }
    }

    private class RewardedAdListener
            implements RewardedListener
    {
        private final MaxRewardedAdapterListener listener;

        private boolean hasGrantedReward;

        public RewardedAdListener(final MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdPresented(@NonNull RewardedAd rewardedAd)
        {
            log( "Rewarded ad presented" );
        }

        @Override
        public void onAdShowFailed(@NonNull RewardedAd rewardedAd, @NonNull BMError bmError)
        {
            MaxAdapterError maxAdapterError = toMaxDisplayError( bmError );
            log( "Rewarded ad failed to show with error (" + maxAdapterError + ")" );
            listener.onRewardedAdDisplayFailed( maxAdapterError );
        }

        @Override
        public void onAdImpression(@NonNull RewardedAd rewardedAd)
        {
            log( "Rewarded ad impression" );
            listener.onRewardedAdDisplayed();
        }

        @Override
        public void onAdClicked(@NonNull RewardedAd rewardedAd)
        {
            log( "Rewarded ad clicked" );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onAdRewarded(@NonNull RewardedAd rewardedAd)
        {
            log( "Rewarded ad should grant reward" );
            hasGrantedReward = true;
        }

        @Override
        public void onAdClosed(@NonNull RewardedAd rewardedAd)
        {
            log( "Rewarded ad closed" );

            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            listener.onRewardedAdHidden();
        }

        @Override
        public void onAdExpired(@NonNull RewardedAd rewardedAd)
        {
            log( "Rewarded ad expired" );
        }
    }

    private class AdViewLoadCallback
            implements AdLoadCallback<BannerView>
    {
        private final MaxAdViewAdapterListener listener;

        public AdViewLoadCallback(final MaxAdViewAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull BannerView ad)
        {
            final boolean isAdapterDestroyed;
            synchronized ( adLock )
            {
                isAdapterDestroyed = isDestroyed;
                if ( !isAdapterDestroyed )
                {
                    ad.setListener( new AdViewListener( listener ) );
                    adView = ad;
                }
            }

            if ( isAdapterDestroyed )
            {
                log( "AdView ad loaded after the adapter was destroyed" );
                ad.destroy();

                return;
            }

            log( "AdView ad loaded" );

            listener.onAdViewAdLoaded( ad, createExtraInfo( ad ) );
        }

        @Override
        public void onAdLoadFailed(@NonNull BMError bmError)
        {
            MaxAdapterError maxAdapterError = toMaxError( bmError );
            log( "AdView ad failed to load with error (" + maxAdapterError + ")" );
            listener.onAdViewAdLoadFailed( maxAdapterError );
        }
    }

    private class AdViewListener
            implements BannerListener
    {
        private final MaxAdViewAdapterListener listener;

        public AdViewListener(final MaxAdViewAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdPresented(@NonNull BannerView bannerView)
        {
            log( "AdView ad presented" );
        }

        @Override
        public void onAdImpression(@NonNull BannerView bannerView)
        {
            log( "AdView ad impression" );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onAdShowFailed(@NonNull BannerView bannerView, @NonNull BMError bmError)
        {
            MaxAdapterError maxAdapterError = toMaxDisplayError( bmError );
            log( "AdView ad failed to show with error (" + maxAdapterError + ")" );
            listener.onAdViewAdDisplayFailed( maxAdapterError );
        }

        @Override
        public void onAdClicked(@NonNull BannerView bannerView)
        {
            log( "AdView ad clicked" );
            listener.onAdViewAdClicked();
        }

        @Override
        public void onAdExpired(@NonNull BannerView bannerView)
        {
            log( "AdView ad expired" );
        }
    }

    private class NativeAdLoadCallback
            implements AdLoadCallback<NativeAd>
    {
        private final Bundle                     serverParameters;
        private final MaxNativeAdAdapterListener listener;

        public NativeAdLoadCallback(final Bundle serverParameters, final MaxNativeAdAdapterListener listener)
        {
            this.serverParameters = serverParameters;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull final NativeAd ad)
        {
            final boolean isAdapterDestroyed;
            synchronized ( adLock )
            {
                isAdapterDestroyed = isDestroyed;
                if ( !isAdapterDestroyed )
                {
                    ad.setListener( new NativeAdListener( listener ) );
                    nativeAd = ad;
                }
            }

            if ( isAdapterDestroyed )
            {
                log( "Native ad loaded after the adapter was destroyed" );
                ad.destroy();

                return;
            }

            log( "Native ad loaded" );

            String templateName = BundleUtils.getString( "template", "", serverParameters );
            final boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );
            if ( isTemplateAd && TextUtils.isEmpty( ad.getTitle() ) )
            {
                e( "Native ad (" + ad + ") does not have required assets." );
                listener.onNativeAdLoadFailed( MaxAdapterError.MISSING_REQUIRED_NATIVE_AD_ASSETS );
                return;
            }

            ImageData iconImageData = ad.getIcon();
            if ( iconImageData == null )
            {
                handleNativeAdLoaded( ad, null );
                return;
            }

            MaxNativeAd.MaxNativeAdImage maxNativeAdImage = null;
            final Drawable image = iconImageData.getImage();
            final Uri localUri = iconImageData.getLocalUri();
            final String remoteUrl = iconImageData.getRemoteUrl();

            if ( image != null )
            {
                maxNativeAdImage = new MaxNativeAd.MaxNativeAdImage( image );
            }
            else if ( localUri != null )
            {
                maxNativeAdImage = new MaxNativeAd.MaxNativeAdImage( localUri );
            }
            else if ( remoteUrl != null )
            {
                getCachingExecutorService().execute( new Runnable()
                {
                    @Override
                    public void run()
                    {
                        Drawable image = null;
                        log( "Adding native ad icon (" + remoteUrl + ") to queue to be fetched" );
                        final Future<Drawable> imageFuture = createDrawableFuture( remoteUrl,
                                                                                   getApplicationContext().getResources() );
                        final int imageTaskTimeoutSeconds = BundleUtils.getInt( "image_task_timeout_seconds",
                                                                                DEFAULT_IMAGE_TASK_TIMEOUT_SECONDS,
                                                                                serverParameters );
                        try
                        {
                            image = imageFuture.get( imageTaskTimeoutSeconds, TimeUnit.SECONDS );
                        }
                        catch ( Throwable th )
                        {
                            e( "Failed to fetch icon image", th );
                        }

                        MaxNativeAd.MaxNativeAdImage maxNativeAdImage = new MaxNativeAd.MaxNativeAdImage( image );
                        handleNativeAdLoaded( ad, maxNativeAdImage );
                    }
                } );

                return;
            }

            handleNativeAdLoaded( ad, maxNativeAdImage );
        }

        @Override
        public void onAdLoadFailed(@NonNull BMError bmError)
        {
            MaxAdapterError maxAdapterError = toMaxError( bmError );
            log( "Native ad failed to load with error (" + maxAdapterError + ")" );
            listener.onNativeAdLoadFailed( maxAdapterError );
        }

        private void handleNativeAdLoaded(@NonNull final NativeAd nativeAd, final MaxNativeAd.MaxNativeAdImage iconMaxNativeAdImage)
        {
            AppLovinSdkUtils.runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    final MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                            .setAdFormat( MaxAdFormat.NATIVE )
                            .setTitle( nativeAd.getTitle() )
                            .setAdvertiser( nativeAd.getAdvertiser() )
                            .setBody( nativeAd.getBody() )
                            .setCallToAction( nativeAd.getCallToAction() )
                            .setStarRating( nativeAd.getStarRating() )
                            .setIcon( iconMaxNativeAdImage )
                            .setOptionsView( nativeAd.getAdChoiceView() )
                            .setMediaView( nativeAd.getMediaView() );
                    if ( nativeAd.getMainImage() != null )
                    {
                        MaxNativeAd.MaxNativeAdImage mainImage = new MaxNativeAd.MaxNativeAdImage( nativeAd.getMainImage().getImage() );
                        builder.setMainImage( mainImage );
                    }
                    final MaxBidMachineNativeAd maxBidMachineNativeAd = new MaxBidMachineNativeAd( builder );

                    listener.onNativeAdLoaded( maxBidMachineNativeAd, createExtraInfo( nativeAd ) );
                }
            } );
        }
    }

    private class NativeAdListener
            implements NativeListener
    {
        private final MaxNativeAdAdapterListener listener;

        public NativeAdListener(final MaxNativeAdAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdPresented(@NonNull NativeAd nativeAd)
        {
            log( "Native ad presented" );
        }

        @Override
        public void onAdImpression(@NonNull NativeAd nativeAd)
        {
            log( "Native ad impression" );
            listener.onNativeAdDisplayed( null );
        }

        @Override
        public void onAdShowFailed(@NonNull NativeAd nativeAd, @NonNull BMError bmError)
        {
            log( "Native ad failed to show with error (" + bmError + ")" );
        }

        @Override
        public void onAdClicked(@NonNull NativeAd nativeAd)
        {
            log( "Native ad clicked" );
            listener.onNativeAdClicked();
        }

        @Override
        public void onAdExpired(@NonNull NativeAd nativeAd)
        {
            log( "Native ad expired" );
        }
    }

    private class MaxBidMachineNativeAd
            extends MaxNativeAd
    {
        public MaxBidMachineNativeAd(Builder builder) { super( builder ); }

        @Override
        public boolean prepareForInteraction(final List<View> clickableViews, final ViewGroup container)
        {
            NativeAd nativeAd = BidMachineMediationAdapter.this.nativeAd;
            if ( nativeAd == null )
            {
                e( "Failed to register native ad views: native ad is null." );
                return false;
            }

            d( "Preparing views for interaction: " + clickableViews + " with container: " + container );

            final List<NativeClickableView> nativeClickableViews = new ArrayList<>( clickableViews.size() );
            for ( final View clickableView : clickableViews )
            {
                nativeClickableViews.add( new NativeClickableView( clickableView ) );
            }

            return nativeAd.bind( container, nativeClickableViews );
        }
    }
}
