package com.applovin.enterprise.apps.demoapp.ads;

import android.os.Bundle;

import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.ads.MaxAdView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * An {@link android.app.Activity} used to show AppLovin MAX banner ads created in the Layout Editor.
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

        adView = findViewById( R.id.banner_ad_view );
        adView.setListener( this );

        // Load the first ad.
        adView.loadAd();
    }

    //region MAX Ad Listener

    @Override
    public void onAdLoaded(final MaxAd ad) { }

    @Override
    public void onAdLoadFailed(final String adUnitId, final int errorCode) { }

    @Override
    public void onAdHidden(final MaxAd ad) { }

    @Override
    public void onAdDisplayFailed(final MaxAd ad, final int errorCode) { }

    @Override
    public void onAdDisplayed(final MaxAd ad) { }

    @Override
    public void onAdClicked(final MaxAd ad) { }

    @Override
    public void onAdExpanded(final MaxAd ad) { }

    @Override
    public void onAdCollapsed(final MaxAd ad) { }

    //endregion
}
