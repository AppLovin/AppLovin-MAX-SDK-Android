package com.applovin.mediation.adapters;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.applovin.impl.sdk.utils.BundleUtils;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
import com.applovin.mediation.adapter.MaxNativeAdAdapter;
import com.applovin.mediation.adapter.MaxSignalProvider;
import com.applovin.mediation.adapter.listeners.MaxAdViewAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxInterstitialAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxNativeAdAdapterListener;
import com.applovin.mediation.adapter.listeners.MaxSignalCollectionListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterInitializationParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.mediation.adapter.parameters.MaxAdapterSignalCollectionParameters;
import com.applovin.mediation.adapters.criteo.BuildConfig;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkUtils;
import com.criteo.publisher.Criteo;
import com.criteo.publisher.CriteoBannerAdListener;
import com.criteo.publisher.CriteoBannerView;
import com.criteo.publisher.CriteoErrorCode;
import com.criteo.publisher.CriteoInitException;
import com.criteo.publisher.CriteoInterstitial;
import com.criteo.publisher.CriteoInterstitialAdListener;
import com.criteo.publisher.advancednative.CriteoMedia;
import com.criteo.publisher.advancednative.CriteoNativeAd;
import com.criteo.publisher.advancednative.CriteoNativeAdListener;
import com.criteo.publisher.advancednative.CriteoNativeLoader;
import com.criteo.publisher.advancednative.CriteoNativeRenderer;
import com.criteo.publisher.advancednative.RendererHelper;
import com.criteo.publisher.model.AdSize;
import com.criteo.publisher.model.BannerAdUnit;
import com.criteo.publisher.model.InterstitialAdUnit;
import com.criteo.publisher.model.NativeAdUnit;

import java.lang.reflect.Method;
import java.net.URL;
import java.util.List;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import static com.applovin.mediation.adapter.MaxAdapterError.MISSING_REQUIRED_NATIVE_AD_ASSETS;
import static com.applovin.sdk.AppLovinSdkUtils.runOnUiThread;

public class CriteoMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxAdViewAdapter, MaxNativeAdAdapter
{
    private static final String PUB_ID_KEY = "pub_id";

    private static final AtomicBoolean        initialized = new AtomicBoolean();
    private static       InitializationStatus initStatus;

    private CriteoBannerView   bannerView;
    private CriteoInterstitial interstitialAd;
    private CriteoNativeAd     nativeAd;
    private ViewGroup          nativeAdContainer;
    private View               renderedView;

    public CriteoMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    //region MaxAdapter Methods

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, @Nullable final Activity activity, final OnCompletionListener onCompletionListener)
    {
        log( "Initializing Criteo SDK..." );

        if ( initialized.compareAndSet( false, true ) )
        {
            initStatus = InitializationStatus.INITIALIZING;

            // Criteo requires a valid publisher key for initialization
            final String publisherKey = parameters.getServerParameters().getString( PUB_ID_KEY );
            if ( TextUtils.isEmpty( publisherKey ) )
            {
                e( "Criteo failed to initialize because the publisher key is missing" );

                initStatus = InitializationStatus.INITIALIZED_FAILURE;
                onCompletionListener.onCompletion( initStatus, "The publisher key is missing." );

                return;
            }

            try
            {
                new Criteo.Builder( (Application) getApplicationContext(), publisherKey )
                        .debugLogsEnabled( parameters.isTesting() )
                        .init();

                initStatus = InitializationStatus.INITIALIZED_UNKNOWN;
                onCompletionListener.onCompletion( initStatus, null );
            }
            catch ( CriteoInitException exception )
            {
                initStatus = InitializationStatus.INITIALIZED_FAILURE;
                onCompletionListener.onCompletion( initStatus, exception.getLocalizedMessage() );
            }
        }
        else
        {
            onCompletionListener.onCompletion( initStatus, null );
        }
    }

    @Override
    public String getSdkVersion()
    {
        return Criteo.getVersion();
    }

    @Override
    public String getAdapterVersion()
    {
        return BuildConfig.VERSION_NAME;
    }

    @Override
    public void onDestroy()
    {
        log( "Destroy called for adapter " + this );

        if ( interstitialAd != null )
        {
            interstitialAd.setCriteoInterstitialAdListener( null );
            interstitialAd = null;
        }

        if ( bannerView != null )
        {
            bannerView.setCriteoBannerAdListener( null );
            bannerView.destroy();
            bannerView = null;
        }

        if ( nativeAdContainer != null )
        {
            nativeAdContainer.removeView( renderedView );
            nativeAdContainer = null;
            renderedView = null;
        }

        nativeAd = null;
    }

    //endregion

    //region MaxSignalProvider Methods

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        callback.onSignalCollected( "" ); // No-op since Criteo does not need the buyeruid to bid
    }

    //endregion

    //region MaxInterstitialAdapter Methods

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        if ( !initialized.get() )
        {
            log( "Interstitial ad failed to load. Criteo SDK not initialized." );
            listener.onInterstitialAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );
            return;
        }

        final String placementId = parameters.getThirdPartyAdPlacementId();
        final boolean isBiddingAd = AppLovinSdkUtils.isValidString( parameters.getBidResponse() );

        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + "interstitial ad: " + placementId + "..." );

        updatePrivacySettings( parameters );

        interstitialAd = new CriteoInterstitial( new InterstitialAdUnit( placementId ) );
        interstitialAd.setCriteoInterstitialAdListener( new InterstitialAdListener( placementId, listener ) );
        interstitialAd.loadAdWithDisplayData( parameters.getBidResponse() );
    }

    @Override
    public void showInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Showing interstitial ad: " + placementId + "..." );

        if ( interstitialAd != null && interstitialAd.isAdLoaded() )
        {
            interstitialAd.show();
        }
        else
        {
            e( "Interstitial ad failed to show: " + placementId );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed", 0, "Interstitial ad not ready" ) );
        }
    }

    //endregion

    //region MaxAdViewAdapter Methods

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        if ( !initialized.get() )
        {
            log( adFormat.getLabel() + " ad failed to load. Criteo SDK not initialized." );
            listener.onAdViewAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );
            return;
        }

        final String placementId = parameters.getThirdPartyAdPlacementId();
        final boolean isBiddingAd = AppLovinSdkUtils.isValidString( parameters.getBidResponse() );

        log( "Loading " + ( isBiddingAd ? "bidding " : "" ) + adFormat.getLabel() + " ad for placement id: " + placementId + "..." );

        updatePrivacySettings( parameters );

        runOnUiThread( new Runnable()
        {
            @Override
            public void run()
            {
                bannerView = new CriteoBannerView( getApplicationContext(), new BannerAdUnit( placementId, toAdSize( adFormat ) ) );
                bannerView.setCriteoBannerAdListener( new AdViewListener( placementId, adFormat, listener ) );
                bannerView.loadAdWithDisplayData( parameters.getBidResponse() );
            }
        } );
    }

    //endregion

    //region MaxNativeAdAdapter Methods

    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        if ( !initialized.get() )
        {
            log( "Native ad failed to load. Criteo SDK not initialized" );
            listener.onNativeAdLoadFailed( MaxAdapterError.NOT_INITIALIZED );
            return;
        }

        final String placementId = parameters.getThirdPartyAdPlacementId();
        log( "Loading native ad: " + placementId + "..." );

        updatePrivacySettings( parameters );

        final MaxNativeAdListener maxNativeAdListener = new MaxNativeAdListener( placementId, parameters, getApplicationContext(), listener );
        final CriteoNativeLoader nativeLoader = new CriteoNativeLoader( new NativeAdUnit( placementId ), maxNativeAdListener, maxNativeAdListener );

        nativeLoader.loadAd();
    }

    //endregion

    //region Helper Methods

    private static MaxAdapterError toMaxError(final CriteoErrorCode criteoErrorCode)
    {
        switch ( criteoErrorCode )
        {
            case ERROR_CODE_NO_FILL:
                return MaxAdapterError.NO_FILL;
            case ERROR_CODE_NETWORK_ERROR:
                return MaxAdapterError.NO_CONNECTION;
            case ERROR_CODE_INTERNAL_ERROR:
                return MaxAdapterError.INTERNAL_ERROR;
            case ERROR_CODE_INVALID_REQUEST:
                return MaxAdapterError.BAD_REQUEST;
        }

        return MaxAdapterError.UNSPECIFIED;
    }

    private AdSize toAdSize(final MaxAdFormat adFormat)
    {
        if ( adFormat == MaxAdFormat.BANNER )
        {
            return new AdSize( 320, 50 );
        }
        else if ( adFormat == MaxAdFormat.MREC )
        {
            return new AdSize( 300, 250 );
        }
        else if ( adFormat == MaxAdFormat.LEADER )
        {
            return new AdSize( 728, 90 );
        }
        else
        {
            throw new IllegalArgumentException( "Unsupported ad format: " + adFormat );
        }
    }

    private void updatePrivacySettings(final MaxAdapterParameters parameters)
    {
        final Boolean isDoNotSell = parameters.isDoNotSell();
        if ( isDoNotSell != null )
        {
            // CCPA
            Criteo.getInstance().setUsPrivacyOptOut( isDoNotSell );
        }
    }

    //endregion

    private class InterstitialAdListener
            implements CriteoInterstitialAdListener
    {
        private final String                         placementId;
        private final MaxInterstitialAdapterListener listener;

        InterstitialAdListener(final String placementId, final MaxInterstitialAdapterListener listener)
        {
            this.placementId = placementId;
            this.listener = listener;
        }

        @Override
        public void onAdReceived(@NonNull final CriteoInterstitial interstitial)
        {
            log( "Interstitial ad loaded: " + placementId );
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onAdFailedToReceive(@NonNull final CriteoErrorCode code)
        {
            final MaxAdapterError adapterError = toMaxError( code );

            log( "Interstitial ad (" + placementId + ") failed to show with error: " + adapterError );
            listener.onInterstitialAdLoadFailed( adapterError );
        }

        @Override
        public void onAdOpened()
        {
            log( "Interstitial ad shown: " + placementId );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onAdClicked()
        {
            log( "Interstitial ad clicked: " + placementId );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onAdLeftApplication()
        {
            log( "Interstitial ad left application: " + placementId );
        }

        @Override
        public void onAdClosed()
        {
            log( "Interstitial ad hidden: " + placementId );
            listener.onInterstitialAdHidden();
        }
    }

    private class AdViewListener
            implements CriteoBannerAdListener
    {
        final String                   placementId;
        final MaxAdFormat              adFormat;
        final MaxAdViewAdapterListener listener;

        AdViewListener(final String placementId,
                       final MaxAdFormat adFormat,
                       final MaxAdViewAdapterListener listener)
        {
            this.placementId = placementId;
            this.adFormat = adFormat;
            this.listener = listener;
        }

        @Override
        public void onAdReceived(@NonNull final CriteoBannerView bannerView)
        {
            log( adFormat.getLabel() + " ad loaded: " + placementId );

            listener.onAdViewAdLoaded( bannerView );
            listener.onAdViewAdDisplayed(); // Criteo does not have a dedicated impression callback
        }

        @Override
        public void onAdFailedToReceive(@NonNull final CriteoErrorCode code)
        {
            final MaxAdapterError adapterError = toMaxError( code );

            log( adFormat.getLabel() + " ad (" + placementId + ") failed to load with error: " + adapterError );
            listener.onAdViewAdLoadFailed( adapterError );
        }

        @Override
        public void onAdLeftApplication()
        {
            log( adFormat.getLabel() + " left application" );
        }

        @Override
        public void onAdClicked()
        {
            log( adFormat.getLabel() + " ad clicked" );
            listener.onAdViewAdClicked();
        }
    }

    private class MaxNativeAdListener
            implements CriteoNativeAdListener, CriteoNativeRenderer
    {
        private final String                     placementId;
        private final Bundle                     serverParameters;
        private final Context                    context;
        private final MaxNativeAdAdapterListener listener;

        public MaxNativeAdListener(final String placementId, final MaxAdapterResponseParameters parameters, final Context context, final MaxNativeAdAdapterListener listener)
        {
            this.placementId = placementId;
            this.serverParameters = parameters.getServerParameters();
            this.context = context;
            this.listener = listener;
        }

        @Override
        public void onAdReceived(@NonNull final CriteoNativeAd ad)
        {
            nativeAd = ad;

            final String templateName = BundleUtils.getString( "template", "", serverParameters );
            final boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );
            if ( isTemplateAd && TextUtils.isEmpty( nativeAd.getTitle() ) )
            {
                e( "Native ad (" + nativeAd + ") does not have required assets." );
                listener.onNativeAdLoadFailed( MISSING_REQUIRED_NATIVE_AD_ASSETS );

                return;
            }

            final URL iconUrl = getImageUrl( ad.getAdvertiserLogoMedia() );
            final URL mediaUrl = getImageUrl( ad.getProductMedia() );

            if ( mediaUrl == null )
            {
                log( "Native ad failed to load: media URL not found" );
                listener.onNativeAdLoadFailed( MISSING_REQUIRED_NATIVE_AD_ASSETS );

                return;
            }

            getCachingExecutorService().execute( new Runnable()
            {
                @Override
                public void run()
                {
                    final Drawable icon = getImageDrawable( iconUrl, context );
                    final Drawable media = getImageDrawable( mediaUrl, context );

                    runOnUiThread( new Runnable()
                    {
                        @Override
                        public void run()
                        {
                            final MaxNativeAd.MaxNativeAdImage iconImage = new MaxNativeAd.MaxNativeAdImage( icon );
                            final ImageView mediaView = new ImageView( getApplicationContext() );
                            mediaView.setImageDrawable( media );

                            final MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                                    .setAdFormat( MaxAdFormat.NATIVE )
                                    .setIcon( iconImage )
                                    .setTitle( ad.getTitle() )
                                    .setAdvertiser( ad.getAdvertiserDescription() )
                                    .setBody( ad.getDescription() )
                                    .setMediaView( mediaView )
                                    .setCallToAction( ad.getCallToAction() );
                            final MaxNativeAd maxNativeAd = new MaxCriteoNativeAd( builder );

                            log( "Native ad loaded: " + placementId );
                            listener.onNativeAdLoaded( maxNativeAd, null );
                        }
                    } );
                }
            } );
        }

        @Override
        public void onAdFailedToReceive(@NonNull CriteoErrorCode errorCode)
        {
            final MaxAdapterError adapterError = toMaxError( errorCode );

            log( "Native ad (" + placementId + ") failed to load with error: " + adapterError );
            listener.onNativeAdLoadFailed( adapterError );
        }

        @NonNull
        @Override
        public View createNativeView(@NonNull final Context context, @Nullable final ViewGroup parent)
        {
            return new FrameLayout( context ); // Temp view to render the ad assets into (see MaxNativeAd.prepareViewForInteraction() implementation)
        }

        @Override
        public void renderNativeView(@NonNull final RendererHelper helper, @NonNull final View nativeView, @NonNull final CriteoNativeAd nativeAd)
        {
            // No-op. Implemented in CriteoNativeAdListener#onAdReceived(CriteoNativeAd)}
        }

        @Override
        public void onAdImpression()
        {
            log( "Native ad shown" );
            listener.onNativeAdDisplayed( null );
        }

        @Override
        public void onAdClicked()
        {
            log( "Native ad clicked" );
            listener.onNativeAdClicked();
        }

        @Override
        public void onAdLeftApplication()
        {
            log( "Native ad left application" );
        }

        @Override
        public void onAdClosed()
        {
            log( "Native ad closed" );
        }

        private URL getImageUrl(CriteoMedia mediaContent)
        {
            if ( mediaContent == null ) return null;

            Object url = null;
            try
            {
                // Criteo renders images into their own View classes so we need to extract the URLs to download and render them ourselves.
                final Method method = mediaContent.getClass().getDeclaredMethod( "getImageUrl" );
                method.setAccessible( true );

                url = method.invoke( mediaContent );
            }
            catch ( Throwable th )
            {
                e( "Failed to get image URL", th );
            }

            return url instanceof URL ? (URL) url : null;
        }

        private Drawable getImageDrawable(final URL url, final Context context)
        {
            Drawable iconDrawable = null;
            final Future<Drawable> iconDrawableFuture = createDrawableFuture( url.toString(), context.getResources() );
            final int imageTaskTimeoutSeconds = BundleUtils.getInt( "image_task_timeout_seconds", 10, serverParameters );

            try
            {
                iconDrawable = iconDrawableFuture.get( imageTaskTimeoutSeconds, TimeUnit.SECONDS );
            }
            catch ( Throwable th )
            {
                e( "Failed to fetch native ad image URL (" + url + ") with error: ", th );
            }

            log( "Native ad image data retrieved" );
            return iconDrawable;
        }
    }

    private class MaxCriteoNativeAd
            extends MaxNativeAd
    {
        public MaxCriteoNativeAd(final Builder builder)
        {
            super( builder );
        }

        @Override
        public void prepareViewForInteraction(final MaxNativeAdView maxNativeAdView)
        {
            prepareForInteraction( null, maxNativeAdView );
        }

        // @Override
        public boolean prepareForInteraction(final List<View> clickableViews, final ViewGroup container)
        {
            final CriteoNativeAd nativeAd = CriteoMediationAdapter.this.nativeAd;
            if ( nativeAd == null )
            {
                e( "Failed to register native ad view: native ad is null." );
                return false;
            }

            nativeAdContainer = container;

            d( "Preparing views for interaction: " + clickableViews + " with container: " + container );

            // Criteo internally registers impression and click tracking logic via this call
            renderedView = nativeAd.createNativeRenderedView( getApplicationContext(), null );
            nativeAdContainer.addView( renderedView );

            return true;
        }
    }
}
