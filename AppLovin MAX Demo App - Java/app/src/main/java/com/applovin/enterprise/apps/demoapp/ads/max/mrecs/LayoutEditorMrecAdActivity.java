package com.applovin.enterprise.apps.demoapp.ads.max.mrecs;

import android.os.Bundle;

import com.adjust.sdk.Adjust;
import com.adjust.sdk.AdjustAdRevenue;
import com.adjust.sdk.AdjustConfig;
import com.applovin.enterprise.apps.demoapp.R;
import com.applovin.enterprise.apps.demoapp.ui.BaseAdActivity;
import com.applovin.mediation.MaxAd;
import com.applovin.mediation.MaxAdRevenueListener;
import com.applovin.mediation.MaxAdViewAdListener;
import com.applovin.mediation.MaxError;
import com.applovin.mediation.ads.MaxAdView;

import androidx.annotation.Nullable;

/**
 * An {@link android.app.Activity} used to show AppLovin MAX MREC ads created in the Layout Editor.
 * <p>
 * Created by Andrew Tian on 2020-01-14.
 */
public class LayoutEditorMrecAdActivity
        extends BaseAdActivity
        implements MaxAdViewAdListener, MaxAdRevenueListener
{
    private MaxAdView adView;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState)
    {
        super.onCreate( savedInstanceState );
        setContentView( R.layout.activity_layout_editor_mrec_ad );
        setTitle( R.string.activity_layout_editor_mrecs );

        setupCallbacksRecyclerView();

        adView = findViewById( R.id.mrec_ad_view );

        adView.setListener( this );
        adView.setRevenueListener( this );

        // Load the first ad.
        adView.loadAd();
    }

    @Override
    protected void onDestroy()
    {
        super.onDestroy();

        adView.destroy();
    }

    //region MAX Ad Listener

    @Override
    public void onAdLoaded(final MaxAd ad) { logCallback(); }

    @Override
    public void onAdLoadFailed(final String adUnitId, final MaxError maxError) { logCallback(); }

    @Override
    public void onAdHidden(final MaxAd ad) { logCallback(); }

    @Override
    public void onAdDisplayFailed(final MaxAd ad, final MaxError maxError) { logCallback(); }

    @Override
    public void onAdDisplayed(final MaxAd ad) { logCallback(); }

    @Override
    public void onAdClicked(final MaxAd ad) { logCallback(); }

    @Override
    public void onAdExpanded(final MaxAd ad) { logCallback(); }

    @Override
    public void onAdCollapsed(final MaxAd ad) { logCallback(); }

    //endregion

    //region MAX Ad Revenue Listener

    @Override
    public void onAdRevenuePaid(final MaxAd maxAd)
    {
        logCallback();

        AdjustAdRevenue adjustAdRevenue = new AdjustAdRevenue( AdjustConfig.AD_REVENUE_APPLOVIN_MAX );
        adjustAdRevenue.setRevenue( maxAd.getRevenue(), "USD" );
        adjustAdRevenue.setAdRevenueNetwork( maxAd.getNetworkName() );
        adjustAdRevenue.setAdRevenueUnit( maxAd.getAdUnitId() );
        adjustAdRevenue.setAdRevenuePlacement( maxAd.getPlacement() );

        Adjust.trackAdRevenue( adjustAdRevenue );
    }

    //endregion
}
