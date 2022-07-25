package com.applovin.mediation.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;

import com.applovin.impl.sdk.utils.BundleUtils;
import com.applovin.mediation.MaxAdFormat;
import com.applovin.mediation.MaxReward;
import com.applovin.mediation.adapter.MaxAdViewAdapter;
import com.applovin.mediation.adapter.MaxAdapterError;
import com.applovin.mediation.adapter.MaxInterstitialAdapter;
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
import com.applovin.mediation.adapters.mytarget.BuildConfig;
import com.applovin.mediation.nativeAds.MaxNativeAd;
import com.applovin.mediation.nativeAds.MaxNativeAdView;
import com.applovin.sdk.AppLovinSdk;
import com.applovin.sdk.AppLovinSdkConfiguration;
import com.applovin.sdk.AppLovinSdkUtils;
import com.my.target.ads.InterstitialAd;
import com.my.target.ads.MyTargetView;
import com.my.target.ads.Reward;
import com.my.target.ads.RewardedAd;
import com.my.target.common.CachePolicy;
import com.my.target.common.MyTargetManager;
import com.my.target.common.MyTargetPrivacy;
import com.my.target.common.MyTargetVersion;
import com.my.target.common.models.ImageData;
import com.my.target.nativeads.AdChoicesPlacement;
import com.my.target.nativeads.NativeAd;
import com.my.target.nativeads.banners.NativePromoBanner;
import com.my.target.nativeads.factories.NativeViewsFactory;
import com.my.target.nativeads.views.MediaAdView;
import com.my.target.nativeads.views.NativeAdView;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import androidx.annotation.NonNull;

/**
 * Created by Lorenzo Gentile on 7/15/19.
 */
public class MyTargetMediationAdapter
        extends MediationAdapterBase
        implements MaxSignalProvider, MaxInterstitialAdapter, MaxRewardedAdapter, MaxAdViewAdapter /* MaxNativeAdAdapter */
{
    private static final AtomicBoolean initialized = new AtomicBoolean();

    private InterstitialAd interstitialAd;
    private RewardedAd     rewardedAd;
    private MyTargetView   adView;
    private NativeAd       nativeAd;
    private NativeAdView   nativeAdView;

    // Explicit default constructor declaration
    public MyTargetMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    @Override
    public String getSdkVersion()
    {
        return MyTargetVersion.VERSION;
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
            interstitialAd.destroy();
            interstitialAd = null;
        }

        if ( rewardedAd != null )
        {
            rewardedAd.destroy();
            rewardedAd = null;
        }

        if ( adView != null )
        {
            adView.destroy();
            adView = null;
        }

        if ( nativeAd != null )
        {
            nativeAd.setListener( null );
            nativeAd.unregisterView();
            nativeAd = null;
            nativeAdView = null;
        }
    }

    @Override
    public void initialize(final MaxAdapterInitializationParameters parameters, final Activity activity, final OnCompletionListener onCompletionListener)
    {
        if ( initialized.compareAndSet( false, true ) )
        {
            if ( parameters.isTesting() )
            {
                MyTargetManager.setDebugMode( true );
            }

            log( "Initializing myTarget SDK... " );

            final Context context = getContext( activity );

            MyTargetManager.initSdk( context );
        }

        onCompletionListener.onCompletion( InitializationStatus.DOES_NOT_APPLY, null );
    }

    @Override
    public void collectSignal(final MaxAdapterSignalCollectionParameters parameters, final Activity activity, final MaxSignalCollectionListener callback)
    {
        log( "Collecting signal..." );

        updatePrivacyStates( parameters );

        // Must be ran on bg thread
        String signal = MyTargetManager.getBidderToken( getContext( activity ) );
        callback.onSignalCollected( signal );
    }

    @Override
    public void loadInterstitialAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxInterstitialAdapterListener listener)
    {
        final int slotId = Integer.parseInt( parameters.getThirdPartyAdPlacementId() );
        log( "Loading " + ( AppLovinSdkUtils.isValidString( parameters.getBidResponse() ) ? "bidding " : "" ) + " interstitial ad for slot id: " + slotId + "..." );

        interstitialAd = new InterstitialAd( slotId, activity );
        interstitialAd.setListener( new InterstitialListener( listener ) );
        interstitialAd.getCustomParams().setCustomParam( "mediation", "7" ); // MAX specific
        updatePrivacyStates( parameters );

        String bidResponse = parameters.getBidResponse();
        if ( !TextUtils.isEmpty( bidResponse ) )
        {
            interstitialAd.loadFromBid( bidResponse );
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

        if ( interstitialAd != null )
        {
            interstitialAd.show();
        }
        else
        {
            log( "Interstitial ad is null" );
            listener.onInterstitialAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed" ) );
        }
    }

    @Override
    public void loadRewardedAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxRewardedAdapterListener listener)
    {
        final int slotId = Integer.parseInt( parameters.getThirdPartyAdPlacementId() );
        log( "Loading " + ( AppLovinSdkUtils.isValidString( parameters.getBidResponse() ) ? "bidding " : "" ) + " rewarded ad for slot id: " + slotId + "..." );

        rewardedAd = new RewardedAd( slotId, activity );
        rewardedAd.setListener( new RewardedAdListener( listener ) );
        rewardedAd.getCustomParams().setCustomParam( "mediation", "7" ); // MAX specific
        updatePrivacyStates( parameters );

        String bidResponse = parameters.getBidResponse();

        if ( !TextUtils.isEmpty( bidResponse ) )
        {
            rewardedAd.loadFromBid( bidResponse );
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

        if ( rewardedAd != null )
        {
            configureReward( parameters );
            rewardedAd.show();
        }
        else
        {
            log( "Rewarded ad is null" );
            listener.onRewardedAdDisplayFailed( new MaxAdapterError( -4205, "Ad Display Failed" ) );
        }
    }

    @Override
    public void loadAdViewAd(final MaxAdapterResponseParameters parameters, final MaxAdFormat adFormat, final Activity activity, final MaxAdViewAdapterListener listener)
    {
        final int slotId = Integer.parseInt( parameters.getThirdPartyAdPlacementId() );
        log( "Loading " + ( AppLovinSdkUtils.isValidString( parameters.getBidResponse() ) ? "bidding " : "" ) + " ad view with format: " + adFormat.getLabel() + " for slot id: " + slotId + "..." );

        adView = new MyTargetView( getContext( activity ) );
        adView.setSlotId( slotId );
        adView.setAdSize( getBannerSize( adFormat ) );
        adView.setRefreshAd( false ); // Disable auto-refreshing so MAX can control it
        adView.setListener( new AdViewListener( listener ) );
        adView.getCustomParams().setCustomParam( "mediation", "7" ); // MAX specific
        updatePrivacyStates( parameters );

        String bidResponse = parameters.getBidResponse();

        if ( !TextUtils.isEmpty( bidResponse ) )
        {
            adView.loadFromBid( bidResponse );
        }
        else
        {
            adView.load();
        }
    }

    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        final int slotId = Integer.parseInt( parameters.getThirdPartyAdPlacementId() );
        log( "Loading " + ( AppLovinSdkUtils.isValidString( parameters.getBidResponse() ) ? "bidding " : "" ) + " native ad for slot id: " + slotId + "..." );

        NativeAdListener adListener = new NativeAdListener( parameters, getContext( activity ), listener );

        nativeAd = new NativeAd( slotId, getContext( activity ) );
        nativeAd.setListener( adListener );
        nativeAd.setMediaListener( adListener );
        nativeAd.getCustomParams().setCustomParam( "mediation", "7" ); // MAX specific
        nativeAd.setAdChoicesPlacement( parameters.getServerParameters().getInt( "ad_choices_placement", AdChoicesPlacement.TOP_RIGHT ) );
        nativeAd.setCachePolicy( parameters.getServerParameters().getInt( "cache_policy", CachePolicy.ALL ) );

        updatePrivacyStates( parameters );

        // Note: only bidding is officially supported by MAX, but placements support is needed for test mode
        String bidResponse = parameters.getBidResponse();
        if ( !TextUtils.isEmpty( bidResponse ) )
        {
            nativeAd.loadFromBid( bidResponse );
        }
        else
        {
            nativeAd.load();
        }
    }

    //region Helper Functions

    private void updatePrivacyStates(final MaxAdapterParameters parameters)
    {
        // NOTE: Adapter / mediated SDK has support for COPPA, but is not approved by Play Store and therefore will be filtered on COPPA traffic
        // https://support.google.com/googleplay/android-developer/answer/9283445?hl=eno
        Boolean isAgeRestrictedUser = getPrivacySetting( "isAgeRestrictedUser", parameters );
        if ( isAgeRestrictedUser != null )
        {
            MyTargetPrivacy.setUserAgeRestricted( isAgeRestrictedUser );
        }

        if ( getWrappingSdk().getConfiguration().getConsentDialogState() == AppLovinSdkConfiguration.ConsentDialogState.APPLIES )
        {
            Boolean hasUserConsent = getPrivacySetting( "hasUserConsent", parameters );
            if ( hasUserConsent != null )
            {
                MyTargetPrivacy.setUserConsent( hasUserConsent );
            }
        }

        if ( AppLovinSdk.VERSION_CODE >= 91100 )
        {
            Boolean isDoNotSell = getPrivacySetting( "isDoNotSell", parameters );
            if ( isDoNotSell != null )
            {
                MyTargetPrivacy.setCcpaUserConsent( isDoNotSell );
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

    private Context getContext(Activity activity)
    {
        // NOTE: `activity` can only be null in 11.1.0+, and `getApplicationContext()` is introduced in 11.1.0
        return ( activity != null ) ? activity.getApplicationContext() : getApplicationContext();
    }

    private static MyTargetView.AdSize getBannerSize(final MaxAdFormat maxAdFormat)
    {
        if ( maxAdFormat == MaxAdFormat.BANNER )
        {
            return MyTargetView.AdSize.ADSIZE_320x50;
        }
        else if ( maxAdFormat == MaxAdFormat.MREC )
        {
            return MyTargetView.AdSize.ADSIZE_300x250;
        }
        else if ( maxAdFormat == MaxAdFormat.LEADER )
        {
            return MyTargetView.AdSize.ADSIZE_728x90;
        }
        return MyTargetView.AdSize.ADSIZE_320x50;
    }

    private static MaxAdapterError toMaxError(final String myTargetError)
    {
        return new MaxAdapterError( MaxAdapterError.NO_FILL.getErrorCode(), MaxAdapterError.NO_FILL.getErrorMessage(), 0, myTargetError );
    }

    //endregion

    //region Ad Listeners

    private class InterstitialListener
            implements InterstitialAd.InterstitialAdListener
    {
        private final MaxInterstitialAdapterListener listener;

        InterstitialListener(MaxInterstitialAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onLoad(@NonNull final InterstitialAd interstitialAd)
        {
            log( "Interstitial loaded" );
            listener.onInterstitialAdLoaded();
        }

        @Override
        public void onNoAd(@NonNull final String reason, @NonNull final InterstitialAd interstitialAd)
        {
            log( "Interstitial failed to load with reason: " + reason );
            listener.onInterstitialAdLoadFailed( toMaxError( reason ) );
        }

        @Override
        public void onDisplay(@NonNull final InterstitialAd interstitialAd)
        {
            log( "Interstitial displayed" );
            listener.onInterstitialAdDisplayed();
        }

        @Override
        public void onClick(@NonNull final InterstitialAd interstitialAd)
        {
            log( "Interstitial clicked" );
            listener.onInterstitialAdClicked();
        }

        @Override
        public void onVideoCompleted(@NonNull final InterstitialAd interstitialAd)
        {
            log( "Interstitial video completed" );
        }

        @Override
        public void onDismiss(@NonNull final InterstitialAd interstitialAd)
        {
            log( "Interstitial dismissed" );
            listener.onInterstitialAdHidden();
        }
    }

    private class RewardedAdListener
            implements RewardedAd.RewardedAdListener
    {
        private final MaxRewardedAdapterListener listener;
        private       boolean                    hasGrantedReward = false;

        RewardedAdListener(MaxRewardedAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onLoad(@NonNull final RewardedAd rewardedAd)
        {
            log( "Rewarded ad loaded" );
            listener.onRewardedAdLoaded();
        }

        @Override
        public void onNoAd(@NonNull final String reason, @NonNull final RewardedAd rewardedAd)
        {
            log( "Rewarded ad failed to load with reason: " + reason );
            listener.onRewardedAdLoadFailed( toMaxError( reason ) );
        }

        @Override
        public void onDisplay(@NonNull final RewardedAd rewardedAd)
        {
            log( "Rewarded ad displayed" );
            listener.onRewardedAdDisplayed();
            listener.onRewardedAdVideoStarted();
        }

        @Override
        public void onClick(@NonNull final RewardedAd rewardedAd)
        {
            log( "Rewarded ad clicked" );
            listener.onRewardedAdClicked();
        }

        @Override
        public void onReward(@NonNull Reward reward, @NonNull final RewardedAd rewardedAd)
        {
            log( "Rewarded ad reward granted" );
            hasGrantedReward = true;
        }

        @Override
        public void onDismiss(@NonNull final RewardedAd rewardedAd)
        {
            if ( hasGrantedReward || shouldAlwaysRewardUser() )
            {
                final MaxReward reward = getReward();
                log( "Rewarded user with reward: " + reward );
                listener.onUserRewarded( reward );
            }

            log( "Rewarded ad dismissed" );
            listener.onRewardedAdHidden();
        }
    }

    private class AdViewListener
            implements MyTargetView.MyTargetViewListener
    {
        private final MaxAdViewAdapterListener listener;

        AdViewListener(MaxAdViewAdapterListener listener)
        {
            this.listener = listener;
        }

        @Override
        public void onLoad(@NonNull final MyTargetView myTargetView)
        {
            log( "Ad view loaded" );
            listener.onAdViewAdLoaded( myTargetView );
        }

        @Override
        public void onNoAd(@NonNull final String reason, @NonNull final MyTargetView myTargetView)
        {
            log( "Ad view failed to load with reason: " + reason );
            listener.onAdViewAdLoadFailed( toMaxError( reason ) );
        }

        @Override
        public void onShow(@NonNull final MyTargetView myTargetView)
        {
            log( "Ad view displayed" );
            listener.onAdViewAdDisplayed();
        }

        @Override
        public void onClick(@NonNull final MyTargetView myTargetView)
        {
            log( "Ad view clicked" );
            listener.onAdViewAdClicked();
        }
    }

    private class NativeAdListener
            implements NativeAd.NativeAdListener, NativeAd.NativeAdMediaListener
    {
        private final String                     slotId;
        private final Bundle                     serverParameters;
        private final Context                    context;
        private final MaxNativeAdAdapterListener listener;

        NativeAdListener(final MaxAdapterResponseParameters parameters, final Context context, MaxNativeAdAdapterListener listener)
        {
            slotId = parameters.getThirdPartyAdPlacementId();
            serverParameters = parameters.getServerParameters();
            this.context = context;

            this.listener = listener;
        }

        @Override
        public void onLoad(@NonNull final NativePromoBanner nativePromoBanner, @NonNull final NativeAd nativeAd)
        {
            log( "Native ad loaded: " + slotId );

            // myTarget native ad should be assigned earlier in load method already - check they are still same instance
            if ( MyTargetMediationAdapter.this.nativeAd != nativeAd )
            {
                e( "Mismatched instance of native ads - adapter: " + MyTargetMediationAdapter.this.nativeAd + " and listener: " + nativeAd );
                listener.onNativeAdLoadFailed( MaxAdapterError.INVALID_LOAD_STATE );

                return;
            }

            final String templateName = BundleUtils.getString( "template", "", serverParameters );
            final boolean isTemplateAd = AppLovinSdkUtils.isValidString( templateName );
            final NativePromoBanner nativeBanner = nativeAd.getBanner();
            if ( isTemplateAd && TextUtils.isEmpty( nativeBanner.getTitle() ) )
            {
                e( "Native ad (" + nativeAd + ") does not have required assets." );
                listener.onNativeAdLoadFailed( new MaxAdapterError( -5400, "Missing Native Ad Assets" ) );

                return;
            }

            final ImageData icon = nativeBanner.getIcon();
            final ImageData mainImageData = nativeBanner.getImage();
            final MediaAdView mediaView = NativeViewsFactory.getMediaAdView( context );

            MaxNativeAd.MaxNativeAdImage iconImage = null;
            MaxNativeAd.MaxNativeAdImage mainImage = null;
            if ( icon != null )
            {
                if ( icon.getBitmap() != null )
                {
                    iconImage = new MaxNativeAd.MaxNativeAdImage( new BitmapDrawable( context.getResources(), icon.getBitmap() ) );
                }
                else
                {
                    iconImage = new MaxNativeAd.MaxNativeAdImage( Uri.parse( icon.getUrl() ) );
                }
            }
            if ( mainImageData != null )
            {
                if ( mainImageData.getBitmap() != null )
                {
                    mainImage = new MaxNativeAd.MaxNativeAdImage( new BitmapDrawable( context.getResources(), mainImageData.getBitmap() ) );
                }
                else
                {
                    mainImage = new MaxNativeAd.MaxNativeAdImage( Uri.parse( mainImageData.getUrl() ) );
                }
            }

            nativeAdView = NativeViewsFactory.getNativeAdView( context );
            nativeAdView.setupView( nativeAd.getBanner() );

            final MaxNativeAd.Builder builder = new MaxNativeAd.Builder()
                    .setAdFormat( MaxAdFormat.NATIVE )
                    .setTitle( nativeBanner.getTitle() )
                    .setBody( nativeBanner.getDescription() )
                    .setCallToAction( nativeBanner.getCtaText() )
                    .setIcon( iconImage )
                    .setMediaView( mediaView )
                    .setAdvertiser( nativeBanner.getAdvertisingLabel() );

            if ( AppLovinSdk.VERSION_CODE >= 11_04_03_99 )
            {
                builder.setMainImage( mainImage );
            }

            if ( AppLovinSdk.VERSION_CODE >= 11_04_00_00 )
            {
                builder.setMediaContentAspectRatio( mediaView.getMediaAspectRatio() );
            }

            final MaxNativeAd maxNativeAd = new MaxMyTargetNativeAd( builder );
            listener.onNativeAdLoaded( maxNativeAd, null );
        }

        @Override
        public void onNoAd(@NonNull final String reason, @NonNull NativeAd nativeAd)
        {
            log( "Native ad (" + slotId + ") failed to load with reason: " + reason );
            listener.onNativeAdLoadFailed( toMaxError( reason ) );
        }

        @Override
        public void onShow(@NonNull final NativeAd nativeAd)
        {
            log( "Native ad shown: " + slotId );
            listener.onNativeAdDisplayed( null );
        }

        @Override
        public void onClick(@NonNull final NativeAd nativeAd)
        {
            log( "Native ad clicked: " + slotId );
            listener.onNativeAdClicked();
        }

        @Override
        public void onVideoPlay(@NonNull final NativeAd nativeAd)
        {
            log( "Native ad video started: " + slotId );
        }

        @Override
        public void onVideoPause(@NonNull final NativeAd nativeAd)
        {
            log( "Native ad video paused: " + slotId );
        }

        @Override
        public void onVideoComplete(@NonNull final NativeAd nativeAd)
        {
            log( "Native ad video completed: " + slotId );
        }

        @Override
        public void onIconLoad(@NonNull final NativeAd nativeAd)
        {
            log( "Native ad icon loaded: " + slotId );
        }

        @Override
        public void onImageLoad(@NonNull final NativeAd nativeAd)
        {
            log( "Native ad image loaded: " + slotId );
        }
    }

    private class MaxMyTargetNativeAd
            extends MaxNativeAd
    {
        private MaxMyTargetNativeAd(final Builder builder) { super( builder ); }

        @Override
        public void prepareViewForInteraction(final MaxNativeAdView maxNativeAdView)
        {
            NativeAd nativeAd = MyTargetMediationAdapter.this.nativeAd;
            if ( nativeAd == null )
            {
                e( "Failed to register native ad views: native ad is null." );
                return;
            }

            final List<View> clickableViews = new ArrayList<>();
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
            if ( getIcon() != null && maxNativeAdView.getIconImageView() != null )
            {
                clickableViews.add( maxNativeAdView.getIconImageView() );
            }
            if ( getMediaView() != null && maxNativeAdView.getMediaContentViewGroup() != null )
            {
                clickableViews.add( maxNativeAdView.getMediaContentViewGroup() );
            }
            if ( AppLovinSdkUtils.isValidString( getAdvertiser() ) && maxNativeAdView.getAdvertiserTextView() != null )
            {
                clickableViews.add( maxNativeAdView.getAdvertiserTextView() );
            }

            nativeAd.registerView( maxNativeAdView, clickableViews );
        }
    }

    //endregion
}
