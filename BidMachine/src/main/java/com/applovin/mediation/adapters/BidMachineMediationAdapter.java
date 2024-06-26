package com.applovin.mediation.adapters;

import android.app.Activity;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

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
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import io.bidmachine.AdsFormat;
import io.bidmachine.BidMachine;
import io.bidmachine.BidTokenCallback;
import io.bidmachine.ImageData;
import io.bidmachine.InitializationCallback;
import io.bidmachine.MediaAssetType;
import io.bidmachine.banner.BannerListener;
import io.bidmachine.banner.BannerRequest;
import io.bidmachine.banner.BannerSize;
import io.bidmachine.banner.BannerView;
import io.bidmachine.interstitial.InterstitialAd;
import io.bidmachine.interstitial.InterstitialListener;
import io.bidmachine.interstitial.InterstitialRequest;
import io.bidmachine.models.AuctionResult;
import io.bidmachine.nativead.NativeAd;
import io.bidmachine.nativead.NativeListener;
import io.bidmachine.nativead.NativeRequest;
import io.bidmachine.nativead.view.NativeMediaView;
import io.bidmachine.rewarded.RewardedAd;
import io.bidmachine.rewarded.RewardedListener;
import io.bidmachine.rewarded.RewardedRequest;
import io.bidmachine.utils.BMError;

public class BidMachineMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter, MaxNativeAdAdapter
{
    private static final int                  DEFAULT_IMAGE_TASK_TIMEOUT_SECONDS = 10;
    private static final AtomicBoolean        initialized                        = new AtomicBoolean();
    private static       InitializationStatus status;

    private InterstitialAd interstitialAd;
    private RewardedAd     rewardedAd;
    private BannerView     adView;
    private NativeAd       nativeAd;

    public BidMachineMediationAdapter(AppLovinSdk sdk) { super( sdk ); }

    @Override
    public void initialize(MaxAdapterInitializationParameters parameters, Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( initialized.compareAndSet( false, true ) )
        {
            status = InitializationStatus.INITIALIZING;

            final String sourceId = parameters.getServerParameters().getString( "source_id" );
            log( "Initializing BidMachine SDK with source id: " + sourceId );

            BidMachine.setLoggingEnabled( parameters.isTesting() );
            BidMachine.setTestMode( parameters.isTesting() );

            updateSettings( parameters );

            BidMachine.initialize( getApplicationContext(), sourceId, new InitializationCallback()
            {
                @Override
                public void onInitialized()
                {
                    log( "BidMachine SDK successfully finished initialization with source id: " + sourceId );

                    status = InitializationStatus.INITIALIZED_SUCCESS;
                    onCompletionListener.onCompletion( status, null );
                }
            } );
        }
        else
        {
            log( "BidMachine SDK is already initialized" );
            onCompletionListener.onCompletion( status, null );
        }
    }

    @Override
    public String getSdkVersion()
    {
        return BidMachine.VERSION;
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        if ( interstitialAd != null )
        {
            interstitialAd.setListener( null );
            interstitialAd.destroy();
            interstitialAd = null;
        }

        if ( rewardedAd != null )
        {
            rewardedAd.setListener( null );
            rewardedAd.destroy();
            rewardedAd = null;
        }

        if ( adView != null )
        {
            adView.setListener( null );
            adView.destroy();
            adView = null;
        }

        if ( nativeAd != null )
        {
            nativeAd.unregisterView();
            nativeAd.setListener( null );
            nativeAd.destroy();
            nativeAd = null;
        }
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal for " + parameters.getAdFormat().getLabel() + " ad..." );

        updateSettings( parameters );

        // NOTE: Must be ran on bg thread
        BidMachine.getBidToken( getApplicationContext(), toAdsFormat( parameters ), new BidTokenCallback()
        {
            @Override
            public void onCollected(@NonNull final String bidToken)
            {
                log( "Signal collection successful" );
                callback.onSignalCollected( bidToken );
            }
        } );
    }

    @Override
    public void loadInterstitialAd(MaxAdapterResponseParameters parameters, Activity activity, MaxInterstitialAdapterListener listener)
    {
        log( "Loading interstitial ad..." );

        updateSettings( parameters );

        interstitialAd = new InterstitialAd( getApplicationContext() );
        interstitialAd.setListener( new InterstitialAdListener( listener ) );
        interstitialAd.load( new InterstitialRequest.Builder()
                                     .setBidPayload( parameters.getBidResponse() )
                                     .build() );
    }

    @Override
    public void showInterstitialAd(MaxAdapterResponseParameters parameters, Activity activity, MaxInterstitialAdapterListener listener)
    {
        log( "Showing interstitial ad..." );

        if ( interstitialAd.isExpired() )
        {
            log( "Unable to show interstitial - ad expired" );
            listener.onInterstitialAdDisplayFailed( MaxAdapterError.AD_EXPIRED );

            return;
        }

        if ( !interstitialAd.canShow() )
        {
            log( "Unable to show interstitial - ad not ready" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Interstitial ad not ready" ) );

            return;
        }

        interstitialAd.show();
    }

    @Override
    public void loadRewardedAd(MaxAdapterResponseParameters parameters, Activity activity, MaxRewardedAdapterListener listener)
    {
        log( "Loading rewarded ad..." );

        updateSettings( parameters );

        rewardedAd = new RewardedAd( getApplicationContext() );
        rewardedAd.setListener( new RewardedAdListener( listener ) );
        rewardedAd.load( new RewardedRequest.Builder()
                                 .setBidPayload( parameters.getBidResponse() )
                                 .build() );
    }

    @Override
    public void showRewardedAd(MaxAdapterResponseParameters parameters, Activity activity, MaxRewardedAdapterListener listener)
    {
        log( "Showing rewarded ad..." );

        if ( rewardedAd.isExpired() )
        {
            log( "Unable to show rewarded ad - ad expired" );
            listener.onRewardedAdDisplayFailed( MaxAdapterError.AD_EXPIRED );

            return;
        }

        if ( !rewardedAd.canShow() )
        {
            log( "Unable to show rewarded ad - ad not ready" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Rewarded ad not ready" ) );

            return;
        }

        configureReward( parameters );

        rewardedAd.show();
    }

    @Override
    public void loadAdViewAd(MaxAdapterResponseParameters parameters, MaxAdFormat adFormat, Activity activity, MaxAdViewAdapterListener listener)
    {
        log( "Loading " + adFormat.getLabel() + " ad..." );

        updateSettings( parameters );

        adView = new BannerView( getApplicationContext() );
        adView.setListener( new AdViewListener( listener ) );
        adView.load( new BannerRequest.Builder()
                             .setSize( toAdSize( adFormat ) )
                             .setBidPayload( parameters.getBidResponse() )
                             .build() );
    }

    @Override
    public void loadNativeAd(MaxAdapterResponseParameters parameters, Activity activity, MaxNativeAdAdapterListener listener)
    {
        log( "Loading native ad..." );

        updateSettings( parameters );

        nativeAd = new NativeAd( getApplicationContext() );
        nativeAd.setListener( new NativeAdListener( parameters.getServerParameters(), listener ) );
        nativeAd.load( new NativeRequest.Builder()
                               .setMediaAssetTypes( MediaAssetType.All )
                               .setBidPayload( parameters.getBidResponse() )
                               .build() );
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
            case BMError.SERVER:
            case BMError.HB_NETWORK:
                maxAdapterError = MaxAdapterError.SERVER_ERROR;
                break;
            case BMError.NO_CONTENT:
                maxAdapterError = MaxAdapterError.NO_FILL;
                break;
            case BMError.EXPIRED:
                maxAdapterError = MaxAdapterError.AD_EXPIRED;
                break;
            case BMError.BAD_CONTENT:
            case BMError.INTERNAL:
            case BMError.DESTROYED:
                maxAdapterError = MaxAdapterError.INTERNAL_ERROR;
                break;
        }

        return new MaxAdapterError( maxAdapterError.getCode(),
                                    maxAdapterError.getMessage(),
                                    bidMachineErrorCode,
                                    bidMachineError.getMessage() );
    }

    private BannerSize toAdSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return BannerSize.Size_320x50;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return BannerSize.Size_728x90;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return BannerSize.Size_300x250;
        }
        else
        {
            throw new IllegalArgumentException( "Invalid ad format: " + adFormat );
        }
    }

    @Nullable
    private AdsFormat toAdsFormat(final MaxAdapterSignalCollectionParameters parameters)
    {
        MaxAdFormat adFormat = parameters.getAdFormat();
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return AdsFormat.Banner_320x50;
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return AdsFormat.Banner_300x250;
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return AdsFormat.Banner_728x90;
        }
        else if ( adFormat == MaxAdFormat.NATIVE )
        {
            return AdsFormat.Native;
        }
        else if ( adFormat == MaxAdFormat.INTERSTITIAL )
        {
            return AdsFormat.Interstitial;
        }
        else if ( adFormat == MaxAdFormat.REWARDED || adFormat == MaxAdFormat.REWARDED_INTERSTITIAL )
        {
            return AdsFormat.Rewarded;
        }
        else
        {
            return null;
        }
    }

    private void updateSettings(MaxAdapterParameters parameters)
    {
        // NOTE: BidMachine's iOS SDK requires that the adapter passes the TCFv2 GDPR consent string, the BidMachine Android SDK does not.
        Boolean isAgeRestrictedUser = parameters.isAgeRestrictedUser();
        if ( isAgeRestrictedUser != null )
        {
            BidMachine.setCoppa( isAgeRestrictedUser );
        }

        Boolean hasUserConsent = parameters.hasUserConsent();
        if ( hasUserConsent != null )
        {
            BidMachine.setConsentConfig( hasUserConsent, null );
        }

        Boolean isDoNotSell = parameters.isDoNotSell();
        if ( isDoNotSell != null )
        {
            BidMachine.setUSPrivacyString( isDoNotSell ? "1YY-" : "1YN-" );
        }
        else
        {
            BidMachine.setUSPrivacyString( "1---" );
        }
    }

    @Nullable
    private String getCreativeId(@Nullable final AuctionResult result)
    {
        return result != null ? result.getCreativeId() : null;
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
        public void onAdLoaded(@NonNull InterstitialAd interstitialAd)
        {
            log( "Interstitial ad loaded" );

            final String creativeID = getCreativeId( interstitialAd.getAuctionResult() );
            Bundle extraInfo = null;

            if ( !TextUtils.isEmpty( creativeID ) )
            {
                extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", creativeID );
            }

            listener.onInterstitialAdLoaded( extraInfo );
        }

        @Override
        public void onAdLoadFailed(@NonNull InterstitialAd interstitialAd, @NonNull BMError bmError)
        {
            MaxAdapterError maxAdapterError = toMaxError( bmError );
            log( "Interstitial ad failed to load with error (" + maxAdapterError + ")" );
            listener.onInterstitialAdLoadFailed( maxAdapterError );
        }

        @Override
        public void onAdShowFailed(@NonNull InterstitialAd interstitialAd, @NonNull BMError bmError)
        {
            MaxAdapterError maxAdapterError = toMaxError( bmError );
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
        public void onAdClosed(@NonNull InterstitialAd interstitialAd, boolean finished)
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
        public void onAdLoaded(@NonNull RewardedAd rewardedAd)
        {
            log( "Rewarded ad loaded" );

            final String creativeID = getCreativeId( rewardedAd.getAuctionResult() );
            Bundle extraInfo = null;

            if ( !TextUtils.isEmpty( creativeID ) )
            {
                extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", creativeID );
            }

            listener.onRewardedAdLoaded( extraInfo );
        }

        @Override
        public void onAdLoadFailed(@NonNull RewardedAd rewardedAd, @NonNull BMError bmError)
        {
            MaxAdapterError maxAdapterError = toMaxError( bmError );
            log( "Rewarded ad failed to load with error (" + maxAdapterError + ")" );
            listener.onRewardedAdLoadFailed( maxAdapterError );
        }

        @Override
        public void onAdShowFailed(@NonNull RewardedAd rewardedAd, @NonNull BMError bmError)
        {
            MaxAdapterError maxAdapterError = toMaxError( bmError );
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
        public void onAdClosed(@NonNull RewardedAd rewardedAd, boolean finished)
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

    private class AdViewListener
            implements BannerListener
    {
        private final MaxAdViewAdapterListener listener;

        public AdViewListener(final MaxAdViewAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull BannerView bannerView)
        {
            log( "AdView ad loaded" );

            final String creativeID = getCreativeId( bannerView.getAuctionResult() );
            Bundle extraInfo = null;

            if ( !TextUtils.isEmpty( creativeID ) )
            {
                extraInfo = new Bundle( 1 );
                extraInfo.putString( "creative_id", creativeID );
            }

            listener.onAdViewAdLoaded( bannerView, extraInfo );
        }

        @Override
        public void onAdLoadFailed(@NonNull BannerView bannerView, @NonNull BMError bmError)
        {
            MaxAdapterError maxAdapterError = toMaxError( bmError );
            log( "AdView ad failed to load with error (" + maxAdapterError + ")" );
            listener.onAdViewAdLoadFailed( maxAdapterError );
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
            MaxAdapterError maxAdapterError = toMaxError( bmError );
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

    private class NativeAdListener
            implements NativeListener
    {
        private final Bundle                     serverParameters;
        private final MaxNativeAdAdapterListener listener;

        public NativeAdListener(final Bundle serverParameters, final MaxNativeAdAdapterListener listener)
        {
            this.serverParameters = serverParameters;
            this.listener = listener;
        }

        @Override
        public void onAdLoaded(@NonNull final NativeAd nativeAd)
        {
            log( "Native ad loaded" );

            String templateName = BundleUtils.getString( "template", "", serverParameters );
            final boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );
            if ( isTemplateAd && TextUtils.isEmpty( nativeAd.getTitle() ) )
            {
                e( "Native ad (" + nativeAd + ") does not have required assets." );
                listener.onNativeAdLoadFailed( new MaxAdapterError( -5400, "Missing Native Ad Assets" ) );
                return;
            }

            ImageData iconImageData = nativeAd.getIcon();
            if ( iconImageData == null )
            {
                handleNativeAdLoaded( nativeAd, null );
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
                        handleNativeAdLoaded( nativeAd, maxNativeAdImage );
                    }
                } );

                return;
            }

            handleNativeAdLoaded( nativeAd, maxNativeAdImage );
        }

        @Override
        public void onAdLoadFailed(@NonNull NativeAd nativeAd, @NonNull BMError bmError)
        {
            MaxAdapterError maxAdapterError = toMaxError( bmError );
            log( "Native ad failed to load with error (" + maxAdapterError + ")" );
            listener.onNativeAdLoadFailed( maxAdapterError );
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

        private void handleNativeAdLoaded(@NonNull final NativeAd nativeAd, final MaxNativeAd.MaxNativeAdImage iconMaxNativeAdImage)
        {
            AppLovinSdkUtils.runOnUiThread( new Runnable()
            {
                @Override
                public void run()
                {
                    final NativeMediaView mediaView = new NativeMediaView( getApplicationContext() );

                    final MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                            .setAdFormat( MaxAdFormat.NATIVE )
                            .setTitle( nativeAd.getTitle() )
                            .setBody( nativeAd.getDescription() )
                            .setCallToAction( nativeAd.getCallToAction() )
                            .setIcon( iconMaxNativeAdImage )
                            .setOptionsView( nativeAd.getProviderView( getApplicationContext() ) )
                            .setMediaView( mediaView );
                    if ( AppLovinSdk.VERSION_CODE >= 11_04_03_99 && nativeAd.getMainImage() != null )
                    {
                        MaxNativeAd.MaxNativeAdImage mainImage = new MaxNativeAd.MaxNativeAdImage( nativeAd.getMainImage().getImage() );
                        builder.setMainImage( mainImage );
                    }
                    final MaxBidMachineNativeAd maxBidMachineNativeAd = new MaxBidMachineNativeAd( builder );

                    final String creativeID = getCreativeId( nativeAd.getAuctionResult() );
                    Bundle extraInfo = null;

                    if ( !TextUtils.isEmpty( creativeID ) )
                    {
                        extraInfo = new Bundle( 1 );
                        extraInfo.putString( "creative_id", creativeID );
                    }

                    listener.onNativeAdLoaded( maxBidMachineNativeAd, extraInfo );
                }
            } );
        }
    }

    private class MaxBidMachineNativeAd
            extends MaxNativeAd
    {
        public MaxBidMachineNativeAd(Builder builder) { super( builder ); }

        @Override
        public void prepareViewForInteraction(MaxNativeAdView maxNativeAdView)
        {
            final List<View> clickableViews = new ArrayList<>( 5 );
            if ( AppLovinSdkUtils.isValidString( getTitle() ) && maxNativeAdView.getTitleTextView() != null )
            {
                clickableViews.add( maxNativeAdView.getTitleTextView() );
            }
            if ( AppLovinSdkUtils.isValidString( getBody() ) && maxNativeAdView.getBodyTextView() != null )
            {
                clickableViews.add( maxNativeAdView.getBodyTextView() );
            }
            if ( AppLovinSdkUtils.isValidString( getCallToAction() ) && maxNativeAdView.getCallToActionButton() != null )
            {
                clickableViews.add( maxNativeAdView.getCallToActionButton() );
            }
            ImageView iconImageView = maxNativeAdView.getIconImageView();
            if ( getIcon() != null && iconImageView != null )
            {
                clickableViews.add( iconImageView );
            }
            if ( getMediaView() != null && maxNativeAdView.getMediaContentViewGroup() != null )
            {
                clickableViews.add( maxNativeAdView.getMediaContentViewGroup() );
            }

            prepareForInteraction( clickableViews, maxNativeAdView );
        }

        // @Override
        public boolean prepareForInteraction(final List<View> clickableViews, final ViewGroup container)
        {
            NativeAd nativeAd = BidMachineMediationAdapter.this.nativeAd;
            if ( nativeAd == null )
            {
                e( "Failed to register native ad views: native ad is null." );
                return false;
            }

            d( "Preparing views for interaction: " + clickableViews + " with container: " + container );

            final Set<View> clickableViewSet = new HashSet<>( clickableViews );

            ImageView iconImageView = null;
            for ( final View clickableView : clickableViews )
            {
                if ( clickableView instanceof ImageView )
                {
                    iconImageView = (ImageView) clickableView;
                    break;
                }
            }

            nativeAd.registerView( container, iconImageView, (NativeMediaView) getMediaView(), clickableViewSet );

            return true;
        }
    }
}

