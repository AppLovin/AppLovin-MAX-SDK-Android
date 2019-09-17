package com.applovin.enterprise.apps.demoapp.ads;

import android.graphics.Color;
import android.os.Bundle;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.ads.MaxAdView;
import com.applovin.sdk.AppLovinSdkUtils;

import androidx.appcompat.app.AppCompatActivity;

/**
 * An {@link android.app.Activity} used to show AppLovin MAX banner ads.
 * <p>
 * Created by Harry Arakkal on 2019-09-17.
 */
public class LayoutEditorBannerAdActivity
        extends AppCompatActivity
        implements MaxAdViewAdListener
{
    private MaxAdView adView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_layout_editor_banner_ad );

        adView = findViewById( R.id.bannerAdView );
        adView.setListener( this );

        adView.loadAd();
    }

    //region MAX Ad Listener

    @Override
    public void onAdExpanded(final MaxAd ad) { }

    @Override
    public void onAdCollapsed(final MaxAd ad) { }

    @Override
    public void onAdLoaded(final MaxAd ad) { }

    @Override
    public void onAdLoadFailed(final String adUnitId, final int errorCode) { }

    @Override
    public void onAdDisplayed(final MaxAd ad) { }

    @Override
    public void onAdHidden(final MaxAd ad) { }

    @Override
    public void onAdClicked(final MaxAd ad) { }

    @Override
    public void onAdDisplayFailed(final MaxAd ad, final int errorCode) { }

    //endregion
}
