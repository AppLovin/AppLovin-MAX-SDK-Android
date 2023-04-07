package com.applovin.mediation.adapters;

import android.app.Activity;

import com.applovin.mediation.adapter.listeners.MaxNativeAdAdapterListener;
import com.applovin.mediation.adapter.parameters.MaxAdapterResponseParameters;
import com.applovin.sdk.AppLovinSdk;

public class VerizonAdsMediationAdapter
        extends YahooMediationAdapter
{
    public VerizonAdsMediationAdapter(final AppLovinSdk sdk) { super( sdk ); }

    // Kept intentionally for Mediation Debugger to detect support for native ads.
    @Override
    public void loadNativeAd(final MaxAdapterResponseParameters parameters, final Activity activity, final MaxNativeAdAdapterListener listener)
    {
        super.loadNativeAd( parameters, activity, listener );
    }
}
